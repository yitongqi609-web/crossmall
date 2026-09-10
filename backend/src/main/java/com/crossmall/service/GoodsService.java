package com.crossmall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crossmall.common.exception.BizException;
import com.crossmall.entity.Category;
import com.crossmall.entity.FxRate;
import com.crossmall.entity.Sku;
import com.crossmall.entity.Spu;
import com.crossmall.mapper.CategoryMapper;
import com.crossmall.mapper.SkuMapper;
import com.crossmall.mapper.SpuMapper;
import com.crossmall.vo.GoodsCardVO;
import com.crossmall.vo.GoodsDetailVO;
import com.crossmall.vo.HomeVO;
import com.crossmall.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品浏览服务(用户端):多语言字段成对返回,展示语言由前端按 locale 选择;
 * 展示币种由服务端按实时汇率换算(USD 分为基准)。
 */
@Service
@RequiredArgsConstructor
public class GoodsService {

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final FxService fxService;

    public HomeVO home(String currency) {
        HomeVO vo = new HomeVO();
        vo.setCategories(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort)));
        List<Spu> hot = spuMapper.selectList(new LambdaQueryWrapper<Spu>()
                .eq(Spu::getStatus, 1)
                .orderByDesc(Spu::getSales)
                .last("limit 8"));
        vo.setHotGoods(toCards(hot, currency));
        return vo;
    }

    public Page<GoodsCardVO> page(Long categoryId, String keyword, long page, long size, String currency) {
        Page<Spu> spuPage = spuMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Spu>()
                        .eq(Spu::getStatus, 1)
                        .eq(categoryId != null, Spu::getCategoryId, categoryId)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(Spu::getTitle, keyword)
                                .or()
                                .like(Spu::getTitleEn, keyword))
                        .orderByDesc(Spu::getSales));
        Page<GoodsCardVO> result = new Page<>(spuPage.getCurrent(), spuPage.getSize(), spuPage.getTotal());
        result.setRecords(toCards(spuPage.getRecords(), currency));
        return result;
    }

    public GoodsDetailVO detail(Long spuId, String currency) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null || spu.getStatus() != 1) {
            throw new BizException(404, "商品不存在或已下架");
        }
        GoodsDetailVO vo = new GoodsDetailVO();
        vo.setSpuId(spu.getId());
        vo.setCategoryId(spu.getCategoryId());
        vo.setTitle(spu.getTitle());
        vo.setTitleEn(spu.getTitleEn());
        vo.setDescription(spu.getDescription());
        vo.setDescriptionEn(spu.getDescriptionEn());
        vo.setBrand(spu.getBrand());
        vo.setImage(spu.getMainImage());
        vo.setSales(spu.getSales());
        vo.setHsCode(spu.getHsCode());

        FxRate fx = fxService.getFx(currency);
        vo.setSkus(skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                        .eq(Sku::getSpuId, spuId)
                        .eq(Sku::getStatus, 1)
                        .orderByAsc(Sku::getId))
                .stream()
                .map(sku -> {
                    SkuVO skuVO = new SkuVO();
                    skuVO.setSkuId(sku.getId());
                    skuVO.setSpuId(sku.getSpuId());
                    skuVO.setAttrs(sku.getAttrs());
                    skuVO.setPriceCents(sku.getPriceCents());
                    fillDisplay(skuVO, sku.getPriceCents(), fx);
                    skuVO.setStock(sku.getStock());
                    return skuVO;
                })
                .toList());
        return vo;
    }

    /** 批量组卡片:每页一次 SKU 批量查询取最低价,避免 N+1 */
    private List<GoodsCardVO> toCards(List<Spu> spus, String currency) {
        if (spus.isEmpty()) {
            return List.of();
        }
        List<Long> spuIds = spus.stream().map(Spu::getId).toList();
        Map<Long, Long> minPriceBySpu = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                        .in(Sku::getSpuId, spuIds)
                        .eq(Sku::getStatus, 1))
                .stream()
                .collect(Collectors.toMap(Sku::getSpuId, Sku::getPriceCents, Long::min));
        FxRate fx = fxService.getFx(currency);
        return spus.stream().map(spu -> {
            GoodsCardVO card = new GoodsCardVO();
            card.setSpuId(spu.getId());
            card.setCategoryId(spu.getCategoryId());
            card.setTitle(spu.getTitle());
            card.setTitleEn(spu.getTitleEn());
            card.setBrand(spu.getBrand());
            card.setImage(spu.getMainImage());
            card.setSales(spu.getSales());
            Long min = minPriceBySpu.get(spu.getId());
            card.setPriceCents(min == null ? 0L : min);
            fillDisplay(card, card.getPriceCents(), fx);
            return card;
        }).toList();
    }

    private void fillDisplay(Object vo, Long usdCents, FxRate fx) {
        long display = fxService.convert(usdCents == null ? 0 : usdCents, fx.getCurrency());
        if (vo instanceof GoodsCardVO card) {
            card.setDisplayCurrency(fx.getCurrency());
            card.setDisplaySymbol(fx.getSymbol());
            card.setDisplayPrice(display);
        } else if (vo instanceof SkuVO skuVO) {
            skuVO.setDisplayCurrency(fx.getCurrency());
            skuVO.setDisplaySymbol(fx.getSymbol());
            skuVO.setDisplayPrice(display);
        }
    }
}
