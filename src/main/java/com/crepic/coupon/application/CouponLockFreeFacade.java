package com.crepic.coupon.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponLockFreeFacade {

    // ⭐️ 무적의 StringRedisTemplate 적용 완료!
    private final StringRedisTemplate redisTemplate;

    public void issueCouponLockFree(Long memberId, Long couponId) {
        // 🚨 [핵심 수정] 테스트 코드와 Key 이름을 똑같이 맞춤! (coupon:1:count)
        String countKey = "coupon:" + couponId + ":count";
        String queueKey = "coupon:queue:" + couponId;

        // 1. [초고속 차감] Redis 원자적 연산 (락 없이 수량 1 감소)
        Long remainCount = redisTemplate.opsForValue().decrement(countKey);

        // ⭐️ [강제 재컴파일 + 로그 확인용] 숫자가 깎이는지 눈으로 보자!
        System.out.println("🔥 [유저 " + memberId + "] 현재 남은 쿠폰 수량: " + remainCount);

        // 2. [빠른 실패] 수량이 0 미만이면 즉시 예외 발생! (DB 안 가고 튕겨냄)
        if (remainCount != null && remainCount < 0) {
            throw new RuntimeException("쿠폰이 모두 소진되었습니다.");
        }

        // 3. [메시지 큐] 통과한 유저는 Redis List(대기열 바구니)에 밀어 넣음
        redisTemplate.opsForList().rightPush(queueKey, String.valueOf(memberId));
    }

    // [관리자용] 테스트 시작 전, Redis에 쿠폰 수량을 100개로 충전해 두는 셋업 메서드
    public void initCoupon(Long couponId, int totalQuantity) {
        // 🚨 여기도 Key 이름을 똑같이 맞춤!
        String countKey = "coupon:" + couponId + ":count";
        String queueKey = "coupon:queue:" + couponId;

        // 1. 기존 데이터 초기화 (혹시 남아있는 쓰레기값 삭제)
        redisTemplate.delete(countKey);
        redisTemplate.delete(queueKey);

        // 2. Redis에 초기 쿠폰 수량 세팅
        redisTemplate.opsForValue().set(countKey, String.valueOf(totalQuantity));
    }
}