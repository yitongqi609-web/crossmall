package com.crossmall.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 下单入参:currency 为结算币种,下单即锁定该币种汇率快照
 */
@Data
public class OrderCreateDTO {

    @NotNull(message = "addressId 不能为空")
    private Long addressId;

    @NotNull(message = "请选择物流线路")
    private Long lineId;

    @NotBlank(message = "请选择结算币种")
    private String currency;

    @Valid
    @NotEmpty(message = "请选择商品")
    private List<OrderItemDTO> items;
}
