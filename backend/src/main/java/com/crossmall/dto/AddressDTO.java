package com.crossmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址(国际格式)
 */
@Data
public class AddressDTO {

    @NotBlank(message = "收件人不能为空")
    @Size(max = 64)
    private String receiverName;

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 32)
    private String phone;

    @NotBlank(message = "国家不能为空")
    @Size(max = 8)
    private String countryCode;

    @NotBlank(message = "国家名不能为空")
    @Size(max = 64)
    private String countryName;

    @Size(max = 64)
    private String stateProvince;

    @NotBlank(message = "城市不能为空")
    @Size(max = 64)
    private String city;

    @NotBlank(message = "街道地址不能为空")
    @Size(max = 255)
    private String street;

    @NotBlank(message = "邮编不能为空")
    @Size(max = 32)
    private String postcode;

    private Integer isDefault;
}
