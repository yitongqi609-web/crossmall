package com.crossmall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.crossmall.common.exception.BizException;
import com.crossmall.dto.AddressDTO;
import com.crossmall.entity.Address;
import com.crossmall.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务(仅操作当前登录用户的地址)
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressMapper addressMapper;

    public List<Address> list(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getId));
    }

    @Transactional
    public Address save(Long userId, AddressDTO dto) {
        Address address = new Address();
        copy(dto, address);
        address.setUserId(userId);
        // 首个地址自动设为默认
        boolean hasAny = addressMapper.selectCount(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)) > 0;
        address.setIsDefault(Boolean.TRUE.equals(toBool(dto.getIsDefault())) || !hasAny ? 1 : 0);
        if (address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        addressMapper.insert(address);
        return address;
    }

    @Transactional
    public Address update(Long userId, Long id, AddressDTO dto) {
        Address address = requireOwned(userId, id);
        copy(dto, address);
        if (toBool(dto.getIsDefault())) {
            clearDefault(userId);
            address.setIsDefault(1);
        }
        addressMapper.updateById(address);
        return address;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        addressMapper.deleteById(id);
    }

    @Transactional
    public void setDefault(Long userId, Long id) {
        requireOwned(userId, id);
        clearDefault(userId);
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getId, id)
                .set(Address::getIsDefault, 1));
    }

    public Address requireOwned(Long userId, Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException(404, "地址不存在");
        }
        return address;
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 0));
    }

    private void copy(AddressDTO dto, Address address) {
        address.setReceiverName(dto.getReceiverName());
        address.setPhone(dto.getPhone());
        address.setCountryCode(dto.getCountryCode().toUpperCase());
        address.setCountryName(dto.getCountryName());
        address.setStateProvince(dto.getStateProvince());
        address.setCity(dto.getCity());
        address.setStreet(dto.getStreet());
        address.setPostcode(dto.getPostcode());
    }

    private Boolean toBool(Integer v) {
        return v != null && v == 1;
    }
}
