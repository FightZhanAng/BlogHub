package com.blog.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** Token 类型声明：区分 access / refresh，避免用 access token 反复续期 */
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    private Key key;

    @PostConstruct
    public void init() {
        // 之前无条件 Base64 解码：secret 不是合法 Base64（例如 docker-compose 里带 "-" 的
        // 那一段）时 getDecoder().decode 会抛 IllegalArgumentException，@PostConstruct 失败
        // 导致整个应用起不来。这里先尝试 Base64，失败则回退到 UTF-8 字节并告警。
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            log.warn("jwt.secret 不是合法 Base64，已回退为 UTF-8 字节解析。"
                    + "生产环境请配置合法的 Base64 密钥（至少 32 字节）。");
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            // HS256 要求密钥至少 256 bit；过短会让 Keys.hmacShaKeyFor 抛异常
            throw new IllegalStateException(
                    "jwt.secret 长度不足：解码后 " + keyBytes.length + " 字节，至少需要 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /** 生成 Access Token */
    public String generateToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, TYPE_ACCESS, expiration);
    }

    /** 生成 Refresh Token（更长有效期） */
    public String generateRefreshToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, TYPE_REFRESH, refreshExpiration);
    }

    private String buildToken(Long userId, String username, String role, String type, long ttl) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("typ", type)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttl))
                .signWith(key)
                .compact();
    }

    /** 解析 Token */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** 验证 Token 有效性 */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 校验 Token 有效且类型匹配 */
    public boolean validateToken(String token, String expectedType) {
        try {
            Claims claims = parseToken(token);
            String typ = claims.get("typ", String.class);
            // 兼容历史 Token（无 typ）：按 access 处理
            if (typ == null) {
                return TYPE_ACCESS.equals(expectedType);
            }
            return expectedType.equals(typ);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 从 Token 获取用户 ID */
    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    /** 从 Token 获取用户名 */
    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    /** 从 Token 获取角色 */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }
}
