package com.crepic.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // ⭐️ 상속받는 자식 클래스에게 이 필드들을 DB 컬럼으로 물려줌
@EntityListeners(AuditingEntityListener.class) // ⭐️ 스프링아, 시간/작성자 변경되는지 네가 감시해!
public abstract class BaseEntity {

    // ==========================================
    // 1. 시간 기록 (When)
    // ==========================================

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==========================================
    // 2. ⭐️ 감사(Audit) 기록 (Who) - S+++급 추가 포인트
    // ==========================================

    @CreatedBy
    @Column(name = "created_by", updatable = false) // 최초 생성자는 바뀌면 안 되니까 updatable = false!
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;
}