package com.crossmall.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crossmall.common.result.Result;
import com.crossmall.dto.SpuSaveDTO;
import com.crossmall.service.AdminGoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "A2-商品管理")
@RestController
@RequestMapping("/api/admin/goods")
@RequiredArgsConstructor
public class AdminGoodsController {

    private final AdminGoodsService adminGoodsService;

    @Operation(summary = "商品分页(含 SKU/库存/分类)")
    @GetMapping("/page")
    public Result<Page<Map<String, Object>>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(adminGoodsService.pageSpu(page, size, keyword, categoryId));
    }

    @Operation(summary = "保存商品(SPU+SKU 整体编辑)")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody SpuSaveDTO dto) {
        adminGoodsService.saveSpu(dto);
        return Result.ok();
    }

    @Operation(summary = "上下架")
    @PutMapping("/{spuId}/status")
    public Result<Void> updateStatus(@PathVariable Long spuId, @RequestParam Integer status) {
        adminGoodsService.updateSpuStatus(spuId, status);
        return Result.ok();
    }
}
