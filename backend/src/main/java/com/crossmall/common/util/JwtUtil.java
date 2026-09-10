package com.crossmall.common.util;

import com.crossmall.common.exception.BizException;
import com.crossmall.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具(买家与管理员共用,通过 role claim 区分)
 */
@Component
public class JwtUtil {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    private final SecretKey key;
    private final long expireHours;

    public JwtUtil(@Value("${crossmall.jwt.secret}") String secret,
                   @Value("${crossmall.jwt.expire-hours}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireHours = expireHours;
    }

    public String create(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireHours * 3600_000L))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 token,非法/过期抛 401
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "登录已过期,请重新登录");
        }
    }
}
