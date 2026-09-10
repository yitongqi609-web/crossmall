package com.crossmall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单试算结果(结算页三段金额:商品 + 运费 + 关税)
 */
@Data
public class OrderPreviewVO {

    private List<ItemLine> items;
    /** 目的国 */
    private String countryCode;
    private String countryName;
    /** 可选线路及运费(lineId 未指定时返回全部) */
    private List<LineOption> lineOptions;
    /** 选中线路(指定 lineId 时返回明细金额) */
    private Long lineId;
    private Long firstWeightGrams;
    private Long totalWeightGrams;

    /** 商品总额(USD 分) */
    private Long goodsFeeCents;
    /** 运费(USD 分) */
    private Long shippingFeeCents;
    /** 关税(USD 分) */
    private Long taxFeeCents;
    /** 总额(USD 分) */
    private Long totalFeeCents;

    /** 关税明细 */
    private List<TaxHitVO> taxHits;
    /** 该国免税额度(USD 分),0=无 */
    private Long deMinimisCents;

    /** 结算币种与汇率(下单时写入快照) */
    private String currency;
    private String displaySymbol;
    private BigDecimal fxRate;
    /** 本币应付总额(分) */
    private Long totalLocalCents;

    @Data
    public static class ItemLine {
        private Long skuId;
        private Long spuId;
        private String title;
        private String titleEn;
        private String attrs;
        private String image;
        private Integer quantity;
        private Long unitPriceCents;
        private String displaySymbol;
        private Long unitDisplayPrice;
        private Long subtotalCents;
        private Long subtotalDisplayPrice;
    }

    @Data
    public static class LineOption {
        private Long lineId;
        private String name;
        private String nameEn;
        private String carrier;
        private Integer etaMinDays;
        private Integer etaMaxDays;
        /** 该线路运费(USD 分) */
        private Long shippingFeeCents;
        private String displaySymbol;
        private Long shippingDisplayPrice;
        private Boolean selected;
    }

    @Data
    public static class TaxHitVO {
        private String hsCode;
        private String hsName;
        private BigDecimal rate;
        private Long baseCents;
        private Long taxCents;
    }
}
