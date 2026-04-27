-- Add optional supplier reference to products so purchase flows can filter products by supplier.
ALTER TABLE products
    ADD COLUMN supplier_id UUID;

ALTER TABLE products
    ADD CONSTRAINT fk_products_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
        ON DELETE SET NULL;

CREATE INDEX idx_products_supplier ON products(supplier_id);

