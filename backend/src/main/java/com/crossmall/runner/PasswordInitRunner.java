package com.crossmall.runner;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crossmall.entity.AdminUser;
import com.crossmall.entity.User;
import com.crossmall.mapper.AdminUserMapper;
import com.crossmall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 初始密码升级:把种子数据里 {raw} 前缀的明文密码升级为 BCrypt
 * (SQL 里无法预生成 BCrypt,由应用启动时一次性完成)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordInitRunner implements ApplicationRunner {

    private static final String RAW_PREFIX = "{raw}";

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;

    @Override
    public void run(ApplicationArguments args) {
        List<User> rawUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .likeRight(User::getPassword, RAW_PREFIX));
        for (User user : rawUsers) {
            user.setPassword(encode(user.getPassword()));
            userMapper.updateById(user);
        }

        List<AdminUser> rawAdmins = adminUserMapper.selectList(new LambdaQueryWrapper<AdminUser>()
                .likeRight(AdminUser::getPassword, RAW_PREFIX));
        for (AdminUser admin : rawAdmins) {
            admin.setPassword(encode(admin.getPassword()));
            adminUserMapper.updateById(admin);
        }

        if (!rawUsers.isEmpty() || !rawAdmins.isEmpty()) {
            log.info("已将 {} 个买家账号与 {} 个管理员账号的初始密码升级为 BCrypt", rawUsers.size(), rawAdmins.size());
        }
    }

    private String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword.substring(RAW_PREFIX.length()), BCrypt.gensalt());
    }
}
