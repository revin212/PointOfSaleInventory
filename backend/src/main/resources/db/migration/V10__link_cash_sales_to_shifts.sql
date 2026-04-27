-- Link CASH sales to shifts (cash drawer reconciliation).

ALTER TABLE sales
    ADD COLUMN shift_id UUID;

ALTER TABLE sales
    ADD CONSTRAINT fk_sales_shift FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE SET NULL;

CREATE INDEX idx_sales_shift ON sales(shift_id);

