package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("address")
public class Address {

    private Long id;
    private Long userId;
    private String receiverName;
    private String phone;
    private String countryCode;
    private String countryName;
    private String stateProvince;
    private String city;
    private String street;
    private String postcode;
    private Integer isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
