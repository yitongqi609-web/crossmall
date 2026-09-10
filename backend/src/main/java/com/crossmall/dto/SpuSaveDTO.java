package com.crossmall.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * SPU + SKU 整体保存(管理端商品编辑)
 */
@Data
public class SpuSaveDTO {

    private Long id;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "中文标题不能为空")
    private String title;

    @NotBlank(message = "英文标题不能为空")
    private String titleEn;

    private String description;
    private String descriptionEn;
    private String brand;

    @NotBlank(message = "HS 编码不能为空")
    private String hsCode;

    @NotNull @Min(value = 1, message = "重量需大于 0")
    private Integer weightGrams;

    private String mainImage;

    private Integer status;

    @Valid
    private List<SkuSaveDTO> skus;

    @Data
    public static class SkuSaveDTO {
        private Long id;

        @NotBlank(message = "规格不能为空")
        private String attrs;

        @NotNull @Min(value = 1, message = "价格需大于 0")
        private Long priceCents;

        @NotNull @Min(value = 0, message = "库存不能为负")
        private Integer stock;

        @NotNull @Min(value = 1, message = "重量需大于 0")
        private Integer weightGrams;

        private Integer status;
    }
}
