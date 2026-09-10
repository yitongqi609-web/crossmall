package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category")
public class Category {

    private Long id;
    private String name;
    private String nameEn;
    private String icon;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
