package com.crepic.coupon.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponLockFreeFacade {

    private final StringRedisTemplate redisTemplate;

    public void issueCouponLockFree(Long memberId, Long couponId) {
        String countKey = "coupon:" + couponId + ":count";
        String queueKey = "coupon:queue:" + couponId;

        // 📜 Redis Lua Script: "수량을 확인하고, 0보다 크면 깎고 큐에 넣어라! 이걸 한 번에(Atomic)!"
        String script =
                "local count = redis.call('get', KEYS[1]) " +
                        "if count ~= nil and tonumber(count) > 0 then " +
                        "  redis.call('decr', KEYS[1]) " +
                        "  redis.call('rpush', KEYS[2], ARGV[1]) " +
                        "  return 1 " + // 성공 시 1 반환
                        "else " +
                        "  return 0 " + // 실패(재고 없음) 시 0 반환
                        "end";

        // 스크립트 실행 (레디스 서버 내부에서 한 방에 실행됨)
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                List.of(countKey, queueKey), // KEYS[1], KEYS[2]
                String.valueOf(memberId)      // ARGV[1]
        );

        if (result == null || result == 0) {
            throw new RuntimeException("쿠폰이 모두 소진되었습니다.");
        }

        System.out.println("🔥 [유저 " + memberId + "] 루아 스크립트로 안전하게 발급 성공!");
    }

    public void initCoupon(Long couponId, int totalQuantity) {
        redisTemplate.delete("coupon:" + couponId + ":count");
        redisTemplate.delete("coupon:queue:" + couponId);
        redisTemplate.opsForValue().set("coupon:" + couponId + ":count", String.valueOf(totalQuantity));
    }
}