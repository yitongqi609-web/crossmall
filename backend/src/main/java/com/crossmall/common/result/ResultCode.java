package com.crossmall.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码约定:200 成功;4xx 客户端错误;5xx 业务/服务错误
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    BIZ_FAIL(500, "业务处理失败");

    private final int code;
    private final String message;
}
