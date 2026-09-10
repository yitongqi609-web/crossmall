package com.crossmall.vo;

import lombok.Data;

/**
 * SKU 规格项
 */
@Data
public class SkuVO {

    private Long skuId;
    private Long spuId;
    /** 规格JSON,含双语:[{"k":"颜色","kEn":"Color","v":"黑","vEn":"Black"}] */
    private String attrs;
    private Long priceCents;
    private String displayCurrency;
    private String displaySymbol;
    private Long displayPrice;
    private Integer stock;
}
