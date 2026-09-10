package com.crossmall.controller;

import com.crossmall.common.result.Result;
import com.crossmall.service.GoodsService;
import com.crossmall.vo.HomeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "03-首页")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final GoodsService goodsService;

    @Operation(summary = "首页聚合:分类 + 热销商品")
    @GetMapping
    public Result<HomeVO> home(@RequestParam(defaultValue = "USD") String currency) {
        return Result.ok(goodsService.home(currency));
    }
}
