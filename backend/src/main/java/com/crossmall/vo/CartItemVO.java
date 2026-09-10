package com.crossmall.vo;

import lombok.Data;

/**
 * 购物车条目(读取时实时解析 SKU 当前价与库存,避免快照过期)
 */
@Data
public class CartItemVO {

    private Long skuId;
    private Long spuId;
    private String title;
    private String titleEn;
    private String attrs;
    private String image;
    private Long priceCents;
    private String displayCurrency;
    private String displaySymbol;
    private Long displayPrice;
    private Integer quantity;
    private Integer stock;
    /** SKU 是否仍有效(下架/删除时置 false,前端提示) */
    private Boolean valid;
}
