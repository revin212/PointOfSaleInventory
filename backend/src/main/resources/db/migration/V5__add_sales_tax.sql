-- Simple VAT (PPN) support on sales.
-- For v1: store tax rate + tax amount + net (before tax, after discounts).

ALTER TABLE sales ADD COLUMN net_amount NUMERIC(18,2) NOT NULL DEFAULT 0;
ALTER TABLE sales ADD COLUMN tax_rate   NUMERIC(8,6)  NOT NULL DEFAULT 0;
ALTER TABLE sales ADD COLUMN tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

-- Backfill existing rows: treat historical totals as net (tax = 0).
UPDATE sales
SET
    net_amount = total,
    tax_rate = 0,
    tax_amount = 0
WHERE net_amount = 0 AND tax_amount = 0;

