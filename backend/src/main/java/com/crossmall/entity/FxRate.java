package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fx_rate")
public class FxRate {

    private Long id;
    /** 目标币种 */
    private String currency;
    private String currencyEn;
    private String symbol;
    /** 1 USD = rate */
    private BigDecimal rate;
    /** MANUAL 手动 / AUTO 定时拉取 */
    private String source;
    private LocalDateTime updateTime;
}
