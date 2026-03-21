package com.crepic.coupon.presentation;

import com.crepic.coupon.application.CouponLockFreeFacade; // ⭐️ 임포트 변경!
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Coupon", description = "쿠폰 API (Lock-Free + 비동기 테스트용)")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    // ⭐️ 기존 Redisson 대신, 새로 만든 LockFreeFacade를 주입받아!
    private final CouponLockFreeFacade couponLockFreeFacade;

    @Operation(summary = "선착순 쿠폰 발급 (Lock-Free)", description = "DB 대기 없이 Redis로만 즉시 응답합니다.")
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<String> issueCoupon(
            @PathVariable Long couponId,
            @RequestParam Long memberId
    ) {
        // ⭐️ 새로운 엔진 호출! (DB 저장 안 하고 큐에만 넣음)
        couponLockFreeFacade.issueCouponLockFree(memberId, couponId);

        // ⭐️ 바로 클라이언트에게 성공했다고 응답해버림 (핵심 포인트)
        return ResponseEntity.ok("쿠폰 발급 요청이 접수되었습니다. (비동기 처리중)");
    }
    // ... 기존 issueCoupon 메서드 아래에 추가!

    @Operation(summary = "쿠폰 수량 초기화 (Redis 셋업)", description = "테스트 전 수량을 100개로 충전합니다.")
    @PostMapping("/{couponId}/init")
    public ResponseEntity<String> initCoupon(
            @PathVariable Long couponId,
            @RequestParam(defaultValue = "100") int totalQuantity // 기본값 100
    ) {
        couponLockFreeFacade.initCoupon(couponId, totalQuantity);
        return ResponseEntity.ok("Redis에 쿠폰 " + totalQuantity + "개 충전 완료!");
    }
}