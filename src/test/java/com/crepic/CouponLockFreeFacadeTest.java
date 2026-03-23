package com.crepic;

import com.crepic.coupon.application.CouponLockFreeFacade;
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

@SpringBootTest(properties = "spring.kafka.consumer.auto-offset-reset=earliest")
@Import(RedisTestContainerConfig.class)
class CouponLockFreeFacadeTest {

    @Autowired
    private CouponLockFreeFacade couponLockFreeFacade;

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
                .name("Kafka 선착순 쿠폰")
                .discountAmount(1000)
                .totalQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .build();

        Coupon savedCoupon = couponRepository.saveAndFlush(testCoupon);
        this.targetCouponId = savedCoupon.getId();

        // 3. Facade의 initCoupon을 사용하여 Redis 상태 초기화 (동적 ID 사용)
        // 🚨 주의: 이제 이 메서드는 Redis 큐를 지우지 않고 '수량(count)'만 100개로 셋팅합니다.
        couponLockFreeFacade.initCoupon(targetCouponId, 100);
    }

    @Test
    @DisplayName("1000명이 동시에 요청하면, Redis에서 100명만 통과시켜 카프카로 보내고 DB에 100건이 저장된다.")
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
                        // Redis 루아 스크립트로 재고 차감 -> 성공 시 Kafka 메시지 발행
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
        // 🚀 [핵심] 일개미를 수동으로 부르지 않습니다!
        // 카프카 컨슈머(@KafkaListener)가 백그라운드에서 메시지를 읽고 DB에 넣을 시간을 줍니다.
        System.out.println("⏳ 카프카가 100개의 메시지를 비동기로 처리할 때까지 5초 대기합니다...");
        Thread.sleep(10000);

        // 3. 최종 DB 데이터 확인
        long finalCount = memberCouponRepository.count();
        System.out.println("✅ 최종 DB에 저장된 발급 내역 수: " + finalCount);

        assertThat(finalCount).isEqualTo(100);

        System.out.println("🎉 [Phase 5] Apache Kafka 비동기 이벤트 드리븐 통합 테스트 100% 성공!");
    }
}