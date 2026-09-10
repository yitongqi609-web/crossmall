package com.crossmall.controller.admin;

import com.crossmall.common.result.Result;
import com.crossmall.entity.FxRate;
import com.crossmall.service.FxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "A5-汇率管理")
@RestController
@RequestMapping("/api/admin/fx")
@RequiredArgsConstructor
public class AdminFxController {

    private final FxService fxService;

    @Operation(summary = "汇率列表")
    @GetMapping("/rates")
    public Result<List<FxRate>> list() {
        return Result.ok(fxService.listAll());
    }

    @Operation(summary = "手动改汇率")
    @PutMapping("/rates/{currency}")
    public Result<Void> update(@PathVariable String currency, @RequestParam BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "汇率必须大于 0");
        }
        fxService.updateRate(currency.toUpperCase(), rate, "admin");
        return Result.ok();
    }

    @Operation(summary = "立即拉取汇率(模拟第三方汇率源)")
    @PostMapping("/pull")
    public Result<Integer> pull() {
        return Result.ok(fxService.refreshFromSource());
    }
}
