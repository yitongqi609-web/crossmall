package com.crossmall.mq.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单超时关单延迟消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloseMessage {

    private String orderNo;
}
