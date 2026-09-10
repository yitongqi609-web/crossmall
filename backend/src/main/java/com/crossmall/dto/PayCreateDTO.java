package com.crossmall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayCreateDTO {

    @NotBlank(message = "请选择支付渠道")
    private String channel;
}
