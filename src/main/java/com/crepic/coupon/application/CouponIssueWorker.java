package com.crepic.coupon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueWorker {

    private final StringRedisTemplate redisTemplate;
    private final CouponService couponService;

    /**
     * 기본 스케줄러 (운영용): 1번 쿠폰을 주기적으로 체크
     */
    @Scheduled(fixedDelay = 1000)
    public void processDefaultQueue() {
        processQueue(1L);
    }

    /**
     * ⭐️ 핵심 수정: 특정 쿠폰 ID의 대기열을 처리하는 공용 메서드
     * 테스트 코드에서 직접 호출할 때 사용!
     */
    public void processQueue(Long couponId) {
        String queueKey = "coupon:queue:" + couponId;

        while (true) {
            String memberIdStr = redisTemplate.opsForList().leftPop(queueKey);

            if (memberIdStr == null) {
                break;
            }

            try {
                Long memberId = Long.parseLong(memberIdStr);
                // 🚨 이제 1L 고정이 아니라 파라미터로 받은 couponId를 사용!
                couponService.issueCoupon(memberId, couponId);

            } catch (Exception e) {
                log.error("[일개미 에러] DB 저장 실패 (memberId: {}, couponId: {}): {}", memberIdStr, couponId, e.getMessage());
            }
        }
    }
}