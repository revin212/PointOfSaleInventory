-- Maintain atomic on-hand quantities per product.
-- The existing stock_movements table remains append-only as the source of history.
-- product_stocks provides a concurrency-safe current balance.

CREATE TABLE product_stocks (
    product_id  UUID PRIMARY KEY,
    on_hand     INTEGER NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_stocks_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_stocks_on_hand ON product_stocks(on_hand);

-- Backfill from existing movements (if any).
INSERT INTO product_stocks (product_id, on_hand, updated_at)
SELECT m.product_id, COALESCE(SUM(m.qty_delta), 0) AS on_hand, CURRENT_TIMESTAMP
FROM stock_movements m
GROUP BY m.product_id
;

