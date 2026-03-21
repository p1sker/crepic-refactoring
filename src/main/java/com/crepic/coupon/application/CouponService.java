package com.crepic.coupon.application;

import com.crepic.coupon.domain.Coupon;
import com.crepic.coupon.domain.CouponRepository;
import com.crepic.coupon.domain.MemberCoupon;
import com.crepic.coupon.domain.MemberCouponRepository;
import com.crepic.member.domain.Member;
import com.crepic.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;


    /**
     * [순수 비즈니스 로직]
     * 락(Lock) 관리는 Facade(Redis)에게 위임하고, 여기서는 순수하게 발급 로직만 처리합니다.
     */
    @Transactional
    public void issueCoupon(Long memberId, Long couponId) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2. 쿠폰 조회 (⭐️ 비관적 락을 쓰던 findByIdWithLock 대신 기본 findById 사용)
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 3. 중복 발급 방지
        if (memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        // 4. 쿠폰 발급 (Redis가 1명씩 통과시켜 주므로 여기서 동시성 이슈 안 터짐)
        coupon.issue();

        // 5. 회원의 쿠폰함에 저장
        memberCouponRepository.save(new MemberCoupon(member, coupon));

        log.info("쿠폰 발급 완료 - memberId: {}, 현재 수량: {}/{}",
                memberId, coupon.getIssuedQuantity(), coupon.getTotalQuantity());
    }
    //
}