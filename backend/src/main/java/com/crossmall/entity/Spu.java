package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("spu")
public class Spu {

    private Long id;
    private Long categoryId;
    private String title;
    private String titleEn;
    private String description;
    private String descriptionEn;
    private String brand;
    private String hsCode;
    private Integer weightGrams;
    private String mainImage;
    private Integer sales;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
