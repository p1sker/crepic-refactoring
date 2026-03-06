package com.crepic.image.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Image 리포지토리 (Domain Layer)
 * JpaRepository를 상속받아 기본적인 CRUD를 수행하며, 도메인 핵심 조회 로직을 담당합니다.
 * * ⭐️ S+++급 실무 세팅: ImageRepositoryCustom을 상속받아 Querydsl의 강력한 동적 쿼리(No-Offset) 기능을 가져옵니다.
 */
public interface ImageRepository extends JpaRepository<Image, Long>, ImageRepositoryCustom {

    // ==========================================================
    // 🚀 1. 메인 갤러리 쿼리: Querydsl (No-Offset) 로 대체됨!
    // ==========================================================
    // 기존에 있던 아래 JPQL 쿼리는 이제 ImageRepositoryCustom의 findImagesByCursor()가
    // 압도적인 성능으로 대신 처리하므로 주석 처리(또는 삭제) 하셔도 좋습니다.
    /*
    @Query("SELECT i FROM Image i JOIN FETCH i.seller JOIN FETCH i.category")
    Slice<Image> findImagesWithFetchJoin(Pageable pageable);
    */

    // ==========================================================
    // 💡 2. 향후 추가될 수 있는 특수 조건 쿼리들 (Offset 기반 Slice 유지)
    // ==========================================================
    // 메인 피드가 아닌, 특정 작가의 마이페이지나 특정 카테고리 탭처럼
    // 데이터 개수가 상대적으로 적은 곳에서는 굳이 복잡한 Cursor를 쓰지 않고
    // 아래처럼 기존의 Slice + Pageable 방식을 써도 성능상 전혀 무리가 없습니다.

    /**
     * 특정 작가의 작품만 보기 (작가 마이페이지 등)
     * - 이미 sellerId로 필터링하므로 seller 정보는 굳이 Fetch Join 하지 않고 category만 Fetch Join 합니다.
     */
    @Query("SELECT i FROM Image i JOIN FETCH i.category WHERE i.seller.id = :sellerId AND i.deletedAt IS NULL")
    Slice<Image> findImagesBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    /**
     * 특정 카테고리의 작품만 보기 (카테고리 탭 클릭 시)
     * - categoryId로 필터링하므로 category 정보는 빼고 seller 정보만 Fetch Join 합니다.
     */
    @Query("SELECT i FROM Image i JOIN FETCH i.seller WHERE i.category.id = :categoryId AND i.deletedAt IS NULL")
    Slice<Image> findImagesByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    /**
     * 특정 상태의 작품만 보기 (예: 판매 중인 작품만 필터링)
     */
    @Query("SELECT i FROM Image i JOIN FETCH i.seller JOIN FETCH i.category WHERE i.status = :status AND i.deletedAt IS NULL")
    Slice<Image> findImagesByStatus(@Param("status") ImageStatus status, Pageable pageable);
}