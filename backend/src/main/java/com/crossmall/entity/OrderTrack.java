package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_track")
public class OrderTrack {

    private Long id;
    private String orderNo;
    /** 轨迹节点状态,见 fulfillment.OrderStatus */
    private String status;
    /** 节点地点,如 Shenzhen CN */
    private String location;
    private String description;
    private String descriptionEn;
    private LocalDateTime occurredAt;
    private LocalDateTime createTime;
}
