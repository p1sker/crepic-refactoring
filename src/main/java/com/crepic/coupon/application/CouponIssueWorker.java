package com.crepic.coupon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueWorker {

    private final CouponService couponService;

    // 🚀 [핵심 변경] 1초마다 깨서 확인하던 스케줄러 삭제!
    // 이제 카프카에 메시지가 들어오는 순간 이 메서드가 '자동으로' 실행된다!
    @KafkaListener(topics = "coupon-issue-topic", groupId = "crepic-group")
    public void processKafkaMessage(String message) {
        // log.info("📩 [카프카 수신] 메시지를 받았습니다: {}", message); // 너무 많이 찍히면 시끄러우니까 주석 처리해도 됨

        try {
            // 파사드에서 쏜 포맷: "memberId:couponId" (예: "5:1")
            String[] parts = message.split(":");
            Long memberId = Long.parseLong(parts[0]);
            Long couponId = Long.parseLong(parts[1]);

            // 드디어 DB 저장 로직 실행!
            couponService.issueCoupon(memberId, couponId);

        } catch (Exception e) {
            log.error("❌ [일개미 에러] DB 저장 실패 (message: {}): {}", message, e.getMessage());
            // 실무 꿀팁: 여기서 에러가 나면 Dead Letter Queue(DLQ)라는 '실패 보관함'으로 메시지를 다시 던짐!
        }
    }
}