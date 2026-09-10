package com.crossmall.controller.mock;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crossmall.common.exception.BizException;
import com.crossmall.common.result.Result;
import com.crossmall.config.CrossMallProperties;
import com.crossmall.dto.MockNotifyDTO;
import com.crossmall.entity.PaymentTransaction;
import com.crossmall.mapper.PaymentTransactionMapper;
import com.crossmall.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟第三方支付网关(独立于商城业务,模拟 PayPal/收单机构的两个行为):
 * 1. 买家在网关收银台点击「立即支付」→ 网关生成签名,异步回调商城
 * 2. 回调 Webhook 公开暴露,供 curl/脚本重放验证幂等
 */
@Tag(name = "09-模拟支付网关")
@RestController
@RequestMapping("/api/mock/pay")
@RequiredArgsConstructor
public class MockPayGatewayController {

    private final PaymentService paymentService;
    private final PaymentTransactionMapper txnMapper;
    private final CrossMallProperties properties;

    @Operation(summary = "买家在收银台确认支付(网关侧模拟,内部触发异步回调)")
    @PostMapping("/submit")
    public Result<String> submit(@RequestParam String txnNo) {
        PaymentTransaction txn = txnMapper.selectOne(new LambdaQueryWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getTxnNo, txnNo));
        if (txn == null) {
            throw new BizException(404, "支付流水不存在");
        }
        if (!"INIT".equals(txn.getStatus())) {
            throw new BizException(400, "该支付流水已失效或已支付,请重新发起");
        }
        long amount = txn.getAmountLocal();
        String sign = sign(txnNo, amount);
        PaymentService.NotifyResult result = paymentService.handleNotify(txnNo, amount, sign,
                "{\"mock\":\"gateway\",\"event\":\"payment.succeeded\"}");
        return result.success() ? Result.ok(result.message()) : Result.fail(400, result.message());
    }

    @Operation(summary = "网关异步回调 Webhook(公开接口,可重放验证幂等)")
    @PostMapping("/notify")
    public Result<String> notify(@Valid @RequestBody MockNotifyDTO dto) {
        PaymentService.NotifyResult result = paymentService.handleNotify(
                dto.getTxnNo(), dto.getAmountLocal(), dto.getSign(), dto.getPayload());
        return result.success() ? Result.ok(result.message()) : Result.fail(400, result.message());
    }

    private String sign(String txnNo, long amountLocal) {
        return SecureUtil.md5(txnNo + "|" + amountLocal + "|"
                + properties.getPay().getMockGatewaySecret());
    }
}
