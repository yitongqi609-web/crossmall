package com.crossmall.mq.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 履约轨迹消息:订单状态流转 → MQ 异步落轨迹
 */
@Data
public class TrackMessage {

    private String orderNo;
    private String status;
    private String location;
    private String description;
    private String descriptionEn;
    private LocalDateTime occurredAt;
}
