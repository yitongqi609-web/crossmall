package com.crossmall.service;

import com.crossmall.common.exception.BizException;
import com.crossmall.dto.OrderItemDTO;
import com.crossmall.entity.FxRate;
import com.crossmall.entity.HsTax;
import com.crossmall.entity.LogisticsLine;
import com.crossmall.entity.Sku;
import com.crossmall.entity.Spu;
import com.crossmall.mapper.SkuMapper;
import com.crossmall.mapper.SpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 交易试算核心:商品额 / 计费重 / 运费 / 关税 / 汇率换算
 * <p>金额规则:内部统一 USD 分;本币金额 = USD 金额 × 汇率,四舍五入到分。
 */
@Service
@RequiredArgsConstructor
public class TradeCalcService {

    private final SkuMapper skuMapper;
    private final SpuMapper spuMapper;
    private final LogisticsService logisticsService;
    private final TaxService taxService;
    private final FxService fxService;

    /** 下单草稿项 */
    public record DraftItem(Sku sku, Spu spu, int quantity, long subtotalUsd) {
    }

    public record CalcResult(List<DraftItem> items, long goodsUsd, int totalWeightGrams,
                             long shippingUsd, long taxUsd, List<TaxService.TaxHit> taxHits,
                             HsTax countryRule, long totalUsd) {
    }

    /** 加载并校验下单条目(在售、SKU 与 SPU 有效) */
    public List<DraftItem> loadDraftItems(List<OrderItemDTO> itemDTOs) {
        Map<Long, Integer> qtyBySku = itemDTOs.stream()
                .collect(Collectors.toMap(OrderItemDTO::getSkuId, OrderItemDTO::getQuantity, Integer::sum));

        Map<Long, Sku> skuById = skuMapper.selectBatchIds(qtyBySku.keySet()).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        List<Long> spuIds = skuById.values().stream().map(Sku::getSpuId).distinct().toList();
        Map<Long, Spu> spuById = spuIds.isEmpty() ? Map.of()
                : spuMapper.selectBatchIds(spuIds).stream()
                        .collect(Collectors.toMap(Spu::getId, Function.identity()));

        return qtyBySku.entrySet().stream().map(entry -> {
            Sku sku = skuById.get(entry.getKey());
            if (sku == null || sku.getStatus() != 1) {
                throw new BizException(400, "商品规格不存在或已下架: " + entry.getKey());
            }
            Spu spu = spuById.get(sku.getSpuId());
            if (spu == null || spu.getStatus() != 1) {
                throw new BizException(400, "商品不存在或已下架: " + sku.getSpuId());
            }
            return new DraftItem(sku, spu, entry.getValue(), sku.getPriceCents() * entry.getValue());
        }).toList();
    }

    /**
     * 整单试算:运费按整单计费重,关税按目的国+各品类 HS 税率
     */
    public CalcResult calc(String countryCode, LogisticsLine line, List<DraftItem> drafts) {
        long goodsUsd = drafts.stream().mapToLong(DraftItem::subtotalUsd).sum();
        int weight = drafts.stream()
                .mapToInt(d -> d.sku().getWeightGrams() * d.quantity())
                .sum();
        long shippingUsd = logisticsService.calcShippingFee(line, weight);

        List<TaxService.TaxItem> taxItems = drafts.stream()
                .map(d -> new TaxService.TaxItem(d.spu().getHsCode(), d.subtotalUsd()))
                .toList();
        TaxService.TaxResult taxResult = taxService.calculateOrder(countryCode, taxItems, goodsUsd);
        long totalUsd = goodsUsd + shippingUsd + taxResult.taxUsd();
        return new CalcResult(drafts, goodsUsd, weight, shippingUsd,
                taxResult.taxUsd(), taxResult.hits(), taxResult.countryRule(), totalUsd);
    }

    /** USD 分 → 本币分 */
    public long toLocal(long usdCents, FxRate fx) {
        return fxService.convert(usdCents, fx.getCurrency());
    }

    public FxRate requireFx(String currency) {
        return fxService.getFx(currency);
    }

    /** 校验线路支持目的国 */
    public void checkLineSupports(LogisticsLine line, String countryCode) {
        if (!logisticsService.supports(line, countryCode)) {
            throw new BizException(400, "线路「" + line.getName() + "」不支持目的国 " + countryCode);
        }
    }
}
