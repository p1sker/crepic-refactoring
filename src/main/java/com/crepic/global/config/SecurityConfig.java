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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ⭐️ 세션 정책: STATELESS (서버가 세션을 만들지 않음)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 🚪 출입 통제 구역 설정
                .authorizeHttpRequests(auth -> auth
                        // ⭐️ 핵심 수정: 회원가입 POST 요청 허용
                        .requestMatchers(HttpMethod.POST, "/api/members").permitAll()

                        // 향후 로그인 API 주소 (미리 허용)
                        .requestMatchers(HttpMethod.POST, "/api/members/login", "/api/members/reissue").permitAll()

                        // 💣 [S급 꼼수] JMeter 동시성 부하 테스트를 위해 쿠폰 API 일시적 전면 개방!
                        .requestMatchers(HttpMethod.POST, "/api/coupons/**").permitAll()

                        // 🚀 [추가!!!] JMeter 부하 테스트 리셋용 API 개방! (테스트 끝나면 꼭 지우기!)
                        .requestMatchers(HttpMethod.GET, "/test/reset").permitAll()

                        // ⭐️ Swagger 문서 주소 허용
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ⭐️ 리액트가 켜질 주소들 (실무에서는 나중에 실제 도메인 주소도 여기에 추가합니다)
        // 리액트 기본 포트(3000)와 요즘 유행하는 Vite 기본 포트(5173)를 둘 다 열어둡니다.
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 허용할 헤더 (Authorization 헤더 등을 모두 허용)
        configuration.setAllowedHeaders(List.of("*"));

        // 내 서버가 응답할 때 프론트엔드에서 쿠키나 인증 헤더를 읽을 수 있게 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 API 주소("/**")에 이 설정을 적용!
        return source;
    }

}