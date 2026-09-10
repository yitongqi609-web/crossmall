package com.crossmall.dto;

import com.crossmall.entity.Address;
import lombok.Data;

/**
 * 下单时的收货地址快照(与地址表解耦,地址后续修改不影响历史订单)
 * <p>注意:必须为普通 JavaBean —— hutool JSONUtil 无法序列化 record(产出 {}),地址快照会丢
 */
@Data
public class AddressSnapshot {

    private String receiverName;
    private String phone;
    private String countryCode;
    private String countryName;
    private String stateProvince;
    private String city;
    private String street;
    private String postcode;

    public static AddressSnapshot of(Address a) {
        AddressSnapshot s = new AddressSnapshot();
        s.setReceiverName(a.getReceiverName());
        s.setPhone(a.getPhone());
        s.setCountryCode(a.getCountryCode());
        s.setCountryName(a.getCountryName());
        s.setStateProvince(a.getStateProvince());
        s.setCity(a.getCity());
        s.setStreet(a.getStreet());
        s.setPostcode(a.getPostcode());
        return s;
    }
}
