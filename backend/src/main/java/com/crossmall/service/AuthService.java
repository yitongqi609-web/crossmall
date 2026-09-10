package com.crossmall.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crossmall.common.exception.BizException;
import com.crossmall.common.util.JwtUtil;
import com.crossmall.dto.LoginDTO;
import com.crossmall.dto.RegisterDTO;
import com.crossmall.entity.User;
import com.crossmall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 买家账号服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public record TokenVO(String token, Long userId, String email, String nickname) {
    }

    public void register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, dto.getEmail()));
        if (exists > 0) {
            throw new BizException(400, "该邮箱已注册");
        }
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? dto.getEmail().split("@")[0] : dto.getNickname());
        user.setStatus(1);
        userMapper.insert(user);
    }

    public TokenVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, dto.getEmail()));
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BizException(400, "邮箱或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用");
        }
        String token = jwtUtil.create(user.getId(), JwtUtil.ROLE_USER);
        return new TokenVO(token, user.getId(), user.getEmail(), user.getNickname());
    }

    public User me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }
}
