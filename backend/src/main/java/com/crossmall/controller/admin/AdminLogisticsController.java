package com.crossmall.controller.admin;

import com.crossmall.common.result.Result;
import com.crossmall.dto.LineDTO;
import com.crossmall.entity.LogisticsLine;
import com.crossmall.service.AdminGoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "A4-物流线路")
@RestController
@RequestMapping("/api/admin/logistics")
@RequiredArgsConstructor
public class AdminLogisticsController {

    private final AdminGoodsService adminGoodsService;

    @Operation(summary = "线路列表")
    @GetMapping("/lines")
    public Result<List<LogisticsLine>> list() {
        return Result.ok(adminGoodsService.listLines());
    }

    @Operation(summary = "保存线路(新增/编辑)")
    @PostMapping("/lines")
    public Result<Void> save(@Valid @RequestBody LineDTO dto) {
        adminGoodsService.saveLine(dto);
        return Result.ok();
    }

    @Operation(summary = "删除线路")
    @DeleteMapping("/lines/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminGoodsService.deleteLine(id);
        return Result.ok();
    }
}
