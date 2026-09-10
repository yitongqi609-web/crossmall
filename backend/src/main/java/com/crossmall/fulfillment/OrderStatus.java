package com.crossmall.fulfillment;

import com.crossmall.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * 订单履约状态机
 * <p>正向主链路:PENDING_PAYMENT → PAID → STOCKED → DECLARED → IN_TRANSIT
 * → CUSTOMS_CLEARANCE → DELIVERING → COMPLETED
 * <p>分支:PENDING_PAYMENT → CANCELLED(用户取消/超时关单);PAID → REFUNDED(发货前退款)
 * <p>合法流转表是唯一真相源:不在表中的流转一律拒绝,杜绝非法跳状态。
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    PENDING_PAYMENT("待付款", "Pending Payment"),
    PAID("已付款/待发货", "Paid"),
    STOCKED("备货完成", "Stocked"),
    DECLARED("已报关", "Export Declared"),
    IN_TRANSIT("干线运输中", "In Transit"),
    CUSTOMS_CLEARANCE("清关中", "Customs Clearance"),
    DELIVERING("派送中", "Out for Delivery"),
    COMPLETED("已妥投", "Delivered"),
    CANCELLED("已取消", "Cancelled"),
    REFUNDED("已退款", "Refunded");

    private final String label;
    private final String labelEn;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING_PAYMENT, Set.of(PAID, CANCELLED),
            PAID, Set.of(STOCKED, REFUNDED),
            STOCKED, Set.of(DECLARED),
            DECLARED, Set.of(IN_TRANSIT),
            IN_TRANSIT, Set.of(CUSTOMS_CLEARANCE),
            CUSTOMS_CLEARANCE, Set.of(DELIVERING),
            DELIVERING, Set.of(COMPLETED)
    );

    public boolean canTransitTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public static OrderStatus of(String code) {
        for (OrderStatus s : values()) {
            if (s.name().equals(code)) {
                return s;
            }
        }
        throw new BizException(400, "未知订单状态: " + code);
    }

    public boolean isFinished() {
        return this == COMPLETED || this == CANCELLED || this == REFUNDED;
    }
}
