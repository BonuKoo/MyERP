-- ============================================================
-- MyERP 데이터베이스/계정 생성
-- ============================================================
CREATE DATABASE IF NOT EXISTS erp_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'erp_user'@'localhost' IDENTIFIED BY 'erp_pass1234!';
GRANT ALL PRIVILEGES ON erp_db.* TO 'erp_user'@'localhost';
FLUSH PRIVILEGES;

USE erp_db;

-- ============================================================
-- 1단계: 사용자 인증 + 거래처
-- ============================================================
CREATE TABLE company_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    email           VARCHAR(100) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    name            VARCHAR(50) NOT NULL,
    role            VARCHAR(20) NOT NULL,      -- OWNER, STAFF
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE partner (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    name                VARCHAR(100) NOT NULL,
    business_number     VARCHAR(20),
    partner_type        VARCHAR(20) NOT NULL,   -- SUPPLIER, CUSTOMER, BOTH
    contact_name        VARCHAR(50),
    contact_phone       VARCHAR(20),
    address             VARCHAR(255),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    receivable_balance  DECIMAL(14,2) NOT NULL DEFAULT 0,   -- 미수금 잔액 (5단계)
    payable_balance     DECIMAL(14,2) NOT NULL DEFAULT 0    -- 미지급금 잔액 (5단계)
);

