package com.joinup.infrastructure.security;

import com.joinup.common.context.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 的生成、解析与校验工具。
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createToken(Long userId) {
        return createToken(LoginUser.builder().userId(userId).build());
    }

    public String createToken(LoginUser loginUser) {
        // 登录态快照直接写入 claims，后续接口可少查一次用户表。
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getExpireMinutes() * 60);
        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .claim("username", loginUser.getUsername())
                .claim("nickname", loginUser.getNickname())
                .claim("creditScore", loginUser.getCreditScore())
                .claim("status", loginUser.getStatus())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey())
                .compact();
    }

    public Long parseUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public LoginUser parseLoginUser(String token) {
        // 统一把 claims 转回 LoginUser，供过滤器和业务层直接使用。
        Claims claims = parseClaims(token);
        return LoginUser.builder()
                .userId(Long.valueOf(claims.getSubject()))
                .username(claims.get("username", String.class))
                .nickname(claims.get("nickname", String.class))
                .creditScore(getIntegerClaim(claims, "creditScore"))
                .status(getIntegerClaim(claims, "status"))
                .build();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // 这里不抛业务异常，让过滤器按未登录处理即可。
            return false;
        }
    }

    public long getExpireSeconds() {
        return jwtProperties.getExpireMinutes() * 60;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Integer getIntegerClaim(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
