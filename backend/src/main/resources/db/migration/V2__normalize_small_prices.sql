-- Normalize obviously-too-small demo prices (e.g. 2.00) into IDR-scale values.
-- Heuristic: prices below 1000 are treated as "thousands" and scaled up 100_000x.
-- This is meant for local/demo databases only (app.seed.enabled=true scenarios).

UPDATE products
SET
  cost  = cost  * 100000,
  price = price * 100000,
  updated_at = CURRENT_TIMESTAMP
WHERE price > 0 AND price < 1000;

