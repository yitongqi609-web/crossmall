package com.crossmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {

    private Long id;
    private String email;
    private String password;
    private String nickname;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
