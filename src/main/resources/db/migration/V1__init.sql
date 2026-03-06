CREATE TABLE "members" (
                           "id" bigserial PRIMARY KEY,
                           "email" varchar(255) UNIQUE NOT NULL,
                           "password" varchar(255) NOT NULL,
                           "nickname" varchar(100) UNIQUE NOT NULL,
                           "role" varchar(20) DEFAULT 'ROLE_USER',
                           "status" varchar(20) DEFAULT 'ACTIVE',
                           "created_at" timestamp DEFAULT (now()),
                           "updated_at" timestamp DEFAULT (now()),
                           "deleted_at" timestamp
);

CREATE TABLE "images" (
                          "id" bigserial PRIMARY KEY,
                          "seller_id" bigint NOT NULL,
                          "category_id" bigint NOT NULL,
                          "title" varchar(200) NOT NULL,
                          "description" text,
                          "price" integer NOT NULL DEFAULT 0,
                          "original_url" varchar(500) NOT NULL,
                          "watermark_url" varchar(500) NOT NULL,
                          "view_count" bigint DEFAULT 0,
                          "status" varchar(20) DEFAULT 'ON_SALE',
                          "created_at" timestamp DEFAULT (now()),
                          "updated_at" timestamp DEFAULT (now()),
                          "deleted_at" timestamp
);

CREATE TABLE "categories" (
                              "id" bigserial PRIMARY KEY,
                              "name" varchar(50) UNIQUE NOT NULL
);

CREATE TABLE "tags" (
                        "id" bigserial PRIMARY KEY,
                        "name" varchar(50) UNIQUE NOT NULL
);

CREATE TABLE "image_tags" (
                              "id" bigserial PRIMARY KEY,
                              "image_id" bigint NOT NULL,
                              "tag_id" bigint NOT NULL
);

CREATE TABLE "carts" (
                         "id" bigserial PRIMARY KEY,
                         "member_id" bigint NOT NULL,
                         "image_id" bigint NOT NULL,
                         "created_at" timestamp DEFAULT (now())
);

CREATE TABLE "orders" (
                          "id" bigserial PRIMARY KEY,
                          "buyer_id" bigint NOT NULL,
                          "order_no" varchar(50) UNIQUE NOT NULL,
                          "total_amount" integer NOT NULL,
                          "status" varchar(20) DEFAULT 'PENDING',
                          "version" integer DEFAULT 0,
                          "created_at" timestamp DEFAULT (now()),
                          "updated_at" timestamp DEFAULT (now())
);

CREATE TABLE "order_lines" (
                               "id" bigserial PRIMARY KEY,
                               "order_id" bigint NOT NULL,
                               "image_id" bigint NOT NULL,
                               "snapshot_price" integer NOT NULL
);

CREATE TABLE "payments" (
                            "id" bigserial PRIMARY KEY,
                            "order_id" bigint UNIQUE NOT NULL,
                            "pg_tid" varchar(100),
                            "method" varchar(50),
                            "amount" integer NOT NULL,
                            "status" varchar(20) DEFAULT 'READY',
                            "paid_at" timestamp
);

CREATE TABLE "wallets" (
                           "id" bigserial PRIMARY KEY,
                           "member_id" bigint UNIQUE NOT NULL,
                           "balance" integer NOT NULL DEFAULT 0,
                           "version" integer DEFAULT 0,
                           "updated_at" timestamp DEFAULT (now())
);

CREATE TABLE "point_ledgers" (
                                 "id" bigserial PRIMARY KEY,
                                 "wallet_id" bigint NOT NULL,
                                 "amount" integer NOT NULL,
                                 "balance_snapshot" integer NOT NULL,
                                 "transaction_type" varchar(50) NOT NULL,
                                 "reference_id" bigint,
                                 "description" varchar(255),
                                 "created_at" timestamp DEFAULT (now())
);

CREATE TABLE "settlements" (
                               "id" bigserial PRIMARY KEY,
                               "seller_id" bigint NOT NULL,
                               "order_line_id" bigint UNIQUE NOT NULL,
                               "fee_amount" integer NOT NULL,
                               "settlement_amount" integer NOT NULL,
                               "status" varchar(20) DEFAULT 'PENDING',
                               "settled_at" timestamp
);

CREATE TABLE "download_rights" (
                                   "id" bigserial PRIMARY KEY,
                                   "member_id" bigint NOT NULL,
                                   "image_id" bigint NOT NULL,
                                   "order_id" bigint NOT NULL,
                                   "is_active" boolean DEFAULT true,
                                   "granted_at" timestamp DEFAULT (now()),
                                   "expires_at" timestamp
);

