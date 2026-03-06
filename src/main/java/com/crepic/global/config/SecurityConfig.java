package com.crepic.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.crepic.global.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity // ⭐️ 스프링 시큐리티의 제어권을 직접 갖겠다는 선언
@RequiredArgsConstructor
public class SecurityConfig {

    // 서장님(Config)이 경찰(Filter)을 전속으로 고용하는 코드입니다.
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 1. ⚔️ 비밀번호 암호화 빈 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. 🛡️ 시큐리티 필터 체인 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // [REST API 필수 설정]
                .formLogin(AbstractHttpConfigurer::disable) // 폼 기반 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 비활성화
                .csrf(AbstractHttpConfigurer::disable)      // CSRF 보안 비활성화 (JWT 사용 예정)

                // ⭐️ 세션 정책: STATELESS (서버가 세션을 만들지 않음)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 🚪 출입 통제 구역 설정
                .authorizeHttpRequests(auth -> auth
                        // ⭐️ 핵심 수정: 회원가입 POST 요청 허용
                        // Controller의 주소와 정확히 일치시켜야 합니다.
                        .requestMatchers(HttpMethod.POST, "/api/members").permitAll()

                        // 향후 로그인 API 주소 (미리 허용)
                        .requestMatchers(HttpMethod.POST, "/api/members/login").permitAll()

                        // ⭐️ Swagger 문서 주소 허용 (문서가 안 보이면 안 되니까요!)
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // 그 외 나머지 요청은 인증(로그인)이 필요함
                        .anyRequest().authenticated()
                )
                // 기존 보안 필터가 작동하기 전에, 우리 JWT 경찰부터 무조건 먼저 거치게 만듦!
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}