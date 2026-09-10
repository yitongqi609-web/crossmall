package com.crossmall.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crossmall.common.result.Result;
import com.crossmall.dto.HsTaxDTO;
import com.crossmall.entity.HsTax;
import com.crossmall.mapper.HsTaxMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "A6-关税税则")
@RestController
@RequestMapping("/api/admin/tax")
@RequiredArgsConstructor
public class AdminTaxController {

    private final HsTaxMapper hsTaxMapper;

    @Operation(summary = "税则列表(可按国家过滤)")
    @GetMapping
    public Result<List<HsTax>> list(@RequestParam(required = false) String countryCode) {
        return Result.ok(hsTaxMapper.selectList(new LambdaQueryWrapper<HsTax>()
                .eq(countryCode != null && !countryCode.isBlank(), HsTax::getCountryCode, countryCode)
                .orderByAsc(HsTax::getCountryCode)
                .orderByAsc(HsTax::getHsCode)));
    }

    @Operation(summary = "保存税则(新增/编辑)")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody HsTaxDTO dto) {
        HsTax tax = new HsTax();
        BeanUtils.copyProperties(dto, tax);
        tax.setCountryCode(dto.getCountryCode().toUpperCase());
        if (dto.getId() == null) {
            hsTaxMapper.insert(tax);
        } else {
            hsTaxMapper.updateById(tax);
        }
        return Result.ok();
    }

    @Operation(summary = "删除税则")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        hsTaxMapper.deleteById(id);
        return Result.ok();
    }
}
