package com.crossmall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crossmall.common.exception.BizException;
import com.crossmall.entity.FxRate;
import com.crossmall.entity.Sku;
import com.crossmall.entity.Spu;
import com.crossmall.mapper.SkuMapper;
import com.crossmall.mapper.SpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车(Redis Hash:field=skuId, value=数量)
 * <p>只存 skuId→数量,读取时实时解析当前价格与库存,天然规避快照过期问题。
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private static final String KEY_PREFIX = "crossmall:cart:";

    private final StringRedisTemplate redis;
    private final SkuMapper skuMapper;
    private final SpuMapper spuMapper;
    private final FxService fxService;

    public void add(Long userId, Long skuId, int quantity) {
        Sku sku = requireValidSku(skuId);
        if (sku.getStock() < quantity + currentQty(userId, skuId)) {
            throw new BizException(400, "库存不足,加购失败");
        }
        redis.opsForHash().increment(cartKey(userId), String.valueOf(skuId), quantity);
    }

    public void updateQty(Long userId, Long skuId, int quantity) {
        requireValidSku(skuId);
        if (quantity <= 0) {
            redis.opsForHash().delete(cartKey(userId), String.valueOf(skuId));
        } else {
            redis.opsForHash().put(cartKey(userId), String.valueOf(skuId), String.valueOf(quantity));
        }
    }

    public void remove(Long userId, List<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return;
        }
        redis.opsForHash().delete(cartKey(userId),
                skuIds.stream().map(String::valueOf).toArray());
    }

    public void clear(Long userId) {
        redis.delete(cartKey(userId));
    }

    public List<com.crossmall.vo.CartItemVO> list(Long userId, String currency) {
        Map<Object, Object> entries = redis.opsForHash().entries(cartKey(userId));
        if (entries.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> qtyBySku = entries.entrySet().stream()
                .collect(Collectors.toMap(e -> Long.valueOf((String) e.getKey()),
                        e -> Integer.parseInt((String) e.getValue())));

        List<Sku> skus = skuMapper.selectBatchIds(qtyBySku.keySet());
        Map<Long, Spu> spuById = spusOf(skus);
        FxRate fx = fxService.getFx(currency);

        List<com.crossmall.vo.CartItemVO> items = new ArrayList<>();
        for (Sku sku : skus) {
            Spu spu = spuById.get(sku.getSpuId());
            com.crossmall.vo.CartItemVO item = new com.crossmall.vo.CartItemVO();
            item.setSkuId(sku.getId());
            item.setSpuId(sku.getSpuId());
            item.setTitle(spu == null ? "" : spu.getTitle());
            item.setTitleEn(spu == null ? "" : spu.getTitleEn());
            item.setAttrs(sku.getAttrs());
            item.setImage(spu == null ? null : spu.getMainImage());
            item.setPriceCents(sku.getPriceCents());
            item.setDisplayCurrency(fx.getCurrency());
            item.setDisplaySymbol(fx.getSymbol());
            item.setDisplayPrice(fxService.convert(sku.getPriceCents(), currency));
            item.setQuantity(qtyBySku.get(sku.getId()));
            item.setStock(sku.getStock());
            item.setValid(sku.getStatus() == 1 && spu != null && spu.getStatus() == 1);
            items.add(item);
        }
        // SKU 已被删除的购物车残留项直接清理
        List<Long> missing = qtyBySku.keySet().stream()
                .filter(id -> skus.stream().noneMatch(s -> s.getId().equals(id)))
                .toList();
        remove(userId, missing);
        return items;
    }

    public int count(Long userId) {
        return redis.opsForHash().size(cartKey(userId)).intValue();
    }

    /** 下单成功后按 skuId 批量移除 */
    public void removeAfterOrder(Long userId, List<Long> skuIds) {
        remove(userId, skuIds);
    }

    private int currentQty(Long userId, Long skuId) {
        Object v = redis.opsForHash().get(cartKey(userId), String.valueOf(skuId));
        return v == null ? 0 : Integer.parseInt((String) v);
    }

    private String cartKey(Long userId) {
        return KEY_PREFIX + userId;
    }

    private Sku requireValidSku(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() != 1) {
            throw new BizException(404, "商品规格不存在或已下架");
        }
        return sku;
    }

    private Map<Long, Spu> spusOf(List<Sku> skus) {
        List<Long> spuIds = skus.stream().map(Sku::getSpuId).distinct().toList();
        if (spuIds.isEmpty()) {
            return Map.of();
        }
        return spuMapper.selectBatchIds(spuIds).stream()
                .collect(Collectors.toMap(Spu::getId, Function.identity()));
    }
}
