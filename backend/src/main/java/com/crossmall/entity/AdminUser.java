package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_user")
public class AdminUser {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private LocalDateTime createTime;
}
