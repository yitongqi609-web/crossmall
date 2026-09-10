package com.crossmall.controller.admin;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crossmall.common.exception.BizException;
import com.crossmall.common.result.Result;
import com.crossmall.common.util.JwtUtil;
import com.crossmall.dto.AdminLoginDTO;
import com.crossmall.entity.AdminUser;
import com.crossmall.mapper.AdminUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "A1-管理端登录")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminUserMapper adminUserMapper;
    private final JwtUtil jwtUtil;

    public record TokenVO(String token, String username, String nickname) {
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, dto.getUsername()));
        if (admin == null || !BCrypt.checkpw(dto.getPassword(), admin.getPassword())) {
            throw new BizException(400, "用户名或密码错误");
        }
        String token = jwtUtil.create(admin.getId(), JwtUtil.ROLE_ADMIN);
        return Result.ok(new TokenVO(token, admin.getUsername(), admin.getNickname()));
    }
}
