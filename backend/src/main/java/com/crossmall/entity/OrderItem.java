package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("order_item")
public class OrderItem {

    private Long id;
    private Long orderId;
    private String orderNo;
    private Long spuId;
    private Long skuId;
    private String spuTitle;
    private String spuTitleEn;
    private String skuAttrs;
    private String image;
    /** 成交单价(USD 分)快照 */
    private Long priceCents;
    private Integer quantity;
    /** 小计(USD 分) */
    private Long subtotalCents;
    private Integer weightGrams;
}
