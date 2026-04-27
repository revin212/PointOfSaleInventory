-- Store payment type snapshot + admin fee on sales.

ALTER TABLE sales ADD COLUMN payment_type_id UUID;
ALTER TABLE sales ADD COLUMN admin_fee       NUMERIC(18,2) NOT NULL DEFAULT 0;

CREATE INDEX idx_sales_payment_type ON sales(payment_type_id);

