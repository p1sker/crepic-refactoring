package com.crepic.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration; // ⭐️ Refresh Token 수명 추가

    // ⭐️ S+++급 디테일 1: 매 요청마다 만들지 않고, 튼튼한 파서를 하나만 만들어 평생 재사용 (성능 극대화)
    private final JwtParser jwtParser;
    private final StringRedisTemplate redisTemplate; // ⭐️ 블랙리스트 검증을 위한 Redis 추가

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration}") long accessTokenExpiration,
            // (application.yml에 설정이 없으면 기본값 14일(1209600000ms)로 작동하도록 안전장치 추가)
            @Value("${jwt.refresh-expiration:1209600000}") long refreshTokenExpiration,
            StringRedisTemplate redisTemplate) {

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.redisTemplate = redisTemplate;

        // 빈이 생성될 때 파서를 미리 조립해둡니다.
        this.jwtParser = Jwts.parser().verifyWith(this.key).build();
    }

    /**
     * 1. 🎫 Access Token(입장권) 발급
     */
    public String createAccessToken(Long memberId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(memberId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 2. 🔄 Refresh Token(재발급권) 발급 (⭐️ 추가됨)
     * - Refresh Token은 권한(role)을 담지 않고 오직 식별자(memberId)만 담아 가볍게 만듭니다.
     */
    public String createRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 3. 🔍 토큰에서 유저 ID(PK) 꺼내기
     */
    public Long getMemberIdFromToken(String token) {
        // ⭐️ 최적화: 미리 만들어둔 파서를 그대로 사용!
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 4. 🛡️ 토큰 위변조, 만료 및 블랙리스트 검증 (경찰 역할)
     */
    public boolean validateToken(String token) {
        try {
            // ⭐️ S+++급 핵심 보안 로직: Redis에 블랙리스트(로그아웃)로 등록된 토큰인지 우선 확인!
            // 파싱 연산을 하기 전에 캐시부터 찔러서 CPU 낭비를 막습니다.
            if (Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + token))) {
                log.warn("🚨 블랙리스트에 등록된 Access Token 접근 차단");
                return false;
            }

            // ⭐️ 최적화: 미리 만들어둔 파서를 사용!
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            // ⭐️ S+++급 디테일 2: 해커의 로그 폭탄(DDoS) 공격 방어를 위해 ERROR -> DEBUG로 강등
            log.debug("🚨 잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.debug("⏳ 만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.debug("🚫 지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.debug("❓ JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    /**
     * 5. 🏷️ 토큰에서 권한(Role) 정보 꺼내기
     */
    public String getRoleFromToken(String token) {
        return jwtParser.parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    /**
     * 6. ⏱️ 토큰의 남은 유효시간(TTL) 계산 (⭐️ 추가됨)
     * - 로그아웃 시 Redis 블랙리스트의 수명을 정확히 설정하기 위해 사용합니다.
     */
    public Long getExpiration(String accessToken) {
        Date expiration = jwtParser.parseSignedClaims(accessToken).getPayload().getExpiration();
        long now = new Date().getTime();
        return (expiration.getTime() - now); // 남은 밀리초 반환
    }
}