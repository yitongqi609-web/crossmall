package com.crossmall.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 订单试算入参:lineId 可空(为空时返回所有可用线路的运费供选择);
 * currency 可空(默认 USD)
 */
@Data
public class OrderPreviewDTO {

    @NotNull(message = "addressId 不能为空")
    private Long addressId;

    private Long lineId;

    private String currency;

    @Valid
    @NotEmpty(message = "请选择商品")
    private List<OrderItemDTO> items;
}
