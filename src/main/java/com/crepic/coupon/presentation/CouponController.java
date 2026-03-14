package com.crepic.coupon.presentation;

import com.crepic.coupon.application.CouponService;
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

    private final CouponService couponService;

    @Operation(summary = "선착순 쿠폰 발급", description = "1,000명 동시성 테스트를 위한 API입니다.")
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<String> issueCoupon(
            @PathVariable Long couponId,
            // 🚨 [S급 실무 꿀팁] 원래는 JWT 토큰에서 회원 ID를 꺼내야 하지만,
            // JMeter로 1,000명의 서로 다른 가짜 회원을 쏘기 편하게 만들기 위해
            // 임시로 memberId를 파라미터로 직접 받도록 열어둡니다!
            @RequestParam Long memberId
    ) {
        couponService.issueCoupon(memberId, couponId);
        return ResponseEntity.ok("쿠폰 발급 성공!");
    }
}