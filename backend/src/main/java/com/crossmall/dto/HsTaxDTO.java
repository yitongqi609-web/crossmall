package com.crossmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 关税税则保存
 */
@Data
public class HsTaxDTO {

    private Long id;

    @NotBlank(message = "国家码不能为空")
    private String countryCode;

    @NotBlank(message = "国家名不能为空")
    private String countryName;

    @NotBlank(message = "HS 编码不能为空")
    private String hsCode;

    @NotBlank(message = "品类名不能为空")
    private String hsName;

    @NotNull(message = "税率不能为空")
    private BigDecimal taxRate;

    private Long deMinimisCents;
}
