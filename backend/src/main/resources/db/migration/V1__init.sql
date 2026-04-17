-- Smart POS + Inventory baseline schema (v1)
-- Notes:
--  * Stock ledger (stock_movements) is append-only at the application layer.
--  * Cancellation writes compensating rows; never mutate historical movements.
--  * Monetary values use NUMERIC(18,2). Quantities are INTEGER for v1 simplicity.

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    email           VARCHAR(180) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role  CHECK (role IN ('OWNER','CASHIER','WAREHOUSE'))
);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    token_hash      VARCHAR(255) NOT NULL,
    issued_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP NOT NULL,
    revoked         BOOLEAN   NOT NULL DEFAULT FALSE,
    replaced_by     UUID,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_exp  ON refresh_tokens(expires_at);

CREATE TABLE categories (
    id          UUID PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE TABLE suppliers (
    id          UUID PRIMARY KEY,
    name        VARCHAR(180) NOT NULL,
    phone       VARCHAR(40),
    address     VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id                  UUID PRIMARY KEY,
    sku                 VARCHAR(60)  NOT NULL,
    name                VARCHAR(200) NOT NULL,
    category_id         UUID,
    unit                VARCHAR(20)  NOT NULL,
    cost                NUMERIC(18,2) NOT NULL DEFAULT 0,
    price               NUMERIC(18,2) NOT NULL DEFAULT 0,
    barcode             VARCHAR(80),
    low_stock_threshold INTEGER NOT NULL DEFAULT 0,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_products_sku  UNIQUE (sku),
    CONSTRAINT fk_products_cat  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT ck_products_cost  CHECK (cost  >= 0),
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_lst   CHECK (low_stock_threshold >= 0)
);
CREATE INDEX idx_products_sku       ON products(sku);
CREATE INDEX idx_products_active    ON products(active);
CREATE INDEX idx_products_category  ON products(category_id);
CREATE INDEX idx_products_barcode   ON products(barcode);

CREATE TABLE purchases (
    id              UUID PRIMARY KEY,
    supplier_id     UUID NOT NULL,
    status          VARCHAR(30) NOT NULL,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchases_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_purchases_created  FOREIGN KEY (created_by)  REFERENCES users(id),
    CONSTRAINT ck_purchases_status   CHECK (status IN ('DRAFT','OPEN','PARTIALLY_RECEIVED','RECEIVED','CANCELLED'))
);
CREATE INDEX idx_purchases_supplier ON purchases(supplier_id);
CREATE INDEX idx_purchases_status   ON purchases(status);

CREATE TABLE purchase_items (
    id                  UUID PRIMARY KEY,
    purchase_id         UUID NOT NULL,
    product_id          UUID NOT NULL,
    qty_ordered         INTEGER NOT NULL,
    cost                NUMERIC(18,2) NOT NULL,
    qty_received_total  INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_pi_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE,
    CONSTRAINT fk_pi_product  FOREIGN KEY (product_id)  REFERENCES products(id),
    CONSTRAINT ck_pi_qty      CHECK (qty_ordered > 0),
    CONSTRAINT ck_pi_cost     CHECK (cost >= 0),
    CONSTRAINT ck_pi_recv     CHECK (qty_received_total >= 0 AND qty_received_total <= qty_ordered)
);
CREATE INDEX idx_purchase_items_purchase ON purchase_items(purchase_id);
CREATE INDEX idx_purchase_items_product  ON purchase_items(product_id);

CREATE TABLE purchase_receipts (
    id              UUID PRIMARY KEY,
    purchase_id     UUID NOT NULL,
    received_by     UUID NOT NULL,
    received_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pr_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_user     FOREIGN KEY (received_by) REFERENCES users(id)
);
CREATE INDEX idx_purchase_receipts_purchase ON purchase_receipts(purchase_id);

CREATE TABLE purchase_receipt_items (
    id              UUID PRIMARY KEY,
    receipt_id      UUID NOT NULL,
    product_id      UUID NOT NULL,
    qty_received    INTEGER NOT NULL,
    cost            NUMERIC(18,2) NOT NULL,
    CONSTRAINT fk_pri_receipt FOREIGN KEY (receipt_id) REFERENCES purchase_receipts(id) ON DELETE CASCADE,
    CONSTRAINT fk_pri_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_pri_qty     CHECK (qty_received > 0),
    CONSTRAINT ck_pri_cost    CHECK (cost >= 0)
);
CREATE INDEX idx_pri_receipt ON purchase_receipt_items(receipt_id);
CREATE INDEX idx_pri_product ON purchase_receipt_items(product_id);

CREATE TABLE sales (
    id              UUID PRIMARY KEY,
    invoice_no      VARCHAR(40)  NOT NULL,
    cashier_id      UUID         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    subtotal        NUMERIC(18,2) NOT NULL,
    discount        NUMERIC(18,2) NOT NULL DEFAULT 0,
    total           NUMERIC(18,2) NOT NULL,
    payment_method  VARCHAR(20)  NOT NULL,
    paid_amount     NUMERIC(18,2) NOT NULL,
    change_amount   NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at    TIMESTAMP,
    cancelled_by    UUID,
    cancel_reason   VARCHAR(500),
    CONSTRAINT uq_sales_invoice    UNIQUE (invoice_no),
    CONSTRAINT fk_sales_cashier    FOREIGN KEY (cashier_id)   REFERENCES users(id),
    CONSTRAINT fk_sales_cancelled  FOREIGN KEY (cancelled_by) REFERENCES users(id),
    CONSTRAINT ck_sales_status     CHECK (status IN ('COMPLETED','CANCELLED')),
    CONSTRAINT ck_sales_payment    CHECK (payment_method IN ('CASH','TRANSFER','EWALLET')),
    CONSTRAINT ck_sales_amounts    CHECK (subtotal >= 0 AND discount >= 0 AND total >= 0 AND paid_amount >= 0)
);
CREATE INDEX idx_sales_created_at ON sales(created_at);
CREATE INDEX idx_sales_cashier    ON sales(cashier_id);
CREATE INDEX idx_sales_status     ON sales(status);
CREATE INDEX idx_sales_payment    ON sales(payment_method);

CREATE TABLE sale_items (
    id              UUID PRIMARY KEY,
    sale_id         UUID NOT NULL,
    product_id      UUID NOT NULL,
    qty             INTEGER NOT NULL,
    unit_price      NUMERIC(18,2) NOT NULL,
    line_discount   NUMERIC(18,2) NOT NULL DEFAULT 0,
    line_total      NUMERIC(18,2) NOT NULL,
    CONSTRAINT fk_si_sale     FOREIGN KEY (sale_id)    REFERENCES sales(id) ON DELETE CASCADE,
    CONSTRAINT fk_si_product  FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_si_qty      CHECK (qty > 0),
    CONSTRAINT ck_si_price    CHECK (unit_price >= 0),
    CONSTRAINT ck_si_discount CHECK (line_discount >= 0),
    CONSTRAINT ck_si_total    CHECK (line_total >= 0)
);
CREATE INDEX idx_sale_items_sale    ON sale_items(sale_id);
CREATE INDEX idx_sale_items_product ON sale_items(product_id);

CREATE TABLE stock_movements (
    id          UUID PRIMARY KEY,
    product_id  UUID NOT NULL,
    type        VARCHAR(30) NOT NULL,
    ref_type    VARCHAR(30),
    ref_id      UUID,
    qty_delta   INTEGER NOT NULL,
    unit_cost   NUMERIC(18,2),
    note        VARCHAR(500),
    created_by  UUID,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sm_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_sm_user    FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_sm_type    CHECK (type IN ('PURCHASE_RECEIVE','SALE','ADJUSTMENT','SALE_CANCEL')),
    CONSTRAINT ck_sm_qty     CHECK (qty_delta <> 0)
);
CREATE INDEX idx_stock_movements_product_created_at ON stock_movements(product_id, created_at);
CREATE INDEX idx_stock_movements_type               ON stock_movements(type);
CREATE INDEX idx_stock_movements_ref                ON stock_movements(ref_type, ref_id);

CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY,
    actor_id      UUID,
    action        VARCHAR(80)  NOT NULL,
    entity_type   VARCHAR(60),
    entity_id     UUID,
    metadata      TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_actor      ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity     ON audit_logs(entity_type, entity_id);
