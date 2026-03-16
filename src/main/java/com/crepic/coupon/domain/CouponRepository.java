package com.crepic.coupon.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    // ⭐️ 핵심: 비관적 쓰기 락 (PESSIMISTIC_WRITE)
    // 이 메서드가 호출되면 해당 쿠폰 행(Row)은 트랜잭션이 끝날 때까지 다른 놈들이 못 건드립니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithLock(@Param("id") Long id);
}