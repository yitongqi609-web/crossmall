package com.crossmall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.crossmall.common.exception.BizException;
import com.crossmall.entity.Order;
import com.crossmall.entity.OrderItem;
import com.crossmall.entity.PaymentTransaction;
import com.crossmall.fulfillment.OrderStatus;
import com.crossmall.mapper.OrderItemMapper;
import com.crossmall.mapper.OrderMapper;
import com.crossmall.mapper.OrderTrackMapper;
import com.crossmall.mapper.PaymentTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 履约服务:管理端状态流转(状态机校验)/ 超时关单 / 发货前退款 / 过期订单兜底对账
 *
 * <p>所有流转都走「状态机合法流转表校验 + DB 条件更新(status=期望值)」双保险:
 * 非法跳状态直接拒绝;并发下条件更新失败的请求视为过期请求拒绝。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderTrackMapper orderTrackMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final StockService stockService;
    private final OrderService orderService;

    // ==================== 管理端履约流转 ====================

    @Transactional
    public void transit(String orderNo, String targetCode, String location, String desc, String descEn) {
        Order order = requireByOrderNo(orderNo);
        OrderStatus current = OrderStatus.of(order.getStatus());
        OrderStatus target = OrderStatus.of(targetCode);

        if (target == OrderStatus.REFUNDED) {
            refund(order);
            return;
        }
        if (!current.canTransitTo(target)) {
            throw new BizException(400, "非法状态流转:「" + current.getLabel() + "」不能变更为「" + target.getLabel() + "」");
        }

        LambdaUpdateWrapper<Order> update = new LambdaUpdateWrapper<Order>()
                .set(Order::getStatus, target.name())
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, current.name());
        if (target == OrderStatus.COMPLETED) {
            update.set(Order::getCompletedAt, LocalDateTime.now());
        }
        if (orderMapper.update(null, update) == 0) {
            throw new BizException(400, "订单状态已变化,请刷新后重试");
        }

        TrackNode node = defaultNode(target);
        orderService.sendTrack(orderNo, target,
                location == null || location.isBlank() ? node.location : location,
                desc == null || desc.isBlank() ? node.desc : desc,
                descEn == null || descEn.isBlank() ? node.descEn : descEn);
        log.info("订单 {} 状态流转 {} → {}", orderNo, current, target);
    }

    /**
     * 发货前退款(PAID → REFUNDED):流水标记 REFUNDED + 回补库存(货未发出)
     */
    @Transactional
    public void refund(Order order) {
        if (orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getStatus, OrderStatus.REFUNDED.name())
                .set(Order::getCancelReason, "发货前退款")
                .eq(Order::getOrderNo, order.getOrderNo())
                .eq(Order::getStatus, OrderStatus.PAID.name())) == 0) {
            throw new BizException(400, "仅已支付未发货订单可退款");
        }
        paymentTransactionMapper.update(null, new LambdaUpdateWrapper<PaymentTransaction>()
                .set(PaymentTransaction::getStatus, "REFUNDED")
                .eq(PaymentTransaction::getOrderNo, order.getOrderNo())
                .eq(PaymentTransaction::getStatus, "SUCCESS"));
        compensateStock(order.getOrderNo());
        orderService.sendTrack(order.getOrderNo(), OrderStatus.REFUNDED, null,
                "退款已按支付快照原路退回", "Refunded per payment snapshot");
    }

    // ==================== 超时关单 ====================

    /**
     * MQ 延迟消息触发:到点未支付自动关单。
     * 与支付回调并发时,谁的条件更新(status=PENDING_PAYMENT)成功谁赢,输方自然跳过。
     */
    public void closeByTimeout(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            log.warn("关单消息对应订单不存在: {}", orderNo);
            return;
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            log.info("订单 {} 状态为 {},已支付或已流转,跳过关单", orderNo, order.getStatus());
            return;
        }
        if (order.getPayTimeoutAt().isAfter(LocalDateTime.now())) {
            log.info("订单 {} 未到支付截止时间,跳过(延迟等级取整误差)", orderNo);
            return;
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getStatus, OrderStatus.CANCELLED.name())
                .set(Order::getCancelReason, "超时未支付,系统自动关单")
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.name()));
        if (rows > 0) {
            compensateStock(orderNo);
            orderService.sendTrack(orderNo, OrderStatus.CANCELLED, null,
                    "超时未支付,系统自动关单", "Auto-closed due to payment timeout");
            log.info("订单 {} 超时未支付已自动关单并回补库存", orderNo);
        }
    }

    /**
     * 兜底对账:直接扫描超时未支付订单关单(防 MQ 消息丢失),
     * 供管理端手动触发与定时任务调用
     */
    @Transactional
    public int closeExpired(int limit) {
        List<Order> expired = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.name())
                .le(Order::getPayTimeoutAt, LocalDateTime.now())
                .last("limit " + limit));
        int closed = 0;
        for (Order order : expired) {
            int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                    .set(Order::getStatus, OrderStatus.CANCELLED.name())
                    .set(Order::getCancelReason, "超时未支付,兜底对账关单")
                    .eq(Order::getOrderNo, order.getOrderNo())
                    .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.name()));
            if (rows > 0) {
                compensateStock(order.getOrderNo());
                orderService.sendTrack(order.getOrderNo(), OrderStatus.CANCELLED, null,
                        "超时未支付,系统关单(兜底对账)", "Auto-closed by reconciliation");
                closed++;
            }
        }
        return closed;
    }

    // ==================== 内部 ====================

    private void compensateStock(String orderNo) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo));
        for (OrderItem item : items) {
            stockService.restoreDb(item.getSkuId(), item.getQuantity());
            stockService.restore(item.getSkuId(), item.getQuantity());
        }
    }

    private Order requireByOrderNo(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        return order;
    }

    private record TrackNode(String location, String desc, String descEn) {
    }

    /** 各履约节点的默认轨迹文案(管理端未填时兜底) */
    private TrackNode defaultNode(OrderStatus status) {
        return switch (status) {
            case STOCKED -> new TrackNode("Shenzhen CN", "仓库备货完成,待发运", "Stocked at Shenzhen warehouse");
            case DECLARED -> new TrackNode("Shenzhen CN", "出口报关申报完成", "Export declaration completed");
            case IN_TRANSIT -> new TrackNode("International Line", "国际干线运输已启运", "Departed on international line");
            case CUSTOMS_CLEARANCE -> new TrackNode("Destination Customs", "包裹抵达目的国,等待清关", "Arrived at destination, awaiting customs clearance");
            case DELIVERING -> new TrackNode("Local Sorting Center", "清关放行,尾程派送中", "Customs cleared, out for delivery");
            case COMPLETED -> new TrackNode("Destination", "买家已签收,订单妥投", "Delivered and signed");
            default -> new TrackNode(null, status.getLabel(), status.getLabelEn());
        };
    }
}
