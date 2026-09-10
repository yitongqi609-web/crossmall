package com.crossmall.mq.consumer;

import cn.hutool.json.JSONUtil;
import com.crossmall.mq.model.CloseMessage;
import com.crossmall.service.FulfillmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 超时关单延迟消息消费者:到点检查订单,未支付则自动关单回补库存
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${crossmall.topic.order-close}",
        consumerGroup = "crossmall-order-close-consumer")
public class OrderCloseConsumer implements RocketMQListener<String> {

    private final FulfillmentService fulfillmentService;

    @Override
    public void onMessage(String message) {
        try {
            String orderNo = JSONUtil.toBean(message, CloseMessage.class).getOrderNo();
            fulfillmentService.closeByTimeout(orderNo);
        } catch (Exception e) {
            // 抛出触发 RocketMQ 重试;多次失败进入死信后由兜底对账任务接管
            log.error("超时关单消息处理失败: {}", message, e);
            throw e;
        }
    }
}
