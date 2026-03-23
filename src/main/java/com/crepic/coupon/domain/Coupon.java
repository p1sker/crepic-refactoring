package com.crepic.coupon.domain;

import com.crepic.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
@SQLRestriction("deleted_at IS NULL")
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "issued_quantity")
    private Integer issuedQuantity = 0;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Coupon(String name, Integer discountAmount, Integer totalQuantity,
                   LocalDateTime validFrom, LocalDateTime validUntil) {
        this.name = name;
        this.discountAmount = discountAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    // ==========================================
    // 💣 [의도된 폭탄 로직] 1차원적인 쿠폰 발급
    // 1명씩 들어오면 문제없지만, 1,000명이 동시에 접근하면?
    // 1,000명이 모두 issuedQuantity가 99인 것을 확인하고 동시에 +1을 해버립니다.
    // ==========================================
    public void issue() {
        if (this.totalQuantity != null && this.issuedQuantity >= this.totalQuantity) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }
        if (LocalDateTime.now().isBefore(validFrom) || LocalDateTime.now().isAfter(validUntil)) {
            throw new IllegalStateException("쿠폰 발급 기간이 아닙니다.");
        }

        this.issuedQuantity++; // 💥 여기가 바로 JMeter에 의해 터져나갈 병목 지점입니다!
    }

    public void resetIssuedQuantity() {
        this.issuedQuantity = 0;
    }
}