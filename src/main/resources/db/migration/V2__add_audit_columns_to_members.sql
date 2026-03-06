-- ==========================================================
-- 1. 감사(Audit) 컬럼 추가 (Batch 처리)
-- 빅테크 스타일: 서비스 확장성을 위해 모든 테이블에 동일한 규격을 적용합니다.
-- ==========================================================

-- 회원/이미지/카테고리/태그
ALTER TABLE "members" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "images" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "categories" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "tags" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;

-- 주문/결제/장바구니/찜
ALTER TABLE "orders" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "order_lines" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "payments" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "carts" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "wishes" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;

-- 자산/정산/쿠폰
ALTER TABLE "wallets" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "point_ledgers" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "settlements" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "coupons" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "member_coupons" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;

-- 로그/알림/댓글
ALTER TABLE "download_rights" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "download_logs" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "comments" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;
ALTER TABLE "notifications" ADD COLUMN "created_by" bigint, ADD COLUMN "updated_by" bigint;


-- ==========================================================
-- 2. 성능 최적화를 위한 인덱스(Index) 추가 (중요!)
-- 빅테크 스타일: "누가 만들었나?"를 조회할 일이 압도적으로 많습니다.
-- 물리적 FK는 쓰기 성능 저하와 데드락 위험 때문에 지양하고, 인덱스로 조회 속도만 잡습니다.
-- ==========================================================

CREATE INDEX idx_members_created_by ON "members" (created_by);
CREATE INDEX idx_images_created_by ON "images" (created_by);
CREATE INDEX idx_orders_created_by ON "orders" (created_by);
CREATE INDEX idx_comments_created_by ON "comments" (created_by);
CREATE INDEX idx_wallets_updated_by ON "wallets" (updated_by);


-- ==========================================================
-- 3. 관리 명확성을 위한 코멘트 일괄 적용
-- ==========================================================

DO $$
    DECLARE
        t text;
    BEGIN
        FOR t IN SELECT table_name
                 FROM information_schema.tables
                 WHERE table_schema = 'public'
                   AND table_name IN ('members', 'images', 'categories', 'tags', 'orders', 'order_lines',
                                      'payments', 'wallets', 'point_ledgers', 'settlements',
                                      'download_rights', 'download_logs', 'coupons',
                                      'member_coupons', 'comments', 'carts', 'wishes', 'notifications')
            LOOP
                EXECUTE format('COMMENT ON COLUMN %I.created_by IS ''생성자 ID (Member ID)''', t);
                EXECUTE format('COMMENT ON COLUMN %I.updated_by IS ''최종 수정자 ID (Member ID)''', t);
            END LOOP;
    END $$;