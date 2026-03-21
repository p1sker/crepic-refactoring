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
import org.springframework.data.redis.core.StringRedisTemplate; // ⭐️ 요걸로 임포트!

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponLockFreeFacadeTest {

    @Autowired
    private CouponLockFreeFacade couponLockFreeFacade;

    @Autowired
    private CouponIssueWorker couponIssueWorker;

    // ⭐️ 무조건 StringRedisTemplate 사용!
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        // 1. 기존 데이터 싹 비우기
        memberCouponRepository.deleteAll();

        // 2. Redis 찌꺼기 비우고 "100" 세팅 (StringRedisTemplate이므로 예쁘게 들어감)
        redisTemplate.delete("coupon:1:count");
        redisTemplate.delete("coupon:queue:1");
        redisTemplate.opsForValue().set("coupon:1:count", "100");

        // 3. 쿠폰 마스터 생성
        if (!couponRepository.existsById(1L)) {
            Coupon dummyCoupon = Coupon.builder()
                    .name("테스트 선착순 쿠폰")
                    .discountAmount(1000)
                    .totalQuantity(100)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(1))
                    .build();
            couponRepository.save(dummyCoupon);
        }
    }

    @Test
    @DisplayName("1000명이 동시에 쿠폰 발급을 요청하면, 100명만 Redis 큐에 들어가고 나머지는 튕겨야 한다.")
    void issueCoupon_Concurrent_1000_Requests() throws InterruptedException {
        int threadCount = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 에러 원인 파악을 위한 카운터
        AtomicInteger errorCount = new AtomicInteger(0);

        try (ExecutorService executorService = Executors.newFixedThreadPool(32)) {
            for (int i = 0; i < threadCount; i++) {
                long memberId = i + 1;
                executorService.submit(() -> {
                    try {
                        // 🚨 Facade 내부의 키 이름이 "coupon:1:count" 와 "coupon:queue:1" 이 맞는지 꼭 확인해!
                        couponLockFreeFacade.issueCouponLockFree(memberId, 1L);
                    } catch (Exception e) {
                        // "소진" 에러가 아닌 진짜 시스템 에러만 5개까지 출력해봄
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

        // 중간 점검
        Long queueSizeBefore = redisTemplate.opsForList().size("coupon:queue:1");
        System.out.println("🔥 일개미 투입 전 Redis 큐 대기자 수 (기대값 100): " + queueSizeBefore);

        // 일개미 출동
        System.out.println("👷 일개미(Worker) 수동 호출 시작...");
        couponIssueWorker.processQueue(); // 네 메서드 이름으로 호출
        System.out.println("👷 일개미(Worker) 작업 완료!");

        // 결과 검증
        long finalCount = memberCouponRepository.count();
        System.out.println("✅ 최종 DB에 저장된 발급 내역 수: " + finalCount);
        assertThat(finalCount).isEqualTo(100);

        System.out.println("🎉 테스트 완벽 통과! 동시성 방어 100% 성공!");
    }
}