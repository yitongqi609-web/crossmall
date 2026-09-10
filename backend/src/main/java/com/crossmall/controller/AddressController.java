package com.crossmall.controller;

import com.crossmall.common.context.UserContext;
import com.crossmall.common.result.Result;
import com.crossmall.dto.AddressDTO;
import com.crossmall.entity.Address;
import com.crossmall.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "02-收货地址")
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "地址列表")
    @GetMapping
    public Result<List<Address>> list() {
        return Result.ok(addressService.list(UserContext.getUserId()));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<Address> save(@Valid @RequestBody AddressDTO dto) {
        return Result.ok(addressService.save(UserContext.getUserId(), dto));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public Result<Address> update(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        return Result.ok(addressService.update(UserContext.getUserId(), id, dto));
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(UserContext.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "设为默认")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(UserContext.getUserId(), id);
        return Result.ok();
    }
}
