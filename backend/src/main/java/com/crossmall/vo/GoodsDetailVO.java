package com.crossmall.vo;

import lombok.Data;

import java.util.List;

@Data
public class GoodsDetailVO {

    private Long spuId;
    private Long categoryId;
    private String title;
    private String titleEn;
    private String description;
    private String descriptionEn;
    private String brand;
    private String image;
    private Integer sales;
    /** HS 编码(用于关税试算,详情页可提示) */
    private String hsCode;
    private List<SkuVO> skus;
}
