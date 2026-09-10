package com.crossmall.controller.admin;

import com.crossmall.common.result.Result;
import com.crossmall.entity.Category;
import com.crossmall.service.AdminGoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "A3-分类管理")
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminGoodsService adminGoodsService;

    @Operation(summary = "分类列表")
    @GetMapping
    public Result<List<Category>> list() {
        return Result.ok(adminGoodsService.listCategory());
    }

    @Operation(summary = "保存分类(新增/编辑)")
    @PostMapping
    public Result<Void> save(@RequestBody Category category) {
        adminGoodsService.saveCategory(category);
        return Result.ok();
    }

    @Operation(summary = "删除分类(分类下有商品时拒绝)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminGoodsService.deleteCategory(id);
        return Result.ok();
    }
}
