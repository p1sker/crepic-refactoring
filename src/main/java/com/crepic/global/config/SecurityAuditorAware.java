package com.crepic.global.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorProvider")
public class SecurityAuditorAware implements AuditorAware<Long> {

    private final SecurityContextHolderStrategy strategy = SecurityContextHolder.getContextHolderStrategy();

    @Override
    @NonNull
    @SuppressWarnings("deprecation") // ⭐️ "이 경고는 프레임워크 이슈니 무시해라" 라고 컴파일러에게 명령!
    public Optional<Long> getCurrentAuditor() {
        return Optional.ofNullable(strategy.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth.isAuthenticated() && !isAnonymous(auth))
                .map(auth -> {
                    try {
                        return Long.valueOf(auth.getName());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                });
    }

    private boolean isAnonymous(Authentication auth) {
        return auth.getPrincipal() instanceof String principal &&
                "anonymousUser".equals(principal);
    }
}