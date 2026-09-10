package com.crossmall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crossmall.entity.HsTax;
import com.crossmall.mapper.HsTaxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 关税试算服务
 * <p>税则匹配顺序:(目的国 + HS 编码) → (目的国 + *) → (全球默认 *)
 * <p>免税额度(de minimis):商品申报总额低于该国额度时免征,如美国 $800、澳大利亚 AUD 1000
 * <p>简化约定:税基=商品总额(不含运费),多品类订单按各品类税率分别计算后求和。
 */
@Service
@RequiredArgsConstructor
public class TaxService {

    private final HsTaxMapper hsTaxMapper;

    /**
     * 税率明细命中记录(结算页展示用)
     */
    public record TaxHit(String hsCode, String hsName, BigDecimal rate, long baseUsd, long taxUsd) {
    }

    public record TaxResult(long taxUsd, List<TaxHit> hits, HsTax countryRule) {
    }

    /**
     * 整单试算:先判免税额度,再按品类逐项计税
     */
    public TaxResult calculateOrder(String countryCode, List<TaxItem> items, long goodsTotalUsd) {
        HsTax countryRule = findRule(countryCode, "*");
        List<TaxHit> hits = new ArrayList<>();
        if (countryRule.getDeMinimisCents() > 0 && goodsTotalUsd < countryRule.getDeMinimisCents()) {
            // 低于免税额度,整单免关税
            return new TaxResult(0, List.of(new TaxHit("*", "De Minimis 免税",
                    BigDecimal.ZERO, goodsTotalUsd, 0)), countryRule);
        }
        long total = 0;
        for (TaxItem item : items) {
            HsTax rule = findRule(countryCode, item.hsCode());
            long tax = calcTax(rule, item.subtotalUsd());
            total += tax;
            hits.add(new TaxHit(rule.getHsCode(), rule.getHsName(), rule.getTaxRate(),
                    item.subtotalUsd(), tax));
        }
        return new TaxResult(total, hits, countryRule);
    }

    private long calcTax(HsTax rule, long baseUsd) {
        return BigDecimal.valueOf(baseUsd).multiply(rule.getTaxRate())
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** 税则查找:(国家+HS) → (国家+*) → (全球*) */
    public HsTax findRule(String countryCode, String hsCode) {
        HsTax rule = hsTaxMapper.selectOne(new LambdaQueryWrapper<HsTax>()
                .eq(HsTax::getCountryCode, countryCode)
                .eq(HsTax::getHsCode, hsCode)
                .last("limit 1"));
        if (rule == null && !"*".equals(hsCode)) {
            rule = hsTaxMapper.selectOne(new LambdaQueryWrapper<HsTax>()
                    .eq(HsTax::getCountryCode, countryCode)
                    .eq(HsTax::getHsCode, "*")
                    .last("limit 1"));
        }
        if (rule == null) {
            rule = hsTaxMapper.selectOne(new LambdaQueryWrapper<HsTax>()
                    .eq(HsTax::getCountryCode, "*")
                    .eq(HsTax::getHsCode, "*")
                    .last("limit 1"));
        }
        if (rule == null) {
            throw new IllegalStateException("关税税则表缺少全球默认规则(country=*, hs=*)");
        }
        return rule;
    }

    /**
     * 整单计税条目
     */
    public record TaxItem(String hsCode, long subtotalUsd) {
    }
}
