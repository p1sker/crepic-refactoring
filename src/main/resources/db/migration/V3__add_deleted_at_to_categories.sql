-- ==========================================================
-- Category 테이블 Soft Delete 컬럼 추가
-- ==========================================================

-- 1. deleted_at 컬럼 추가
ALTER TABLE "categories" ADD COLUMN "deleted_at" timestamp;

-- 2. 성능 최적화를 위한 인덱스 추가 (조회 시 필터링 속도 향상)
CREATE INDEX idx_categories_deleted_at ON "categories" (deleted_at);

-- 3. 관리자 및 개발자를 위한 코멘트 추가
COMMENT ON COLUMN "categories"."deleted_at" IS '카테고리 삭제일시 (Soft Delete)';