package com.crepic.coupon.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    // ⭐️ Phase 3: DB 락(PESSIMISTIC_WRITE) 제거!
    // JpaRepository가 기본 제공하는 findById()를 그대로 사용합니다.
}