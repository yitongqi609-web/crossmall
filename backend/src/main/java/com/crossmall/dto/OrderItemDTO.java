package com.crossmall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemDTO {

    @NotNull(message = "skuId 不能为空")
    private Long skuId;

    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;
}
