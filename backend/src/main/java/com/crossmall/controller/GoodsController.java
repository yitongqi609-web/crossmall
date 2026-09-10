package com.crossmall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crossmall.common.result.Result;
import com.crossmall.service.GoodsService;
import com.crossmall.vo.GoodsCardVO;
import com.crossmall.vo.GoodsDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "04-商品浏览")
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @Operation(summary = "商品列表(分类/关键词/分页)")
    @GetMapping
    public Result<Page<GoodsCardVO>> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long size,
            @RequestParam(defaultValue = "USD") String currency) {
        return Result.ok(goodsService.page(categoryId, keyword, page, size, currency));
    }

    @Operation(summary = "商品详情(含 SKU 多币种价格)")
    @GetMapping("/{spuId}")
    public Result<GoodsDetailVO> detail(
            @PathVariable Long spuId,
            @RequestParam(defaultValue = "USD") String currency) {
        return Result.ok(goodsService.detail(spuId, currency));
    }
}
