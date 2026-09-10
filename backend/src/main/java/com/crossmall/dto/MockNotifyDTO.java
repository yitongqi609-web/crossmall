package com.crossmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 支付网关异步回调报文
 */
@Data
public class MockNotifyDTO {

    @NotBlank(message = "txnNo 不能为空")
    private String txnNo;

    @NotNull(message = "amountLocal 不能为空")
    private Long amountLocal;

    @NotBlank(message = "sign 不能为空")
    private String sign;

    private String payload;
}
