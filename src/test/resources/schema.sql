CREATE TABLE company_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(100) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    name            VARCHAR(50) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE partner (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    business_number     VARCHAR(20),
    partner_type        VARCHAR(20) NOT NULL,
    contact_name        VARCHAR(50),
    contact_phone       VARCHAR(20),
    address             VARCHAR(255),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE company_info (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name        VARCHAR(100) NOT NULL,
    business_number     VARCHAR(20),
    ceo_name            VARCHAR(50),
    address             VARCHAR(255),
    phone               VARCHAR(20),
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
