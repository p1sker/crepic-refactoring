package com.crepic.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
// ⭐️ 핵심: "스프링아, JPA 자동 감시 모드 켜고, 도장 찍을 때는 'auditorProvider' 기계를 써라!"
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {
    // 내용은 텅 비어있어도 됩니다. 이 어노테이션 두 개가 전부 다 알아서 합니다!
}