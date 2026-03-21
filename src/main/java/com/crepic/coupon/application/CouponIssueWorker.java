package com.crepic.coupon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueWorker {

    // 기존: private final RedisTemplate<String, String> redisTemplate;
    private final StringRedisTemplate redisTemplate; // ⭐️ 여기도 똑같이 변경!

    // ⭐️ 예전에 만들어둔 '진짜 DB 저장 로직'을 여기서 불러옴!
    private final CouponService couponService;

    // 1초(1000ms)마다 백그라운드에서 몰래 깨어나서 바구니를 확인하는 스케줄러
    @Scheduled(fixedDelay = 1000)
    public void processQueue() {
        String queueKey = "coupon:queue:1"; // 우리가 테스트 중인 1번 쿠폰 큐

        while (true) {
            // 1. 바구니(Redis List)에서 왼쪽부터 하나씩 꺼내기 (leftPop)
            String memberIdStr = redisTemplate.opsForList().leftPop(queueKey);

            // 2. 바구니가 비어있으면? 할 일 끝! 루프 탈출해서 다시 1초 취침.
            if (memberIdStr == null) {
                break;
            }

            try {
                Long memberId = Long.parseLong(memberIdStr);

                // 3. 꺼낸 유저 ID로 드디어 느려터진 DB 저장 로직 실행!
                // 유저는 이미 "발급 성공" 화면을 보고 떠났기 때문에, 여기서 DB가 1초든 10초든 걸려도 상관없음.
                couponService.issueCoupon(memberId, 1L);

            } catch (Exception e) {
                // 에러가 나면 서버가 뻗지 않게 로그만 남기고 다음 사람 처리 (실무에선 실패 큐로 보냄)
                log.error("[일개미 에러] DB 저장 실패 (memberId: {}): {}", memberIdStr, e.getMessage());
            }
        }
    }
}