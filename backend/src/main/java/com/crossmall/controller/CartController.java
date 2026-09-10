package com.crossmall.controller;

import com.crossmall.common.context.UserContext;
import com.crossmall.common.result.Result;
import com.crossmall.dto.CartAddDTO;
import com.crossmall.service.CartService;
import com.crossmall.vo.CartItemVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "06-购物车")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "购物车列表(实时解析价格/库存)")
    @GetMapping
    public Result<List<CartItemVO>> list(@RequestParam(defaultValue = "USD") String currency) {
        return Result.ok(cartService.list(UserContext.getUserId(), currency));
    }

    @Operation(summary = "购物车商品数")
    @GetMapping("/count")
    public Result<Integer> count() {
        return Result.ok(cartService.count(UserContext.getUserId()));
    }

    @Operation(summary = "加入购物车")
    @PostMapping("/items")
    public Result<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(UserContext.getUserId(), dto.getSkuId(), dto.getQuantity());
        return Result.ok();
    }

    @Operation(summary = "修改数量(0 即移除)")
    @PutMapping("/items/{skuId}")
    public Result<Void> updateQty(@PathVariable Long skuId, @RequestParam Integer quantity) {
        cartService.updateQty(UserContext.getUserId(), skuId, quantity);
        return Result.ok();
    }

    @Operation(summary = "移除商品")
    @DeleteMapping("/items")
    public Result<Void> remove(@RequestParam List<Long> skuIds) {
        cartService.remove(UserContext.getUserId(), skuIds);
        return Result.ok();
    }

    @Operation(summary = "清空购物车")
    @PostMapping("/clear")
    public Result<Void> clear() {
        cartService.clear(UserContext.getUserId());
        return Result.ok();
    }
}
