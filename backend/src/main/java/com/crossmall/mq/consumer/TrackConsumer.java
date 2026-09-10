package com.crossmall.mq.consumer;

import cn.hutool.json.JSONUtil;
import com.crossmall.entity.OrderTrack;
import com.crossmall.mapper.OrderTrackMapper;
import com.crossmall.mq.model.TrackMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 履约轨迹消费者:状态流转事件异步落库,主链路不因轨迹写放大而变慢
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${crossmall.topic.order-track}",
        consumerGroup = "crossmall-order-track-consumer")
public class TrackConsumer implements RocketMQListener<String> {

    private final OrderTrackMapper orderTrackMapper;

    @Override
    public void onMessage(String message) {
        try {
            TrackMessage track = JSONUtil.toBean(message, TrackMessage.class);
            OrderTrack entity = new OrderTrack();
            entity.setOrderNo(track.getOrderNo());
            entity.setStatus(track.getStatus());
            entity.setLocation(track.getLocation());
            entity.setDescription(track.getDescription());
            entity.setDescriptionEn(track.getDescriptionEn());
            entity.setOccurredAt(track.getOccurredAt());
            orderTrackMapper.insert(entity);
        } catch (Exception e) {
            log.error("履约轨迹消息处理失败: {}", message, e);
            throw e;
        }
    }
}
