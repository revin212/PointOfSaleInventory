-- Payment types (payment method + admin fee configuration)
-- Admin fee is flat currency amount (IDR) for now.

CREATE TABLE payment_types (
    id          UUID PRIMARY KEY,
    method      VARCHAR(20)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    admin_fee   NUMERIC(18,2) NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_types_method UNIQUE (method),
    CONSTRAINT ck_payment_types_method CHECK (method IN ('CASH','TRANSFER','EWALLET')),
    CONSTRAINT ck_payment_types_admin_fee CHECK (admin_fee >= 0)
);

