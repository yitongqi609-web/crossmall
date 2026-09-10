package com.crossmall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FxRateVO {

    private Long id;
    private String currency;
    private String currencyEn;
    private String symbol;
    private BigDecimal rate;
    private String source;
    private LocalDateTime updateTime;
}
