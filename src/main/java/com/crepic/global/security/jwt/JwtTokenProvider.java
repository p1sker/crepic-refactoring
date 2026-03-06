package com.crepic.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;
    // ⭐️ S+++급 디테일 1: 매 요청마다 만들지 않고, 튼튼한 파서를 하나만 만들어 평생 재사용 (성능 극대화)
    private final JwtParser jwtParser;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration}") long accessTokenExpiration) {

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;

        // 빈이 생성될 때 파서를 미리 조립해둡니다.
        this.jwtParser = Jwts.parser().verifyWith(this.key).build();
    }

    /**
     * 1. 🎫 엑세스 토큰(입장권) 발급
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
     * 2. 🔍 토큰에서 유저 ID(PK) 꺼내기
     */
    public Long getMemberIdFromToken(String token) {
        // ⭐️ 최적화: 미리 만들어둔 파서를 그대로 사용!
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 3. 🛡️ 토큰 위변조 및 만료 검증 (경찰 역할)
     */
    public boolean validateToken(String token) {
        try {
            // ⭐️ 최적화: 역시 미리 만들어둔 파서를 사용!
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
     * 4. 🏷️ 토큰에서 권한(Role) 정보 꺼내기
     */
    public String getRoleFromToken(String token) {
        return jwtParser.parseSignedClaims(token)
                .getPayload()
                .get("role", String.class); // 우리가 넣었던 "role" 키값을 찾아옵니다!
    }
}