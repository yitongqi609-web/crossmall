package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sku")
public class Sku {

    private Long id;
    private Long spuId;
    /** 规格 JSON:[{"k":"颜色","kEn":"Color","v":"黑","vEn":"Black"}] */
    private String attrs;
    /** 单价(USD 分) */
    private Long priceCents;
    /** DB 库存(权威值) */
    private Integer stock;
    private Integer weightGrams;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
