-- Add refund bookkeeping to sale returns.

ALTER TABLE sale_returns
    ADD COLUMN refund_method VARCHAR(20),
    ADD COLUMN refund_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

ALTER TABLE sale_returns
    ADD CONSTRAINT ck_sale_returns_refund_amount CHECK (refund_amount >= 0);

