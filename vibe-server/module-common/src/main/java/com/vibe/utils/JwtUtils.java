package com.vibe.utils;

import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT 工具类
 *
 * <p>负责 Token 的签发、解析、校验。</p>
 *
 * <p>Token 载荷结构：</p>
 * <pre>
 * {
 *   "userId": 1001,
 *   "userName": "zhangsan",
 *   "realName": "张三",
 *   "roles": ["PM", "ENGINEER"],
 *   "tenantType": "INTERNAL",
 *   "tenantId": null,
 *   "orgId": 101,
 *   "clientType": "PC"
 * }
 * </pre>
 *
 * @author vibe
 */
@Slf4j
@Component
public class JwtUtils {

    /** 签名密钥（HS256 至少 32 字节） */
    @Value("${vibe.jwt.secret:vibe-default-secret-key-please-change-in-production-32bytes}")
    private String secret;

    /** 签发者 */
    @Value("${vibe.jwt.issuer:vibe}")
    private String issuer;

    /**
     * 获取签名密钥
     */
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     *
     * @param claims        自定义载荷
     * @param tokenId       Token 唯一标识（jti），用于黑名单
     * @param ttlSeconds    有效期（秒）
     */
    public String generateToken(Map<String, Object> claims, String tokenId, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .id(tokenId)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(signingKey())
                .compact();
    }

    /**
     * 解析 Token，返回 Claims
     *
     * @throws BusinessException Token 无效/过期时抛出
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Token 已过期: {}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            log.warn("[JWT] Token 无效: {}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        } catch (Exception e) {
            log.error("[JWT] Token 解析异常", e);
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
    }

    /**
     * 校验 Token 是否有效（不抛异常，返回 boolean）
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Token 剩余有效期（秒）
     */
    public long getRemainingTtl(String token) {
        Claims claims = parseToken(token);
        long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
        return remainingMs > 0 ? remainingMs / 1000 : 0;
    }

    /**
     * 是否需要续签（剩余有效期 < 阈值）
     */
    public boolean needsRenew(String token, long thresholdSeconds) {
        try {
            return getRemainingTtl(token) < thresholdSeconds;
        } catch (Exception e) {
            return false;
        }
    }

    /* ============ 载荷字段快捷读取 ============ */

    public Long getUserId(Claims claims) {
        Object v = claims.get("userId");
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }

    public String getUserName(Claims claims) {
        Object v = claims.get("userName");
        return v == null ? null : v.toString();
    }

    public String getRealName(Claims claims) {
        Object v = claims.get("realName");
        return v == null ? null : v.toString();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims claims) {
        Object v = claims.get("roles");
        if (v instanceof List<?>) {
            return (List<String>) v;
        }
        return List.of();
    }

    public String getTenantType(Claims claims) {
        Object v = claims.get("tenantType");
        return v == null ? null : v.toString();
    }

    public Long getTenantId(Claims claims) {
        Object v = claims.get("tenantId");
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }

    public Long getOrgId(Claims claims) {
        Object v = claims.get("orgId");
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }

    public String getClientType(Claims claims) {
        Object v = claims.get("clientType");
        return v == null ? null : v.toString();
    }

    public String getTokenId(Claims claims) {
        return claims.getId();
    }
}
