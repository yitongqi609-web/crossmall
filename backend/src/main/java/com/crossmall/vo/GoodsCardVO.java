package com.crossmall.vo;

import lombok.Data;

/**
 * 商品卡片(列表/首页)
 * priceCents 为 USD 分基准价;display* 为按请求币种换算后的展示价
 */
@Data
public class GoodsCardVO {

    private Long spuId;
    private Long categoryId;
    private String title;
    private String titleEn;
    private String brand;
    private String image;
    private Long priceCents;
    private String displayCurrency;
    private String displaySymbol;
    private Long displayPrice;
    private Integer sales;
}
