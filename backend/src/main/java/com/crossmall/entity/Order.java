package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    private Long id;
    private String orderNo;
    private Long userId;
    /** 见 fulfillment.OrderStatus */
    private String status;
    /** 收货地址快照 JSON */
    private String addressSnapshot;
    private Long logisticsLineId;
    private String lineName;
    /** 结算币种 */
    private String currency;
    /** 汇率快照(1 USD = fx),下单时锁定 */
    private BigDecimal fxRate;
    /** 商品总额(USD 分) */
    private Long goodsFeeCents;
    /** 运费(USD 分) */
    private Long shippingFeeCents;
    /** 关税(USD 分) */
    private Long taxFeeCents;
    /** 订单总额(USD 分)= 商品 + 运费 + 关税 */
    private Long totalFeeCents;
    /** 本币应付总额(分)= total * fx 快照 */
    private Long totalLocalCents;
    private LocalDateTime payTimeoutAt;
    private LocalDateTime paidAt;
    private LocalDateTime completedAt;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
