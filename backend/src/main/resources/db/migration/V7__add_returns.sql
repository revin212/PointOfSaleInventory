-- Sales returns (retur) support.

-- 1) Allow RETURN movement type.
ALTER TABLE stock_movements DROP CONSTRAINT ck_sm_type;
ALTER TABLE stock_movements
    ADD CONSTRAINT ck_sm_type CHECK (type IN ('PURCHASE_RECEIVE','SALE','ADJUSTMENT','SALE_CANCEL','RETURN'));

-- 2) Returns tables.
CREATE TABLE sale_returns (
    id            UUID PRIMARY KEY,
    sale_id       UUID NOT NULL,
    created_by    UUID NOT NULL,
    reason        VARCHAR(500),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sale_returns_sale FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_returns_user FOREIGN KEY (created_by) REFERENCES users(id)
);
CREATE INDEX idx_sale_returns_sale_created_at ON sale_returns(sale_id, created_at);

CREATE TABLE sale_return_items (
    id            UUID PRIMARY KEY,
    sale_return_id UUID NOT NULL,
    product_id    UUID NOT NULL,
    qty           INTEGER NOT NULL,
    CONSTRAINT fk_sri_return FOREIGN KEY (sale_return_id) REFERENCES sale_returns(id) ON DELETE CASCADE,
    CONSTRAINT fk_sri_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_sri_qty CHECK (qty > 0)
);
CREATE INDEX idx_sri_return ON sale_return_items(sale_return_id);
CREATE INDEX idx_sri_product ON sale_return_items(product_id);

