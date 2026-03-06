package com.crepic.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
// ⭐️ S+++급 디테일: OncePerRequestFilter를 상속받아 한 번의 요청당 단 한 번만 실행됨을 보장합니다.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. 요청 헤더에서 토큰을 꺼냅니다.
        String token = resolveToken(request);

        // 2. 토큰이 존재하고, 위조/만료되지 않은 진짜 토큰이라면?
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

            // 3. 토큰을 찢어서 안에 있는 유저 ID(PK)를 꺼냅니다.
            Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

            // ⭐️ 추가: 토큰을 한 번 더 찢어서, 그 안에 든 진짜 권한(Role)을 꺼냅니다!
            String role = jwtTokenProvider.getRoleFromToken(token);

            // 4. 스프링 시큐리티가 읽을 수 있는 '임시 신분증(UserDetails)'을 만듭니다.
            UserDetails userDetails = User.builder()
                    .username(memberId.toString())
                    .password("")
                    .authorities(role) // ⭐️ 하드코딩 탈출! 토큰에 적힌 진짜 권한(USER or ADMIN)을 부여함
                    .build();

            // 5. 완벽한 '출입증(Authentication)' 객체로 승급시킵니다.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 6. 누가, 어디서(IP 등) 접속했는지 디테일한 웹 요청 정보를 추가로 달아줍니다.
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 7. ⭐️ 대망의 하이라이트: 경찰서 명부(SecurityContextHolder)에 이름 올리기!
            // 이제부터 AuditorAware가 이 명부를 보고 created_by를 채울 수 있습니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 8. 내 할 일(검사) 끝났으니 다음 필터(혹은 컨트롤러)로 넘어가라고 문을 열어줍니다.
        filterChain.doFilter(request, response);
    }

    // ==========================================================
    // 🛠️ 헬퍼 메서드: 헤더에서 "Bearer " 글자를 떼어내고 순수 토큰만 추출
    // ==========================================================
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        // 프론트엔드가 표준 규칙(Bearer + 공백 + 토큰)을 지켜서 보냈는지 확인
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}