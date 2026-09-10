package com.crossmall.common.context;

/**
 * 登录用户上下文(由认证拦截器写入,请求结束清理)
 */
public class UserContext {

    public record LoginUser(Long userId, String role) {
    }

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
