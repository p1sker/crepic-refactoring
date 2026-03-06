package com.crepic.image.domain;

import com.crepic.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * [실무 최적화 도메인: Category]
 * - DB 스키마(단순 UNIQUE 제약조건) 완벽 대응
 * - 삭제 시 Unique 제약 충돌 방지를 위해 도메인 레벨 데이터 더미화(Mutation) 적용
 */
@Entity
@Table(name = "categories") // DB 스크립트에 맞춰서 JPA에서의 Unique 명시 생략 (어차피 DB에 걸려있음)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 🚨 주의: @SQLDelete는 물리적 삭제(DELETE 쿼리)가 들어올 때 인터셉트해서 UPDATE로 바꾸는 용도입니다.
// 하지만 우리는 비즈니스 로직(delete() 메서드)에서 이름을 변경해야 하므로,
// JPA의 flush 시점에 UPDATE 쿼리가 자연스럽게 날아가도록 유도하는 것이 더 안전합니다.
// 따라서 @SQLDelete를 제거하고 도메인 로직으로 처리합니다.
@SQLRestriction("deleted_at IS NULL") // 조회 시 삭제된 데이터 자동 필터링 (필수 유지)
@ToString(of = {"id", "name"})
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    // DB: "name" varchar(50) UNIQUE NOT NULL
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // DB 스크립트 DO 블록에서 생성된 deleted_at 컬럼 매핑
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Category(String name) {
        validateName(name);
        this.name = name;
    }

    public static Category create(String name) {
        return Category.builder()
                .name(name)
                .build();
    }

    public void updateName(String newName) {
        validateName(newName);
        this.name = newName;
    }

    /**
     * [비즈니스 로직: 카테고리 삭제 (Soft Delete)]
     * DB의 UNIQUE 제약조건(uk_category_name)을 피하기 위해 이름을 더미화합니다.
     */
    public void delete() {
        if (this.deletedAt != null) {
            throw new IllegalStateException("이미 삭제된 카테고리입니다.");
        }

        // 1. 이름 더미화 (재사용 가능하도록 길 터주기)
        // DB length가 50이므로, UUID 앞부분과 조합하여 50자가 넘지 않도록 자릅니다.
        String dummySuffix = "_del_" + UUID.randomUUID().toString().substring(0, 8);
        if (this.name.length() + dummySuffix.length() > 50) {
            this.name = this.name.substring(0, 50 - dummySuffix.length());
        }
        this.name = this.name + dummySuffix;

        // 2. 삭제 시간 기록
        this.deletedAt = LocalDateTime.now();
    }

    private void validateName(String name) {
        Assert.hasText(name, "카테고리 이름은 비어있을 수 없습니다.");
        Assert.isTrue(name.length() <= 50, "카테고리 이름은 50자를 초과할 수 없습니다.");
    }

    // ==========================================
    // 엔티티 상태와 무관하게 안전한 Equals & HashCode
    // ==========================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category category)) return false;
        return id != null && Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}