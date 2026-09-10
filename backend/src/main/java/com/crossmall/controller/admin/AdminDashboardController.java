package com.crossmall.controller.admin;

import com.crossmall.common.result.Result;
import com.crossmall.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "A8-数据看板")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "看板总览(销售/趋势/状态分布/热销)")
    @GetMapping
    public Result<Map<String, Object>> overview() {
        return Result.ok(dashboardService.overview());
    }
}
