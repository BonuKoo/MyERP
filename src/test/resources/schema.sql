CREATE TABLE IF NOT EXISTS company_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(100) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    name            VARCHAR(50) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS partner (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    business_number     VARCHAR(20),
    partner_type        VARCHAR(20) NOT NULL,
    contact_name        VARCHAR(50),
    contact_phone       VARCHAR(20),
    address             VARCHAR(255),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    receivable_balance  DECIMAL(14,2) NOT NULL DEFAULT 0,
    payable_balance     DECIMAL(14,2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS company_info (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name        VARCHAR(100) NOT NULL,
    business_number     VARCHAR(20),
    ceo_name            VARCHAR(50),
    address             VARCHAR(255),
    phone               VARCHAR(20),
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS category_main (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
    display_order   INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS category_sub (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_main_id    BIGINT NOT NULL,
    name                VARCHAR(50) NOT NULL,
    display_order       INT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (category_main_id, name)
);

CREATE TABLE IF NOT EXISTS item (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_sub_id     BIGINT NOT NULL,
    name                VARCHAR(150) NOT NULL,
    description         TEXT,
    ks_standard         VARCHAR(50),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS item_spec (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id         BIGINT NOT NULL,
    spec_name       VARCHAR(50) NOT NULL,
    unit            VARCHAR(20) NOT NULL,
    cost_price      DECIMAL(12,2) NOT NULL DEFAULT 0,
    sale_price      DECIMAL(12,2) NOT NULL DEFAULT 0,
    current_stock   INT NOT NULL DEFAULT 0,
    safety_stock    INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    version         INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS certification (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS item_certification (
    item_id             BIGINT NOT NULL,
    certification_id    BIGINT NOT NULL,
    PRIMARY KEY (item_id, certification_id)
);

CREATE TABLE IF NOT EXISTS item_image (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id             BIGINT NOT NULL,
    upload_file_name    VARCHAR(255) NOT NULL,
    store_file_name     VARCHAR(255) NOT NULL,
    file_path           VARCHAR(500) NOT NULL,
    file_type           VARCHAR(100) NOT NULL,
    file_size           BIGINT NOT NULL,
    display_order       INT NOT NULL DEFAULT 0,
    is_primary          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stock_history (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_spec_id            BIGINT NOT NULL,
    change_type             VARCHAR(20) NOT NULL,
    quantity                INT NOT NULL,
    before_stock            INT NOT NULL,
    after_stock             INT NOT NULL,
    related_document_type   VARCHAR(20),
    related_document_id     BIGINT,
    created_by              BIGINT NOT NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchase (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_no         VARCHAR(30) NOT NULL UNIQUE,
    partner_id          BIGINT NOT NULL,
    company_info_id     BIGINT NOT NULL,
    purchase_date       DATE NOT NULL,
    total_amount        DECIMAL(14,2) NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    memo                VARCHAR(255),
    created_by          BIGINT NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at         DATETIME
);

CREATE TABLE IF NOT EXISTS purchase_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_id     BIGINT NOT NULL,
    item_spec_id    BIGINT NOT NULL,
    quantity        INT NOT NULL,
    unit_price      DECIMAL(12,2) NOT NULL,
    amount          DECIMAL(14,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS sale (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_no             VARCHAR(30) NOT NULL UNIQUE,
    partner_id          BIGINT NOT NULL,
    company_info_id     BIGINT NOT NULL,
    sale_date           DATE NOT NULL,
    total_amount        DECIMAL(14,2) NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    memo                VARCHAR(255),
    created_by          BIGINT NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at         DATETIME
);

CREATE TABLE IF NOT EXISTS sale_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id         BIGINT NOT NULL,
    item_spec_id    BIGINT NOT NULL,
    quantity        INT NOT NULL,
    unit_price      DECIMAL(12,2) NOT NULL,
    amount          DECIMAL(14,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS ledger_entry (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    partner_id              BIGINT NOT NULL,
    ledger_type             VARCHAR(20) NOT NULL,
    change_type             VARCHAR(30) NOT NULL,
    amount                  DECIMAL(14,2) NOT NULL,
    balance_after           DECIMAL(14,2) NOT NULL,
    related_document_type   VARCHAR(20),
    related_document_id     BIGINT,
    created_by              BIGINT NOT NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_no      VARCHAR(30) NOT NULL UNIQUE,
    partner_id      BIGINT NOT NULL,
    payment_type    VARCHAR(20) NOT NULL,
    amount          DECIMAL(14,2) NOT NULL,
    payment_date    DATE NOT NULL,
    method          VARCHAR(20),
    memo            VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_by      BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at     DATETIME
);

CREATE TABLE IF NOT EXISTS department (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_position (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    allowance   DECIMAL(12,2) NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS employee (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id       BIGINT NOT NULL,
    position_id         BIGINT NOT NULL,
    company_user_id     BIGINT UNIQUE,
    name                VARCHAR(50) NOT NULL,
    phone               VARCHAR(20),
    email               VARCHAR(100),
    hire_date           DATE NOT NULL,
    resignation_date    DATE,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS attendance (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id     BIGINT NOT NULL,
    work_date       DATE NOT NULL,
    clock_in        DATETIME,
    clock_out       DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (employee_id, work_date)
);

CREATE TABLE IF NOT EXISTS leave_request (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id     BIGINT NOT NULL,
    leave_type      VARCHAR(20) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    leave_days      DECIMAL(4,1) NOT NULL,
    reason          VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by     BIGINT,
    approved_at     DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS salary_setting (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    daily_wage  DECIMAL(12,2) NOT NULL,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS salary (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id             BIGINT NOT NULL,
    pay_year_month          VARCHAR(7) NOT NULL,
    work_days               INT NOT NULL,
    base_pay                DECIMAL(12,2) NOT NULL,
    position_allowance      DECIMAL(12,2) NOT NULL DEFAULT 0,
    overtime_pay            DECIMAL(12,2) NOT NULL DEFAULT 0,
    gross_pay               DECIMAL(12,2) NOT NULL,
    income_tax              DECIMAL(12,2) NOT NULL,
    resident_tax            DECIMAL(12,2) NOT NULL,
    national_pension        DECIMAL(12,2) NOT NULL,
    health_insurance        DECIMAL(12,2) NOT NULL,
    employment_insurance    DECIMAL(12,2) NOT NULL,
    total_deduction         DECIMAL(12,2) NOT NULL,
    net_pay                 DECIMAL(12,2) NOT NULL,
    calculated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (employee_id, pay_year_month)
);
