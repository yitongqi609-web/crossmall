package com.crossmall.config;

import com.crossmall.common.util.JwtUtil;
import com.crossmall.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置:拦截器注册 + 跨域
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 买家端:/api/** 需要登录,公开接口与管理端接口单独放行
        registry.addInterceptor(new AuthInterceptor(jwtUtil, JwtUtil.ROLE_USER))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/home/**",
                        "/api/goods/**",
                        "/api/fx/**",
                        "/api/mock/**",
                        "/api/admin/**")
                .order(1);
        // 管理端:/api/admin/** 需要 admin 角色
        registry.addInterceptor(new AuthInterceptor(jwtUtil, JwtUtil.ROLE_ADMIN))
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login")
                .order(2);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