CREATE TABLE "download_logs" (
                                 "id" bigserial PRIMARY KEY,
                                 "member_id" bigint NOT NULL,
                                 "image_id" bigint NOT NULL,
                                 "ip_address" varchar(45),
                                 "downloaded_at" timestamp DEFAULT (now())
);

CREATE TABLE "coupons" (
                           "id" bigserial PRIMARY KEY,
                           "name" varchar(100) NOT NULL,
                           "discount_amount" integer NOT NULL,
                           "total_quantity" integer,
                           "issued_quantity" integer DEFAULT 0,
                           "valid_from" timestamp NOT NULL,
                           "valid_until" timestamp NOT NULL
);

CREATE TABLE "member_coupons" (
                                  "id" bigserial PRIMARY KEY,
                                  "member_id" bigint NOT NULL,
                                  "coupon_id" bigint NOT NULL,
                                  "is_used" boolean DEFAULT false,
                                  "used_at" timestamp,
                                  "created_at" timestamp DEFAULT (now())
);

CREATE TABLE "comments" (
                            "id" bigserial PRIMARY KEY,
                            "image_id" bigint NOT NULL,
                            "member_id" bigint NOT NULL,
                            "content" text NOT NULL,
                            "rating" integer,
                            "created_at" timestamp DEFAULT (now()),
                            "updated_at" timestamp DEFAULT (now()),
                            "deleted_at" timestamp
);

CREATE TABLE "wishes" (
                          "id" bigserial PRIMARY KEY,
                          "member_id" bigint NOT NULL,
                          "image_id" bigint NOT NULL,
                          "created_at" timestamp DEFAULT (now())
);

CREATE TABLE "notifications" (
                                 "id" bigserial PRIMARY KEY,
                                 "member_id" bigint NOT NULL,
                                 "title" varchar(100) NOT NULL,
                                 "content" text NOT NULL,
                                 "is_read" boolean DEFAULT false,
                                 "created_at" timestamp DEFAULT (now())
);

COMMENT ON COLUMN "members"."id" IS '회원 고유 식별자';

COMMENT ON COLUMN "members"."email" IS '로그인 이메일';

COMMENT ON COLUMN "members"."deleted_at" IS '탈퇴일시 (Soft Delete)';

COMMENT ON COLUMN "orders"."version" IS '동시성 제어';

ALTER TABLE "images" ADD FOREIGN KEY ("seller_id") REFERENCES "members" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "images" ADD FOREIGN KEY ("category_id") REFERENCES "categories" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "image_tags" ADD FOREIGN KEY ("tag_id") REFERENCES "tags" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "image_tags" ADD FOREIGN KEY ("image_id") REFERENCES "images" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "carts" ADD FOREIGN KEY ("member_id") REFERENCES "members" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "carts" ADD FOREIGN KEY ("image_id") REFERENCES "images" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "orders" ADD FOREIGN KEY ("buyer_id") REFERENCES "members" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "order_lines" ADD FOREIGN KEY ("order_id") REFERENCES "orders" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "order_lines" ADD FOREIGN KEY ("image_id") REFERENCES "images" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "payments" ADD FOREIGN KEY ("order_id") REFERENCES "orders" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "download_rights" ADD FOREIGN KEY ("member_id") REFERENCES "members" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "download_rights" ADD FOREIGN KEY ("image_id") REFERENCES "images" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "download_rights" ADD FOREIGN KEY ("order_id") REFERENCES "orders" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "download_logs" ADD FOREIGN KEY ("member_id") REFERENCES "members" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "download_logs" ADD FOREIGN KEY ("image_id") REFERENCES "images" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "settlements" ADD FOREIGN KEY ("seller_id") REFERENCES "members" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "settlements" ADD FOREIGN KEY ("order_line_id") REFERENCES "order_lines" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "comments" ADD FOREIGN KEY ("member_id") REFERENCES "members" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "comments" ADD FOREIGN KEY ("image_id") REFERENCES "images" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "wishes" ADD FOREIGN KEY ("member_id") REFERENCES "members" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "wishes" ADD FOREIGN KEY ("image_id") REFERENCES "images" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "wallets" ADD FOREIGN KEY ("member_id") REFERENCES "members" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "point_ledgers" ADD FOREIGN KEY ("wallet_id") REFERENCES "wallets" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "member_coupons" ADD FOREIGN KEY ("member_id") REFERENCES "members" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "member_coupons" ADD FOREIGN KEY ("coupon_id") REFERENCES "coupons" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "notifications" ADD FOREIGN KEY ("member_id") REFERENCES "members" ("id") DEFERRABLE INITIALLY IMMEDIATE;
