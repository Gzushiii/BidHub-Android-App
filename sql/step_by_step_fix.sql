-- Step-by-step fix for items table (run each section separately)

-- SECTION 1: Check what we have
USE defaultdb;
DESCRIBE items;

-- SECTION 2: Check if columns exist
SELECT 'starting_price exists:' as check_type, COUNT(*) as exists_count
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'starting_price'

UNION ALL

SELECT 'current_price exists:', COUNT(*)
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'current_price'

UNION ALL

SELECT 'end_date exists:', COUNT(*)
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'end_date';

-- SECTION 3: Add starting_price only if it doesn't exist
-- (Only run this if the above shows starting_price doesn't exist)
-- ALTER TABLE items ADD COLUMN starting_price DECIMAL(10,2) DEFAULT 0.00;

-- SECTION 4: Add current_price only if it doesn't exist  
-- (Only run this if the above shows current_price doesn't exist)
-- ALTER TABLE items ADD COLUMN current_price DECIMAL(10,2) DEFAULT 0.00;

-- SECTION 5: Add end_date only if it doesn't exist
-- (Only run this if the above shows end_date doesn't exist)
-- ALTER TABLE items ADD COLUMN end_date TIMESTAMP NULL;

-- SECTION 6: Copy data (run this after adding columns)
-- UPDATE items SET starting_price = starting_bid WHERE id > 0 AND starting_price = 0.00 AND starting_bid > 0.00 LIMIT 1000;

-- SECTION 7: Copy current_bid data
-- UPDATE items SET current_price = current_bid WHERE id > 0 AND current_price = 0.00 AND current_bid > 0.00 LIMIT 1000;

-- SECTION 8: Set end_date for active items
-- UPDATE items SET end_date = DATE_ADD(created_at, INTERVAL 7 DAY) WHERE id > 0 AND status = 'active' AND end_date IS NULL LIMIT 1000;

-- SECTION 9: Verify results
-- DESCRIBE items;
-- SELECT id, title, starting_price, current_price, status, end_date FROM items LIMIT 5;

