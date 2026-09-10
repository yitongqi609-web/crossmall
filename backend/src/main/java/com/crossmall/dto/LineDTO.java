package com.crossmall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 物流线路保存
 */
@Data
public class LineDTO {

    private Long id;

    @NotBlank(message = "线路名不能为空")
    private String name;

    @NotBlank(message = "英文名不能为空")
    private String nameEn;

    @NotBlank(message = "承运商不能为空")
    private String carrier;

    /** 支持目的国:["*"] 或 ["US","CA"] */
    @NotNull(message = "支持国家不能为空")
    private List<String> supportedCountries;

    @NotNull @Min(1)
    private Integer firstWeightGrams;

    @NotNull @Min(0)
    private Long firstFeeCents;

    @NotNull @Min(1)
    private Integer continueWeightGrams;

    @NotNull @Min(0)
    private Long continueFeeCents;

    @NotNull @Min(1)
    private Integer etaMinDays;

    @NotNull @Min(1)
    private Integer etaMaxDays;

    private Integer status;
}
