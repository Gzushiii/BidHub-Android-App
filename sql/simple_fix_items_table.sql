-- Simple fix for items table - run this step by step
USE defaultdb;

-- Step 1: Check current structure (this should work)
DESCRIBE items;

-- Step 2: Check if we have any items
SELECT COUNT(*) as item_count FROM items;

-- Step 3: Check if starting_price column exists
SELECT COLUMN_NAME 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'starting_price';

-- Step 4: If starting_price doesn't exist, add it
ALTER TABLE items ADD COLUMN starting_price DECIMAL(10,2) DEFAULT 0.00;

-- Step 5: Copy data from starting_bid to starting_price if needed
UPDATE items SET starting_price = starting_bid WHERE starting_price = 0.00 AND starting_bid > 0.00;

-- Step 6: Check if current_price column exists  
SELECT COLUMN_NAME 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'current_price';

-- Step 7: If current_price doesn't exist, add it
ALTER TABLE items ADD COLUMN current_price DECIMAL(10,2) DEFAULT 0.00;

-- Step 8: Copy data from current_bid to current_price if needed
UPDATE items SET current_price = current_bid WHERE current_price = 0.00 AND current_bid > 0.00;

-- Step 9: Check if end_date column exists
SELECT COLUMN_NAME 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'end_date';

-- Step 10: If end_date doesn't exist, add it
ALTER TABLE items ADD COLUMN end_date TIMESTAMP NULL;

-- Step 11: Set default end_date for active items
UPDATE items SET end_date = DATE_ADD(created_at, INTERVAL 7 DAY) WHERE status = 'active' AND end_date IS NULL;

-- Step 12: Verify the final structure
DESCRIBE items;
