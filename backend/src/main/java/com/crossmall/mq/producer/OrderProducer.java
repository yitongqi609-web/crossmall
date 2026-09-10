package com.crossmall.mq.producer;

import cn.hutool.json.JSONUtil;
import com.crossmall.config.CrossMallProperties;
import com.crossmall.mq.model.CloseMessage;
import com.crossmall.mq.model.TrackMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 订单 MQ 生产者:延迟关单消息 + 履约轨迹消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {

    /** RocketMQ 延迟等级对应秒数:1=1s 2=5s 3=10s 4=30s 5=1m ... 16=30m 17=1h */
    private static final long[] DELAY_LEVEL_SECONDS = {
            1, 5, 10, 30, 60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 1200, 1800
    };

    private final RocketMQTemplate rocketMQTemplate;
    private final CrossMallProperties properties;

    /**
     * 下单成功后发送延迟关单消息:到点未支付自动关单
     */
    public void sendCloseDelay(String orderNo) {
        int level = toDelayLevel(properties.getOrder().getPayTimeout());
        rocketMQTemplate.syncSend(properties.getTopic().getOrderClose(),
                org.springframework.messaging.support.MessageBuilder
                        .withPayload(JSONUtil.toJsonStr(new CloseMessage(orderNo))).build(),
                3000, level);
        log.info("已发送订单 {} 超时关单延迟消息,等级 {}(约 {}s)", orderNo, level,
                level <= DELAY_LEVEL_SECONDS.length ? DELAY_LEVEL_SECONDS[level - 1] : 3600);
    }

    public void sendTrack(TrackMessage message) {
        rocketMQTemplate.syncSend(properties.getTopic().getOrderTrack(),
                JSONUtil.toJsonStr(message));
    }

    /** 把配置的超时时长向上取整到最近的 RocketMQ 延迟等级(消息不会早于超时时间到达) */
    static int toDelayLevel(Duration timeout) {
        long seconds = Math.max(1, timeout.getSeconds());
        for (int i = 0; i < DELAY_LEVEL_SECONDS.length; i++) {
            if (seconds <= DELAY_LEVEL_SECONDS[i]) {
                return i + 1;
            }
        }
        return 17;
    }
}
