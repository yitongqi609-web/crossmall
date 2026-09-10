package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("hs_tax")
public class HsTax {

    private Long id;
    private String countryCode;
    private String countryName;
    /** HS 编码,* 表示该国通用税率 */
    private String hsCode;
    private String hsName;
    /** 关税税率,0.0800 = 8% */
    private BigDecimal taxRate;
    /** 免税额度(USD 分),商品额低于此值免关税 */
    private Long deMinimisCents;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
