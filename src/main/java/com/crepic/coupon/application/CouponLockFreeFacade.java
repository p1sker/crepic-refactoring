package com.crepic.coupon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponLockFreeFacade {

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC_NAME = "coupon-issue-topic";

    // 🚀 [핵심 변경] 1인 1매 검증 + 재고 차감을 동시에 하는 루아 스크립트!
    private static final String SCRIPT =
            "local countKey = KEYS[1] " +
                    "local userKey = KEYS[2] " +   // ⭐️ 발급받은 유저 명단(Set) Key
                    "local memberId = ARGV[1] " +  // ⭐️ 요청한 유저 ID

                    // 1. 이미 받은 유저인지 확인 (Set에 존재하면 1 반환)
                    "if redis.call('sismember', userKey, memberId) == 1 then " +
                    "  return -1 " + // 🚨 이미 발급받은 유저면 -1 반환
                    "end " +

                    // 2. 재고 확인 및 차감
                    "local count = redis.call('get', countKey) " +
                    "if count ~= nil and tonumber(count) > 0 then " +
                    "  redis.call('decr', countKey) " +          // 재고 차감
                    "  redis.call('sadd', userKey, memberId) " + // 발급 명단에 유저 추가!
                    "  return 1 " + // 발급 성공
                    "else " +
                    "  return 0 " + // 재고 소진
                    "end";

    public void issueCouponLockFree(Long memberId, Long couponId) {
        String countKey = "coupon:" + couponId + ":count";
        String userKey = "coupon:" + couponId + ":users"; // ⭐️ 유저 명단 Key 생성

        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(SCRIPT, Long.class),
                List.of(countKey, userKey), // KEYS[1], KEYS[2]
                String.valueOf(memberId)    // ARGV[1]
        );

        // 🚨 결과에 따른 예외 처리
        if (result != null && result == -1) {
            throw new RuntimeException("이미 쿠폰을 발급받으셨습니다."); // 중복 요청 컷!
        }
        if (result == null || result == 0) {
            throw new RuntimeException("쿠폰이 모두 소진되었습니다.");
        }

        // 성공 시 카프카로 쏜다!
        String message = memberId + ":" + couponId;
        kafkaTemplate.send(TOPIC_NAME, message);

        log.info("🔥 [카프카 발행 성공] 유저 {} 님이 쿠폰 {} 당첨!", memberId, couponId);
    }

    public void initCoupon(Long couponId, int totalQuantity) {
        String countKey = "coupon:" + couponId + ":count";
        String userKey = "coupon:" + couponId + ":users";

        redisTemplate.delete(countKey);
        redisTemplate.delete(userKey); // ⭐️ 리셋할 때 유저 명단도 싹 지워주기!
        redisTemplate.opsForValue().set(countKey, String.valueOf(totalQuantity));
    }
}