-- ==========================================================
-- [S+++급] 모든 테이블 Audit(감사) 및 Soft Delete 컬럼 통합 정렬
-- 목적: Java BaseEntity 상속에 따른 DB 스펙 동기화 (Error 방지)
-- ==========================================================

DO $$
    DECLARE
        t text;
        -- 정렬 대상 테이블 목록
        tables text[] := ARRAY[
            'members', 'images', 'categories', 'tags', 'image_tags',
            'carts', 'orders', 'order_lines', 'payments', 'wallets',
            'point_ledgers', 'settlements', 'download_rights',
            'download_logs', 'coupons', 'member_coupons', 'comments',
            'wishes', 'notifications'
            ];
    BEGIN
        FOREACH t IN ARRAY tables
            LOOP
                -- 1. created_at (생성일시)
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS created_at timestamp DEFAULT now()', t);

                -- 2. updated_at (수정일시)
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_at timestamp DEFAULT now()', t);

                -- 3. created_by (생성자 ID)
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS created_by bigint', t);

                -- 4. updated_by (수정자 ID)
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_by bigint', t);

                -- 5. deleted_at (논리 삭제일시 - Soft Delete용)
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS deleted_at timestamp', t);

                -- 컬럼 주석 추가
                EXECUTE format('COMMENT ON COLUMN %I.created_at IS ''생성 일시''', t);
                EXECUTE format('COMMENT ON COLUMN %I.updated_at IS ''수정 일시''', t);
                EXECUTE format('COMMENT ON COLUMN %I.created_by IS ''생성자 ID''', t);
                EXECUTE format('COMMENT ON COLUMN %I.updated_by IS ''수정자 ID''', t);
                EXECUTE format('COMMENT ON COLUMN %I.deleted_at IS ''삭제 일시 (Soft Delete)''', t);
            END LOOP;
    END $$;

-- 성능 최적화: 자주 쓰이는 Audit 조회용 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_categories_created_at ON "categories" (created_at);
CREATE INDEX IF NOT EXISTS idx_categories_deleted_at ON "categories" (deleted_at);