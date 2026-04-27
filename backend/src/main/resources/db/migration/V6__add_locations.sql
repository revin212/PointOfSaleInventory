-- Multi-location readiness (default single location).
-- Keep current behavior by creating one default location and using it implicitly.

CREATE TABLE locations (
    id          UUID PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_locations_code UNIQUE (code)
);

-- Add location_id to stock movements.
ALTER TABLE stock_movements
    ADD COLUMN location_id UUID;

ALTER TABLE stock_movements
    ADD CONSTRAINT fk_stock_movements_location
        FOREIGN KEY (location_id) REFERENCES locations(id);

CREATE INDEX idx_stock_movements_location_created_at
    ON stock_movements(location_id, created_at);

-- Create default location.
INSERT INTO locations (id, code, name, is_default)
SELECT '00000000-0000-0000-0000-000000000001', 'DEFAULT', 'Default Location', TRUE
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE code = 'DEFAULT');

-- Backfill existing rows to default location.
UPDATE stock_movements
SET location_id = '00000000-0000-0000-0000-000000000001'
WHERE location_id IS NULL;

-- Ensure not-null going forward.
ALTER TABLE stock_movements
    ALTER COLUMN location_id SET NOT NULL;

-- Rebuild product_stocks with composite PK (product_id, location_id) for multi-location readiness.
CREATE TABLE product_stocks_v2 (
    product_id  UUID NOT NULL,
    location_id UUID NOT NULL,
    on_hand     INTEGER NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_product_stocks PRIMARY KEY (product_id, location_id),
    CONSTRAINT fk_product_stocks_v2_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_stocks_v2_location FOREIGN KEY (location_id) REFERENCES locations(id)
);

CREATE INDEX idx_product_stocks_location_on_hand ON product_stocks_v2(location_id, on_hand);

INSERT INTO product_stocks_v2 (product_id, location_id, on_hand, updated_at)
SELECT product_id, '00000000-0000-0000-0000-000000000001', on_hand, updated_at
FROM product_stocks;

DROP TABLE product_stocks;

ALTER TABLE product_stocks_v2 RENAME TO product_stocks;

