-- Correct potential over-scaling from V2__normalize_small_prices.sql.
--
-- Context:
-- V2 multiplies products.cost/price by 100000 when 0 < price < 1000. The intended
-- normalization in IDR is commonly *1000 (e.g. 65 -> 65000). If V2 was applied with
-- an incorrect factor, values become 100x too large.
--
-- This migration conservatively adjusts only values that appear to be scaled by 100000
-- (i.e. very large, divisible by 100, and within a reasonable upper bound).
--
-- NOTE: If your production data was intentionally scaled by 100000, do NOT apply this
-- migration (or adjust the predicate).

UPDATE products
SET
  cost  = cost  / 100,
  price = price / 100,
  updated_at = CURRENT_TIMESTAMP
WHERE
  price >= 100000
  AND price <  1000000000
  AND MOD(price, 100) = 0
  AND (cost = 0 OR MOD(cost, 100) = 0);

