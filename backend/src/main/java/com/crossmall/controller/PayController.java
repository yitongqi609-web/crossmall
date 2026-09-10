package com.crossmall.controller;

import com.crossmall.common.context.UserContext;
import com.crossmall.common.result.Result;
import com.crossmall.dto.PayCreateDTO;
import com.crossmall.service.PaymentService;
import com.crossmall.vo.PayCreateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "08-支付")
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PaymentService paymentService;

    @Operation(summary = "发起支付(创建支付流水,跳转模拟收银台)")
    @PostMapping("/{orderNo}")
    public Result<PayCreateVO> create(@PathVariable String orderNo,
                                      @Valid @RequestBody PayCreateDTO dto) {
        return Result.ok(paymentService.createPayment(UserContext.getUserId(), orderNo, dto.getChannel()));
    }
}
