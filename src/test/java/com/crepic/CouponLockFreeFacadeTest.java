package com.crepic;

import com.crepic.coupon.application.CouponLockFreeFacade;
import com.crepic.coupon.application.CouponIssueWorker;
import com.crepic.coupon.domain.Coupon;
import com.crepic.coupon.domain.CouponRepository;
import com.crepic.coupon.domain.MemberCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(RedisTestContainerConfig.class)
class CouponLockFreeFacadeTest {

    @Autowired
    private CouponLockFreeFacade couponLockFreeFacade;

    @Autowired
    private CouponIssueWorker couponIssueWorker;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private CouponRepository couponRepository;

    private Long targetCouponId;

    @BeforeEach
    void setUp() {
        // 1. 기존 데이터 초기화 (외래키 제약조건 고려하여 순서대로 삭제)
        memberCouponRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();

        // 2. 테스트용 쿠폰 생성
        Coupon testCoupon = Coupon.builder()
                .name("Testcontainers 선착순 쿠폰")
                .discountAmount(1000)
                .totalQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .build();

        Coupon savedCoupon = couponRepository.saveAndFlush(testCoupon);
        this.targetCouponId = savedCoupon.getId();

        // 3. Facade의 initCoupon을 사용하여 Redis 상태 초기화 (동적 ID 사용)
        couponLockFreeFacade.initCoupon(targetCouponId, 100);
    }

    @Test
    @DisplayName("1000명이 동시에 쿠폰 발급을 요청하면, 100명만 Redis 큐에 들어가고 나머지는 튕겨야 한다.")
    void issueCoupon_Concurrent_1000_Requests() throws InterruptedException {
        // given
        int threadCount = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        try (ExecutorService executorService = Executors.newFixedThreadPool(32)) {
            // when
            for (int i = 0; i < threadCount; i++) {
                long memberId = i + 1;
                executorService.submit(() -> {
                    try {
                        // 생성된 진짜 ID로 요청 폭격!
                        couponLockFreeFacade.issueCouponLockFree(memberId, targetCouponId);
                    } catch (Exception e) {
                        if (e.getMessage() == null || !e.getMessage().contains("소진")) {
                            if (errorCount.incrementAndGet() <= 5) {
                                System.out.println("❌ 찐 에러 발생!: " + e.getMessage());
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        // then
        // 1. Redis 큐에 정확히 100명이 있는지 확인
        String queueKey = "coupon:queue:" + targetCouponId;
        Long queueSizeBefore = redisTemplate.opsForList().size(queueKey);
        System.out.println("🔥 일개미 투입 전 Redis 큐 대기자 수 (기대값 100): " + queueSizeBefore);
        assertThat(queueSizeBefore).isEqualTo(100);

        // 2. 일개미(Worker)에게 진짜 ID를 주며 수동 호출
        System.out.println("👷 일개미(Worker) 수동 호출 시작... 쿠폰 ID: " + targetCouponId);
        couponIssueWorker.processQueue(targetCouponId);
        System.out.println("👷 일개미(Worker) 작업 완료!");

        // 3. 최종 DB 데이터 확인
        long finalCount = memberCouponRepository.count();
        System.out.println("✅ 최종 DB에 저장된 발급 내역 수: " + finalCount);
        assertThat(finalCount).isEqualTo(100);

        System.out.println("🎉 [Phase 4] 동시성 방어 & Testcontainers 연동 성공!");
    }
}