package com.crossmall.security;

import com.crossmall.common.context.UserContext;
import com.crossmall.common.exception.BizException;
import com.crossmall.common.result.ResultCode;
import com.crossmall.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器:校验 Authorization: Bearer xxx 并写入 UserContext
 * 通过 expectedRole 区分买家端与管理端
 */
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final String expectedRole;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        Claims claims = jwtUtil.parse(auth.substring(7));
        String role = claims.get("role", String.class);
        if (!expectedRole.equals(role)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage());
        }
        UserContext.set(new UserContext.LoginUser(Long.valueOf(claims.getSubject()), role));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