CREATE TABLE company_info (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_name        VARCHAR(100) NOT NULL,
    business_number     VARCHAR(20),
    ceo_name            VARCHAR(50),
    address             VARCHAR(255),
    phone               VARCHAR(20),
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 2단계: 카테고리 / 품목 / 규격 / 재고
-- ============================================================
CREATE TABLE category_main (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(50) NOT NULL UNIQUE,
    display_order   INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE category_sub (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_main_id    BIGINT NOT NULL,
    name                VARCHAR(50) NOT NULL,
    display_order       INT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (category_main_id) REFERENCES category_main(id),
    UNIQUE (category_main_id, name)
);

CREATE TABLE item (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_sub_id     BIGINT NOT NULL,
    name                VARCHAR(150) NOT NULL,
    description         TEXT,
    ks_standard         VARCHAR(50),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_sub_id) REFERENCES category_sub(id)
);

CREATE TABLE item_spec (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id         BIGINT NOT NULL,
    spec_name       VARCHAR(50) NOT NULL,
    unit            VARCHAR(20) NOT NULL,
    cost_price      DECIMAL(12,2) NOT NULL DEFAULT 0,
    sale_price      DECIMAL(12,2) NOT NULL DEFAULT 0,
    current_stock   INT NOT NULL DEFAULT 0,
    safety_stock    INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    version         INT NOT NULL DEFAULT 0,   -- 낙관적 락용 (MyBatis에서 WHERE 조건으로 수동 처리)
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES item(id)
);

CREATE TABLE certification (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    name    VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE item_certification (
    item_id             BIGINT NOT NULL,
    certification_id    BIGINT NOT NULL,
    PRIMARY KEY (item_id, certification_id),
    FOREIGN KEY (item_id) REFERENCES item(id),
    FOREIGN KEY (certification_id) REFERENCES certification(id)
);

-- 품목 사진. 한 품목에 여러 장(1:N)이고 순서가 의미를 가지므로 display_order로
-- 정렬한다. 목록 카드에 쓸 대표 사진은 키가 아니라 속성이라 is_primary 플래그로 둔다
-- (품목당 최대 1건이 TRUE — 애플리케이션이 보장).
CREATE TABLE item_image (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id             BIGINT NOT NULL,
    upload_file_name    VARCHAR(255) NOT NULL,      -- 사용자가 올린 원본 파일명
    store_file_name     VARCHAR(255) NOT NULL,      -- 서버 저장명 (UUID.확장자)
    file_path           VARCHAR(500) NOT NULL,
    file_type           VARCHAR(100) NOT NULL,      -- MIME type
    file_size           BIGINT NOT NULL,
    display_order       INT NOT NULL DEFAULT 0,
    is_primary          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES item(id)
);

CREATE INDEX idx_item_image_item ON item_image (item_id, display_order);

CREATE TABLE stock_history (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_spec_id            BIGINT NOT NULL,
    change_type             VARCHAR(20) NOT NULL,   -- PURCHASE_IN, SALE_OUT, ADJUST
    quantity                INT NOT NULL,
    before_stock            INT NOT NULL,
    after_stock             INT NOT NULL,
    related_document_type   VARCHAR(20),             -- PURCHASE, SALE, MANUAL
    related_document_id     BIGINT,
    created_by              BIGINT NOT NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_spec_id) REFERENCES item_spec(id),
    FOREIGN KEY (created_by) REFERENCES company_user(id)
);

CREATE INDEX idx_stock_history_item_spec_created
ON stock_history (item_spec_id, created_at DESC);

-- ============================================================
-- 3단계: 매입 전표 (세금 계산 미적용, 단순 금액 처리)
-- ============================================================
CREATE TABLE purchase (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_no         VARCHAR(30) NOT NULL UNIQUE,
    partner_id          BIGINT NOT NULL,
    company_info_id     BIGINT NOT NULL,
    purchase_date       DATE NOT NULL,
    total_amount        DECIMAL(14,2) NOT NULL DEFAULT 0,
    status               VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',   -- DRAFT, CONFIRMED, CANCELED
    memo                 VARCHAR(255),
    created_by           BIGINT NOT NULL,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at          DATETIME,
    FOREIGN KEY (partner_id) REFERENCES partner(id),
    FOREIGN KEY (company_info_id) REFERENCES company_info(id),
    FOREIGN KEY (created_by) REFERENCES company_user(id)
);

CREATE TABLE purchase_item (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_id     BIGINT NOT NULL,
    item_spec_id    BIGINT NOT NULL,
    quantity        INT NOT NULL,
    unit_price      DECIMAL(12,2) NOT NULL,
    amount          DECIMAL(14,2) NOT NULL,
    FOREIGN KEY (purchase_id) REFERENCES purchase(id),
    FOREIGN KEY (item_spec_id) REFERENCES item_spec(id)
);

-- ============================================================
-- 4단계: 매출 전표
-- ============================================================
CREATE TABLE sale (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    sale_no             VARCHAR(30) NOT NULL UNIQUE,
    partner_id          BIGINT NOT NULL,
    company_info_id     BIGINT NOT NULL,
    sale_date           DATE NOT NULL,
    total_amount        DECIMAL(14,2) NOT NULL DEFAULT 0,
    status               VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    memo                 VARCHAR(255),
    created_by           BIGINT NOT NULL,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at           DATETIME,
    FOREIGN KEY (partner_id) REFERENCES partner(id),
    FOREIGN KEY (company_info_id) REFERENCES company_info(id),
    FOREIGN KEY (created_by) REFERENCES company_user(id)
);

CREATE TABLE sale_item (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sale_id          BIGINT NOT NULL,
    item_spec_id      BIGINT NOT NULL,
    quantity          INT NOT NULL,
    unit_price         DECIMAL(12,2) NOT NULL,
    amount             DECIMAL(14,2) NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sale(id),
    FOREIGN KEY (item_spec_id) REFERENCES item_spec(id)
);

-- ============================================================
-- 5단계: 미수금/미지급금 원장
-- ============================================================
CREATE TABLE ledger_entry (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    partner_id              BIGINT NOT NULL,
    ledger_type             VARCHAR(20) NOT NULL,   -- RECEIVABLE, PAYABLE
    change_type             VARCHAR(30) NOT NULL,   -- SALE_CONFIRMED, SALE_CANCELED, PURCHASE_CONFIRMED, PURCHASE_CANCELED, PAYMENT_RECEIVED, PAYMENT_RECEIVED_CANCELED, PAYMENT_PAID, PAYMENT_PAID_CANCELED
    amount                  DECIMAL(14,2) NOT NULL,  -- 부호 있는 증감액
    balance_after           DECIMAL(14,2) NOT NULL,
    related_document_type   VARCHAR(20),             -- SALE, PURCHASE, PAYMENT
    related_document_id     BIGINT,
    created_by              BIGINT NOT NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (partner_id) REFERENCES partner(id),
    FOREIGN KEY (created_by) REFERENCES company_user(id)
);

CREATE INDEX idx_ledger_entry_partner_created
ON ledger_entry (partner_id, created_at DESC);

CREATE TABLE payment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_no      VARCHAR(30) NOT NULL UNIQUE,
    partner_id      BIGINT NOT NULL,
    payment_type    VARCHAR(20) NOT NULL,   -- RECEIPT(수금), DISBURSEMENT(지급)
    amount          DECIMAL(14,2) NOT NULL,
    payment_date    DATE NOT NULL,
    method          VARCHAR(20),
    memo            VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_by      BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at     DATETIME,
    FOREIGN KEY (partner_id) REFERENCES partner(id),
    FOREIGN KEY (created_by) REFERENCES company_user(id)
);
