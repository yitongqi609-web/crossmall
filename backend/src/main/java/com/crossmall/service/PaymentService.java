package com.crossmall.service;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.crossmall.common.exception.BizException;
import com.crossmall.config.CrossMallProperties;
import com.crossmall.entity.Order;
import com.crossmall.entity.PaymentTransaction;
import com.crossmall.fulfillment.OrderStatus;
import com.crossmall.mapper.OrderMapper;
import com.crossmall.mapper.PaymentTransactionMapper;
import com.crossmall.mq.producer.OrderProducer;
import com.crossmall.vo.PayCreateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 支付服务(模拟 PayPal/卡收单):
 * <p>支付幂等三道防线:
 * <ol>
 *   <li>Redis SETNX 防重闸门 —— 高频重复回调快速拦截</li>
 *   <li>支付流水条件更新(INIT→SUCCESS)+ txn_no 唯一索引 —— 并发回调只成功一次</li>
 *   <li>订单条件流转(PENDING_PAYMENT→PAID)—— 与超时关单/取消赛跑,输方自动补偿(流水置 VOID)</li>
 * </ol>
 * 金额校验:回调金额必须与流水一致,防止篡改;金额锚定订单汇率快照。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String PAY_DONE_KEY = "crossmall:pay:done:";

    private final PaymentTransactionMapper txnMapper;
    private final OrderMapper orderMapper;
    private final OrderProducer orderProducer;
    private final FulfillmentService fulfillmentService;
    private final CrossMallProperties properties;
    private final StringRedisTemplate redis;

    /**
     * 回调处理结果(不抛异常以便保留已落库状态)
     */
    public record NotifyResult(boolean success, String message, String txnNo) {
    }

    /**
     * 创建支付单:金额直接锚定订单汇率快照,生成支付流水
     */
    @Transactional
    public PayCreateVO createPayment(Long userId, String orderNo, String channel) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(404, "订单不存在");
        }
        if (OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())
                && order.getPayTimeoutAt().isBefore(LocalDateTime.now())) {
            fulfillmentService.closeByTimeout(orderNo);
            throw new BizException(400, "订单已超时关闭");
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BizException(400, "订单当前状态不可支付");
        }
        Long paid = txnMapper.selectCount(new LambdaQueryWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getOrderNo, orderNo)
                .eq(PaymentTransaction::getStatus, "SUCCESS"));
        if (paid > 0) {
            throw new BizException(400, "订单已支付,请勿重复支付");
        }
        // 旧的 INIT 流水作废,一单只保留一个有效收银台
        txnMapper.update(null, new LambdaUpdateWrapper<PaymentTransaction>()
                .set(PaymentTransaction::getStatus, "VOID")
                .eq(PaymentTransaction::getOrderNo, orderNo)
                .eq(PaymentTransaction::getStatus, "INIT"));

        PaymentTransaction txn = new PaymentTransaction();
        txn.setTxnNo("TXN" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + cn.hutool.core.util.RandomUtil.randomNumbers(6));
        txn.setOrderNo(orderNo);
        txn.setChannel(channel.toUpperCase());
        txn.setCurrency(order.getCurrency());
        txn.setFxRate(order.getFxRate());
        txn.setAmountLocal(order.getTotalLocalCents());
        txn.setAmountUsd(order.getTotalFeeCents());
        txn.setStatus("INIT");
        txnMapper.insert(txn);

        PayCreateVO vo = new PayCreateVO();
        vo.setOrderNo(orderNo);
        vo.setTxnNo(txn.getTxnNo());
        vo.setChannel(txn.getChannel());
        vo.setCurrency(order.getCurrency());
        vo.setSymbol(fxSymbol(order.getCurrency()));
        vo.setAmountLocal(order.getTotalLocalCents());
        vo.setExpireAt(order.getPayTimeoutAt());
        return vo;
    }

    /**
     * 支付网关异步回调(核心幂等逻辑)
     *
     * @param txnNo       支付流水号
     * @param amountLocal 回调本币金额(分)
     * @param sign        签名 md5(txnNo|amountLocal|secret)
     * @param payload     原始回调报文
     */
    @Transactional
    public NotifyResult handleNotify(String txnNo, long amountLocal, String sign, String payload) {
        // 1. 验签:防伪造回调
        String expected = SecureUtil.md5(txnNo + "|" + amountLocal + "|" + properties.getPay().getMockGatewaySecret());
        if (!expected.equalsIgnoreCase(sign)) {
            throw new BizException(400, "回调验签失败");
        }

        PaymentTransaction txn = txnMapper.selectOne(new LambdaQueryWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getTxnNo, txnNo));
        if (txn == null) {
            throw new BizException(404, "支付流水不存在");
        }

        // 2. Redis 防重:重复回调快速幂等返回
        Boolean first = redis.opsForValue().setIfAbsent(PAY_DONE_KEY + txnNo, "1", Duration.ofHours(24));
        if (first == null || !first) {
            log.info("支付流水 {} 重复回调,幂等返回", txnNo);
            return new NotifyResult(true, "重复回调,幂等处理", txnNo);
        }

        if ("SUCCESS".equals(txn.getStatus())) {
            return new NotifyResult(true, "流水已支付,幂等处理", txnNo);
        }
        if (!"INIT".equals(txn.getStatus())) {
            redis.delete(PAY_DONE_KEY + txnNo);
            return new NotifyResult(false, "支付流水已失效(订单已关闭)", txnNo);
        }
        // 3. 金额一致性校验:防篡改
        if (txn.getAmountLocal() != amountLocal) {
            redis.delete(PAY_DONE_KEY + txnNo);
            throw new BizException(400, "回调金额与支付流水不一致,疑似篡改");
        }

        // 4. 流水条件更新 INIT→SUCCESS:并发回调只有一次生效
        int txnRows = txnMapper.update(null, new LambdaUpdateWrapper<PaymentTransaction>()
                .set(PaymentTransaction::getStatus, "SUCCESS")
                .set(PaymentTransaction::getPaidAt, LocalDateTime.now())
                .set(PaymentTransaction::getCallbackPayload, payload)
                .eq(PaymentTransaction::getTxnNo, txnNo)
                .eq(PaymentTransaction::getStatus, "INIT"));
        if (txnRows == 0) {
            log.info("支付流水 {} 并发回调被条件更新挡下,幂等返回", txnNo);
            return new NotifyResult(true, "并发回调,幂等处理", txnNo);
        }

        // 5. 订单条件流转 PENDING_PAYMENT→PAID:与超时关单赛跑,输方补偿
        int orderRows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getStatus, OrderStatus.PAID.name())
                .set(Order::getPaidAt, LocalDateTime.now())
                .eq(Order::getOrderNo, txn.getOrderNo())
                .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.name()));
        if (orderRows == 0) {
            txnMapper.update(null, new LambdaUpdateWrapper<PaymentTransaction>()
                    .set(PaymentTransaction::getStatus, "VOID")
                    .eq(PaymentTransaction::getTxnNo, txnNo));
            redis.delete(PAY_DONE_KEY + txnNo);
            log.warn("支付流水 {} 对应订单已关闭,支付款将原路退回", txnNo);
            return new NotifyResult(false, "订单已关闭,支付款将原路退回", txnNo);
        }

        // 6. 事务提交后再写履约轨迹
        String orderNo = txn.getOrderNo();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                orderProducer.sendTrack(orderTrack(orderNo));
            }
        });
        log.info("支付成功:订单 {} 流水 {} 本币 {} {}", orderNo, txnNo, amountLocal, txn.getCurrency());
        return new NotifyResult(true, "支付成功", txnNo);
    }

    private com.crossmall.mq.model.TrackMessage orderTrack(String orderNo) {
        com.crossmall.mq.model.TrackMessage message = new com.crossmall.mq.model.TrackMessage();
        message.setOrderNo(orderNo);
        message.setStatus(OrderStatus.PAID.name());
        message.setLocation("Payment Gateway");
        message.setDescription("支付成功,等待仓库备货");
        message.setDescriptionEn("Payment received, awaiting fulfilment");
        message.setOccurredAt(LocalDateTime.now());
        return message;
    }

    private String fxSymbol(String currency) {
        return switch (currency) {
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "JPY" -> "¥";
            case "AUD" -> "A$";
            case "CNY" -> "CN¥";
            default -> "";
        };
    }
}
