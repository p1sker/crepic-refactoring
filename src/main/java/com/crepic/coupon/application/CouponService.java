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
     * [비관적 락 적용 버전]
     * 데이터베이스 레벨에서 Row에 Lock을 걸어 정합성을 보장합니다.
     */
    @Transactional // ⭐️ 락은 트랜잭션이 유지되는 동안 유효하므로 필수입니다!
    public void issueCoupon(Long memberId, Long couponId) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2. 쿠폰 조회 (비관적 락 사용 ⭐️)
        // 다른 유저들이 동시에 접근해도 여기서 "줄을 서서" 대기하게 됩니다.
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 3. 중복 발급 방지 (락이 걸려있으므로 이제 안전하게 체크됩니다)
        if (memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        // 4. 쿠폰 발급 (Entity 내부의 폭탄 로직 호출)
        // 락 덕분에 이제 더 이상 폭탄이 아니라 "순차 처리"가 됩니다.
        coupon.issue();

        // 5. 회원의 쿠폰함에 저장
        memberCouponRepository.save(new MemberCoupon(member, coupon));

        log.info("쿠폰 발급 완료 - memberId: {}, 현재 수량: {}/{}",
                memberId, coupon.getIssuedQuantity(), coupon.getTotalQuantity());
    }
}