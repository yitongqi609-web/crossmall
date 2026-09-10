package com.crossmall.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crossmall.common.result.Result;
import com.crossmall.dto.OrderActionDTO;
import com.crossmall.entity.Order;
import com.crossmall.mapper.OrderMapper;
import com.crossmall.service.FulfillmentService;
import com.crossmall.service.OrderService;
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

import java.util.List;

@Tag(name = "A7-订单履约")
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final FulfillmentService fulfillmentService;

    @Operation(summary = "订单分页(状态/订单号筛选)")
    @GetMapping("/page")
    public Result<Page<OrderVO>> page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(status != null && !status.isBlank(), Order::getStatus, status)
                        .eq(orderNo != null && !orderNo.isBlank(), Order::getOrderNo, orderNo)
                        .orderByDesc(Order::getId));
        Page<OrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(orderService.pageForAdmin(result.getRecords()));
        return Result.ok(voPage);
    }

    @Operation(summary = "订单详情(含履约轨迹)")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.detailForAdmin(orderNo));
    }

    @Operation(summary = "履约流转(状态机校验:备货/报关/启运/清关/派送/妥投/退款)")
    @PostMapping("/{orderNo}/transit")
    public Result<Void> transit(@PathVariable String orderNo, @Valid @RequestBody OrderActionDTO dto) {
        fulfillmentService.transit(orderNo, dto.getTargetStatus(),
                dto.getLocation(), dto.getDescription(), dto.getDescriptionEn());
        return Result.ok();
    }

    @Operation(summary = "关单兜底对账(扫描超时未支付订单)")
    @PostMapping("/close-expired")
    public Result<Integer> closeExpired(@RequestParam(defaultValue = "100") int limit) {
        return Result.ok(fulfillmentService.closeExpired(limit));
    }
}
