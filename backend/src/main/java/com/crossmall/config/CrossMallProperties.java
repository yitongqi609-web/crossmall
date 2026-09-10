package com.crossmall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * crossmall.* 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "crossmall")
public class CrossMallProperties {

    private Jwt jwt = new Jwt();
    private Pay pay = new Pay();
    private Order order = new Order();
    private Fx fx = new Fx();
    private Topic topic = new Topic();

    @Data
    public static class Jwt {
        private String secret;
        private long expireHours = 72;
    }

    @Data
    public static class Pay {
        /** 模拟支付网关回调验签密钥 */
        private String mockGatewaySecret;
    }

    @Data
    public static class Order {
        /** 支付超时关单时长 */
        private Duration payTimeout = Duration.ofMinutes(30);
    }

    @Data
    public static class Fx {
        /** 汇率定时拉取 cron */
        private String pullCron = "0 0 */2 * * ?";
    }

    @Data
    public static class Topic {
        /** 订单超时关单延迟消息 Topic */
        private String orderClose;
        /** 履约轨迹消息 Topic */
        private String orderTrack;
    }
}
