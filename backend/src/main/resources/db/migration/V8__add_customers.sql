-- Customers module + optional link to sales.

CREATE TABLE customers (
    id          UUID PRIMARY KEY,
    name        VARCHAR(180) NOT NULL,
    phone       VARCHAR(40),
    email       VARCHAR(180),
    notes       VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_customers_name  ON customers(name);
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_email ON customers(email);

ALTER TABLE sales
    ADD COLUMN customer_id UUID;

ALTER TABLE sales
    ADD CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;

CREATE INDEX idx_sales_customer ON sales(customer_id);

