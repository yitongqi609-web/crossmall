package com.crossmall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crossmall.common.context.UserContext;
import com.crossmall.common.result.Result;
import com.crossmall.dto.OrderCreateDTO;
import com.crossmall.dto.OrderPreviewDTO;
import com.crossmall.service.OrderService;
import com.crossmall.vo.OrderPreviewVO;
import com.crossmall.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "07-订单")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "结算试算(运费+关税+汇率三段明细)")
    @PostMapping("/preview")
    public Result<OrderPreviewVO> preview(@Valid @RequestBody OrderPreviewDTO dto) {
        return Result.ok(orderService.preview(UserContext.getUserId(), dto));
    }

    @Operation(summary = "创建订单(锁定汇率快照)")
    @PostMapping
    public Result<String> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.ok(orderService.create(UserContext.getUserId(), dto));
    }

    @Operation(summary = "我的订单分页")
    @GetMapping("/page")
    public Result<Page<OrderVO>> page(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(orderService.pageMine(UserContext.getUserId(), status, page, size));
    }

    @Operation(summary = "订单详情(含履约轨迹)")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.detail(UserContext.getUserId(), orderNo));
    }

    @Operation(summary = "取消订单(仅待付款)")
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo,
                               @RequestParam(required = false) String reason) {
        orderService.cancel(UserContext.getUserId(), orderNo, reason);
        return Result.ok();
    }
}
