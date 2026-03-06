package com.crepic.image.domain;

import com.crepic.global.entity.BaseEntity;
import com.crepic.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "images",
        indexes = {
                // ⭐️ S+++급 실무 포인트: FK에 명시적 인덱스를 걸어 Join 성능을 극대화한 아주 좋은 설계입니다!
                @Index(name = "idx_image_seller", columnList = "seller_id"),
                @Index(name = "idx_image_category", columnList = "category_id"),
                @Index(name = "idx_image_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// ⭐️ @DynamicUpdate 대활약 포인트: 컬럼이 15개로 많고, description(TEXT)같이 무거운 데이터가 있습니다.
// viewCount나 status만 단건으로 업데이트될 일이 많으므로 여기서는 이 어노테이션이 제 몫을 톡톡히 합니다!
@DynamicUpdate
// 🚨 @SQLDelete 제거: 이전 Category와 동일하게, 삭제 행위를 '도메인 로직(delete)'으로 끌어안기 위해 제거합니다.
@SQLRestriction("deleted_at IS NULL")
@ToString(of = {"id", "title", "price", "status"})
public class Image extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_image_seller"))
    private Member seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_image_category"))
    private Category category;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "original_url", nullable = false, length = 500)
    private String originalUrl;

    @Column(name = "watermark_url", nullable = false, length = 500)
    private String watermarkUrl;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ImageStatus status = ImageStatus.ON_SALE;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Image(Member seller, Category category, String title, String description,
                  Integer price, String originalUrl, String watermarkUrl) {

        validateImage(seller, category, title, originalUrl, watermarkUrl, price);

        this.seller = seller;
        this.category = category;
        this.title = title;
        this.description = description;
        this.price = price != null ? price : 0;
        this.originalUrl = originalUrl;
        this.watermarkUrl = watermarkUrl;
        this.status = ImageStatus.ON_SALE;
        this.viewCount = 0L;
    }

    public static Image create(Member seller, Category category, String title, String description,
                               Integer price, String originalUrl, String watermarkUrl) {
        return Image.builder()
                .seller(seller)
                .category(category)
                .title(title)
                .description(description)
                .price(price)
                .originalUrl(originalUrl)
                .watermarkUrl(watermarkUrl)
                .build();
    }

    // ==========================================
    // 💡 핵심 비즈니스 로직 (Rich Domain Model)
    // ==========================================

    /**
     * [조회수 증가]
     * 🚨 실무 아키텍처 주의: 트래픽이 몰릴 때 이 메서드로 조회수를 올리면 '갱신 손실(Lost Update)'이 발생합니다.
     * 향후 Redis 단위로 조회수를 캐싱하고 Batch로 DB에 반영하거나,
     * Repository에서 JPQL 벌크 연산(UPDATE Image i SET i.viewCount = i.viewCount + 1...)을 사용하는 것을 권장합니다.
     */
    /**
     * [비즈니스 로직: 조회수 벌크 업데이트용]
     * Redis에서 모아온 조회수를 한 번에 더합니다.
     */
    public void addViewCount(Long addedCount) {
        if (addedCount != null && addedCount > 0) {
            this.viewCount += addedCount;
        }
    }

    public void changeStatus(ImageStatus newStatus) {
        Assert.notNull(newStatus, "상태값은 필수입니다.");
        this.status = newStatus;
    }

    public void updateInfo(String title, String description, Integer price) {
        Assert.hasText(title, "제목은 필수입니다.");
        if (price != null && price < 0) throw new IllegalArgumentException("가격은 0원 이상이어야 합니다.");

        this.title = title;
        this.description = description;
        this.price = price;
    }

    /**
     * [비즈니스 로직: 이미지 삭제 (Soft Delete)]
     * @SQLDelete에 의존하지 않고 명시적으로 삭제 상태를 제어합니다.
     */
    public void delete() {
        if (this.deletedAt != null) {
            throw new IllegalStateException("이미 삭제된 이미지입니다.");
        }
        // (선택 사항) 상태 코드도 명시적으로 변경하여 관리의 일관성을 줍니다.
        // this.status = ImageStatus.DELETED;

        this.deletedAt = LocalDateTime.now();
    }

    private void validateImage(Member seller, Category category, String title,
                               String originalUrl, String watermarkUrl, Integer price) {
        Assert.notNull(seller, "판매자 정보는 필수입니다.");
        Assert.notNull(category, "카테고리 정보는 필수입니다.");
        Assert.hasText(title, "제목은 필수입니다.");
        Assert.hasText(originalUrl, "원본 URL은 필수입니다.");
        Assert.hasText(watermarkUrl, "워터마크 URL은 필수입니다.");
        if (price != null && price < 0) throw new IllegalArgumentException("가격은 0원 이상이어야 합니다.");
    }

    // ==========================================
    // ⭐️ 완벽하게 교정된 Equals & HashCode
    // ==========================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Image image)) return false;
        return id != null && Objects.equals(id, image.id);
    }

    @Override
    public int hashCode() {
        // 🚨 아까 실수하셨던 부분 교정 완료!
        // Objects.hashCode(id)를 쓰면 영속화 전후로 해시값이 변합니다.
        return getClass().hashCode();
    }
}