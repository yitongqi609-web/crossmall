package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logistics_line")
public class LogisticsLine {

    private Long id;
    private String name;
    private String nameEn;
    private String carrier;
    /** 支持目的国 JSON:["*"] 或 ["US","CA"] */
    private String supportedCountries;
    private Integer firstWeightGrams;
    private Long firstFeeCents;
    private Integer continueWeightGrams;
    private Long continueFeeCents;
    private Integer etaMinDays;
    private Integer etaMaxDays;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
