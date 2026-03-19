package com.crepic.coupon.presentation;

import com.crepic.coupon.application.RedissonLockFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Coupon", description = "쿠폰 API (동시성 부하 테스트용)")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    // ⭐️ 기존 CouponService 대신 파사드(RedissonLockFacade)를 주입받습니다.
    private final RedissonLockFacade redissonLockFacade;

    @Operation(summary = "선착순 쿠폰 발급", description = "1,000명 동시성 테스트를 위한 API입니다.")
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<String> issueCoupon(
            @PathVariable Long couponId,
            @RequestParam Long memberId
    ) {
        // ⭐️ 서비스 대신 분산 락 파사드를 호출합니다!
        redissonLockFacade.issueCouponWithDistributedLock(memberId, couponId);
        return ResponseEntity.ok("쿠폰 발급 완료 여부 확인됨");
    }
}