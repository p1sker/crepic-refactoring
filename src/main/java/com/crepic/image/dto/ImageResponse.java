package com.crepic.image.dto;

import com.crepic.image.domain.Image;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "이미지 정보 응답 객체 (갤러리 및 목록용)")
public record ImageResponse(
        @Schema(description = "이미지 고유 ID", example = "1")
        Long id,

        @Schema(description = "이미지 제목", example = "서울의 푸른 밤")
        String title,

        @Schema(description = "작가(판매자) 닉네임", example = "p1sker")
        String sellerNickname,

        @Schema(description = "카테고리명", example = "도시/야경")
        String categoryName,

        @Schema(description = "메인 노출용 이미지 URL (워터마크 적용본)", example = "https://cdn.crepic.com/wm/img_01.jpg")
        String imageUrl,

        @Schema(description = "판매 가격", example = "50000")
        Integer price,

        @Schema(description = "조회수", example = "128")
        Long viewCount,

        @Schema(description = "판매 상태", example = "ON_SALE")
        String status,

        @Schema(description = "등록 일시")
        LocalDateTime createdAt
) {
    /**
     * 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static ImageResponse from(Image image) {
        return new ImageResponse(
                image.getId(),
                image.getTitle(),
                image.getSeller().getNickname(), // ⭐️ seller_id 연관관계 (Member)
                image.getCategory().getName(),   // ⭐️ category_id 연관관계 (Category)
                image.getWatermarkUrl(),         // 🛡️ 보안: watermarkUrl 노출
                image.getPrice(),
                image.getViewCount(),
                image.getStatus().name(),        // ✅ [해결] Enum을 String으로 명시적 변환 (.name())
                image.getCreatedAt()             // BaseEntity에서 상속받은 날짜
        );
    }
}