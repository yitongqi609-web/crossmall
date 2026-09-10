package com.crossmall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.crossmall.entity.Order;
import com.crossmall.entity.OrderItem;
import com.crossmall.fulfillment.OrderStatus;
import com.crossmall.mapper.OrderItemMapper;
import com.crossmall.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据看板:销售总览 / 近 7 日趋势 / 状态分布 / 热销 Top5
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** 有效销售口径:已支付起至妥投的全部状态 */
    private static final List<String> PAID_STATUSES = List.of(
            OrderStatus.PAID.name(), OrderStatus.STOCKED.name(), OrderStatus.DECLARED.name(),
            OrderStatus.IN_TRANSIT.name(), OrderStatus.CUSTOMS_CLEARANCE.name(),
            OrderStatus.DELIVERING.name(), OrderStatus.COMPLETED.name());

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public Map<String, Object> overview() {
        Map<String, Object> vo = new LinkedHashMap<>();

        // 总览:有效订单数 + GMV(USD 分,多币种订单以 USD 基准币汇总)
        Map<String, Object> total = orderMapper.selectMaps(new QueryWrapper<Order>()
                .select("COUNT(*) AS cnt, IFNULL(SUM(total_fee_cents),0) AS gmv")
                .in("status", PAID_STATUSES)).get(0);
        vo.put("paidOrders", ((Number) total.get("cnt")).longValue());
        vo.put("gmvUsdCents", ((Number) total.get("gmv")).longValue());

        vo.put("pendingPayment", orderMapper.selectCount(
                new QueryWrapper<Order>().eq("status", OrderStatus.PENDING_PAYMENT.name())));
        vo.put("inFulfillment", orderMapper.selectCount(
                new QueryWrapper<Order>().in("status", List.of(
                        OrderStatus.PAID.name(), OrderStatus.STOCKED.name(), OrderStatus.DECLARED.name(),
                        OrderStatus.IN_TRANSIT.name(), OrderStatus.CUSTOMS_CLEARANCE.name(),
                        OrderStatus.DELIVERING.name()))));
        vo.put("completed", orderMapper.selectCount(
                new QueryWrapper<Order>().eq("status", OrderStatus.COMPLETED.name())));

        // 近 7 日趋势(含未支付,反映真实流量)
        List<Map<String, Object>> trend = orderMapper.selectMaps(new QueryWrapper<Order>()
                .select("DATE(create_time) AS d, COUNT(*) AS cnt, IFNULL(SUM(total_fee_cents),0) AS gmv")
                .ge("create_time", LocalDate.now().minusDays(6).atStartOfDay())
                .groupBy("DATE(create_time)")
                .orderByAsc("d"));
        vo.put("trend", trend.stream().map(row -> Map.of(
                "date", String.valueOf(row.get("d")),
                "orders", ((Number) row.get("cnt")).longValue(),
                "gmvUsdCents", ((Number) row.get("gmv")).longValue())).toList());

        // 状态分布
        List<Map<String, Object>> dist = orderMapper.selectMaps(new QueryWrapper<Order>()
                .select("status, COUNT(*) AS cnt")
                .groupBy("status"));
        vo.put("statusDist", dist.stream().map(row -> {
            OrderStatus status = OrderStatus.of((String) row.get("status"));
            return Map.of(
                    "status", status.name(),
                    "label", status.getLabel(),
                    "labelEn", status.getLabelEn(),
                    "count", ((Number) row.get("cnt")).longValue());
        }).toList());

        // 热销 Top5(有效订单口径)
        List<String> paidOrderNos = orderMapper.selectList(new QueryWrapper<Order>()
                        .select("order_no").in("status", PAID_STATUSES))
                .stream().map(Order::getOrderNo).toList();
        if (paidOrderNos.isEmpty()) {
            vo.put("topGoods", List.of());
        } else {
            List<Map<String, Object>> top = orderItemMapper.selectMaps(new QueryWrapper<OrderItem>()
                    .select("spu_id AS spuId, spu_title AS title, spu_title_en AS titleEn, " +
                            "SUM(quantity) AS quantity, SUM(subtotal_cents) AS amountUsd")
                    .in("order_no", paidOrderNos)
                    .groupBy("spu_id", "spu_title", "spu_title_en")
                    .orderByDesc("quantity")
                    .last("limit 5"));
            vo.put("topGoods", top.stream().map(row -> Map.of(
                    "spuId", row.get("spuId"),
                    "title", row.get("title"),
                    "titleEn", row.get("titleEn"),
                    "quantity", ((Number) row.get("quantity")).longValue(),
                    "amountUsdCents", ((Number) row.get("amountUsd")).longValue())).collect(Collectors.toList()));
        }
        return vo;
    }
}
