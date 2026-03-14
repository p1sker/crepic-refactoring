package com.crepic.coupon.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    // 회원이 해당 쿠폰을 이미 발급받았는지 확인하는 쿼리 메서드
    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);
}