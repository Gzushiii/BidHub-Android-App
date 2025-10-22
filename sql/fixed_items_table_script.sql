-- Fixed script for items table - handles existing columns and safe update mode
USE defaultdb;

-- Step 1: Check current structure
DESCRIBE items;

-- Step 2: Check if starting_price column exists
SELECT COLUMN_NAME 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'starting_price';

-- Step 3: Only add starting_price if it doesn't exist
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = 'defaultdb' 
       AND TABLE_NAME = 'items' 
       AND COLUMN_NAME = 'starting_price') = 0,
    'ALTER TABLE items ADD COLUMN starting_price DECIMAL(10,2) DEFAULT 0.00',
    'SELECT "starting_price column already exists" as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 4: Check if current_price column exists
SELECT COLUMN_NAME 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'current_price';

-- Step 5: Only add current_price if it doesn't exist
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = 'defaultdb' 
       AND TABLE_NAME = 'items' 
       AND COLUMN_NAME = 'current_price') = 0,
    'ALTER TABLE items ADD COLUMN current_price DECIMAL(10,2) DEFAULT 0.00',
    'SELECT "current_price column already exists" as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 6: Check if end_date column exists
SELECT COLUMN_NAME 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'end_date';

-- Step 7: Only add end_date if it doesn't exist
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = 'defaultdb' 
       AND TABLE_NAME = 'items' 
       AND COLUMN_NAME = 'end_date') = 0,
    'ALTER TABLE items ADD COLUMN end_date TIMESTAMP NULL',
    'SELECT "end_date column already exists" as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 8: Copy data from starting_bid to starting_price (with safe update mode fix)
UPDATE items 
SET starting_price = starting_bid 
WHERE id > 0 AND starting_price = 0.00 AND starting_bid > 0.00
LIMIT 1000;

-- Step 9: Copy data from current_bid to current_price (with safe update mode fix)
UPDATE items 
SET current_price = current_bid 
WHERE id > 0 AND current_price = 0.00 AND current_bid > 0.00
LIMIT 1000;

-- Step 10: Set default end_date for active items (with safe update mode fix)
UPDATE items 
SET end_date = DATE_ADD(created_at, INTERVAL 7 DAY) 
WHERE id > 0 AND status = 'active' AND end_date IS NULL
LIMIT 1000;

-- Step 11: Verify the final structure
DESCRIBE items;

-- Step 12: Show sample data
SELECT id, title, starting_price, current_price, status, end_date 
FROM items 
LIMIT 5;

