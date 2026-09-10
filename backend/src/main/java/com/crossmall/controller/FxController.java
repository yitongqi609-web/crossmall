package com.crossmall.controller;

import com.crossmall.common.result.Result;
import com.crossmall.entity.FxRate;
import com.crossmall.service.FxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "05-汇率(公开)")
@RestController
@RequestMapping("/api/fx")
@RequiredArgsConstructor
public class FxController {

    private final FxService fxService;

    @Operation(summary = "全部币种汇率(币种切换器用)")
    @GetMapping("/rates")
    public Result<List<FxRate>> rates() {
        return Result.ok(fxService.listAll());
    }
}
