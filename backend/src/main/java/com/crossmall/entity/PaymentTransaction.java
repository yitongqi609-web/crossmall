package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_transaction")
public class PaymentTransaction {

    private Long id;
    /** 支付流水号(全局唯一,幂等锚点) */
    private String txnNo;
    private String orderNo;
    /** PAYPAL / CARD */
    private String channel;
    private String currency;
    private BigDecimal fxRate;
    /** 本币支付金额(分) */
    private Long amountLocal;
    /** 折算美元(分) */
    private Long amountUsd;
    /** INIT / SUCCESS / REFUNDED / VOID */
    private String status;
    private String callbackPayload;
    private LocalDateTime createTime;
    private LocalDateTime paidAt;
}
