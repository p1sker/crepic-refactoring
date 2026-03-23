package com.crepic.coupon.presentation;

import com.crepic.coupon.application.CouponLockFreeFacade;
import com.crepic.coupon.domain.Coupon;
import com.crepic.coupon.domain.CouponRepository;
import com.crepic.coupon.domain.MemberCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate; // ⭐️ 추가됨!
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@RestController
@RequiredArgsConstructor
public class TestResetController {

    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final CouponLockFreeFacade couponLockFreeFacade;
    private final StringRedisTemplate redisTemplate; // ⭐️ Redis 전체 청소를 위해 추가

    @GetMapping("/test/reset")
    @Transactional
    public String resetForJmeter() {
        // 1. 기존 데이터 싹 다 날리기 (외래키 꼬임 및 찌꺼기 방지)
        memberCouponRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();

        // 2. ⭐️ Redis에 있는 모든 쿠폰 관련 키(재고, 유저명단) 싹 날리기!
        Set<String> keys = redisTemplate.keys("coupon:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 3. 완벽하게 깨끗한 새 쿠폰 뚝딱 만들기
        Coupon coupon = Coupon.builder()
                .name("1인 1매 찐막 부하테스트 쿠폰")
                .discountAmount(1000)
                .totalQuantity(100)
                .validFrom(java.time.LocalDateTime.now().minusDays(1))
                .validUntil(java.time.LocalDateTime.now().plusDays(1))
                .build();

        Coupon savedCoupon = couponRepository.saveAndFlush(coupon);

        // 4. Redis에 100개 빵빵하게 충전
        couponLockFreeFacade.initCoupon(savedCoupon.getId(), 100);

        return "✅ [1인 1매 정책 완료] JMeter 찐막 테스트 준비 완료! (새 쿠폰 ID: " + savedCoupon.getId() + "번)";
    }
}