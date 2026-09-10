package com.crossmall.controller;

import com.crossmall.common.context.UserContext;
import com.crossmall.common.result.Result;
import com.crossmall.entity.User;
import com.crossmall.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01-买家账号")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @Operation(summary = "我的信息")
    @GetMapping("/me")
    public Result<User> me() {
        return Result.ok(authService.me(UserContext.getUserId()));
    }
}
