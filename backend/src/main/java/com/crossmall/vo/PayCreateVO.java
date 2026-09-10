package com.crossmall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建支付返回:跳转模拟收银台所需信息
 */
@Data
public class PayCreateVO {

    private String orderNo;
    private String txnNo;
    private String channel;
    private String currency;
    private String symbol;
    /** 应付本币金额(分) */
    private Long amountLocal;
    /** 支付截止时间 */
    private LocalDateTime expireAt;
}
