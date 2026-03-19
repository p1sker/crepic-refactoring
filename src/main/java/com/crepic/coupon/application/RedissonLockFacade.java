package com.crepic.coupon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockFacade {

    private final RedissonClient redissonClient;
    private final CouponService couponService;

    /**
     * [분산 락 파사드]
     * Redis 락을 획득한 유저만 실제 비즈니스 로직(CouponService)으로 입장시킵니다.
     */
    public void issueCouponWithDistributedLock(Long memberId, Long couponId) {
        // 1. 쿠폰 ID를 기반으로 락의 고유 이름(Key) 생성
        String lockKey = "coupon_lock:" + couponId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. 락 획득 시도 (10초 동안 락 획득 대기, 1초 동안 락 점유)
            // Redisson의 장점: Pub/Sub 방식으로 락을 대기하므로 Redis 부하가 거의 없음
            boolean isLocked = lock.tryLock(10, 1, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("락 획득 실패 - 쿠폰 발급 트래픽 초과. memberId: {}", memberId);
                throw new IllegalStateException("현재 접속자가 많아 처리가 지연되고 있습니다. 다시 시도해주세요.");
            }

            // 3. 락을 획득한 스레드만 실제 트랜잭션(비즈니스 로직) 시작!
            couponService.issueCoupon(memberId, couponId);

        } catch (InterruptedException e) {
            log.error("락 대기 중 인터럽트 발생", e);
            Thread.currentThread().interrupt(); // 스레드 인터럽트 상태 복구
            throw new RuntimeException("시스템 에러가 발생했습니다.");
        } finally {
            // 4. 로직이 끝나면 (또는 예외가 터져도) 반드시 락을 해제해서 다음 사람을 들여보냄
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}