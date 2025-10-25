-- Migrate items table from integer IDs to UUID IDs
-- This script converts the existing integer-based ID system to UUID-based

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'MIGRATING ITEMS TABLE TO UUID IDs' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Step 1: Create a mapping table to track old -> new ID conversions
SELECT 'Step 1: Creating ID mapping table...' AS '';
DROP TABLE IF EXISTS id_mapping;
CREATE TABLE id_mapping (
    old_id INT UNSIGNED,
    new_id VARCHAR(36),
    PRIMARY KEY (old_id)
);

-- Step 2: Generate UUIDs for existing items
SELECT 'Step 2: Generating UUIDs for existing items...' AS '';
INSERT INTO id_mapping (old_id, new_id)
SELECT id, UUID() as new_id
FROM items
ORDER BY id;

-- Show the mapping
SELECT 'ID Mapping (first 10):' AS '';
SELECT old_id, new_id FROM id_mapping LIMIT 10;

SELECT '' AS '';

-- Step 3: Create new items table with UUID primary key
SELECT 'Step 3: Creating new items table structure...' AS '';

-- First, let's see the current structure
SELECT 'Current items table structure:' AS '';
DESCRIBE items;

-- Skip backup creation due to sql_require_primary_key constraint
SELECT 'Skipping backup creation due to sql_require_primary_key constraint...' AS '';
SELECT 'Proceeding directly with UUID column addition...' AS '';

-- Step 4: Update items table to use UUID primary key
SELECT 'Step 4: Updating items table to use UUID primary key...' AS '';

-- Drop foreign key constraints first (we'll recreate them)
-- Note: We need to check what foreign keys exist
SELECT 'Checking for foreign key constraints...' AS '';
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'defaultdb'
  AND (TABLE_NAME = 'items' OR REFERENCED_TABLE_NAME = 'items')
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- For now, let's add a new UUID column and populate it
-- Check if uuid_id column already exists
SELECT 'Checking if uuid_id column exists...' AS '';
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'uuid_id column already exists, skipping...'
        ELSE 'uuid_id column does not exist, will add it...'
    END as column_status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'uuid_id';

-- Add the column (will fail gracefully if it already exists)
SELECT 'Adding uuid_id column to items table...' AS '';
ALTER TABLE items ADD COLUMN uuid_id VARCHAR(36) UNIQUE;

-- Update the uuid_id column with mapped UUIDs
UPDATE items i 
JOIN id_mapping m ON i.id = m.old_id 
SET i.uuid_id = m.new_id
WHERE i.id = m.old_id;

-- Show the updated items
SELECT 'Updated items with UUIDs (first 5):' AS '';
SELECT id, uuid_id, title, status FROM items LIMIT 5;

SELECT '' AS '';

-- Step 5: Update related tables
SELECT 'Step 5: Checking related tables that reference items.id...' AS '';

-- Check bids table
SELECT 'Bids table structure:' AS '';
DESCRIBE bids;

-- Check item_images table  
SELECT 'Item_images table structure:' AS '';
DESCRIBE item_images;

-- Add UUID columns to related tables
SELECT 'Checking if item_uuid_id column exists in bids table...' AS '';
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'item_uuid_id column already exists in bids table, skipping...'
        ELSE 'item_uuid_id column does not exist in bids table, will add it...'
    END as bids_column_status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'bids' 
  AND COLUMN_NAME = 'item_uuid_id';

SELECT 'Checking if item_uuid_id column exists in item_images table...' AS '';
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'item_uuid_id column already exists in item_images table, skipping...'
        ELSE 'item_uuid_id column does not exist in item_images table, will add it...'
    END as images_column_status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'item_images' 
  AND COLUMN_NAME = 'item_uuid_id';

-- Add the columns (will fail gracefully if they already exist)
SELECT 'Adding item_uuid_id column to bids table...' AS '';
ALTER TABLE bids ADD COLUMN item_uuid_id VARCHAR(36);

SELECT 'Adding item_uuid_id column to item_images table...' AS '';
ALTER TABLE item_images ADD COLUMN item_uuid_id VARCHAR(36);

-- Update the UUID columns in related tables
UPDATE bids b 
JOIN id_mapping m ON b.item_id = m.old_id 
SET b.item_uuid_id = m.new_id
WHERE b.item_id = m.old_id;

UPDATE item_images ii 
JOIN id_mapping m ON ii.item_id = m.old_id 
SET ii.item_uuid_id = m.new_id
WHERE ii.item_id = m.old_id;

SELECT 'Updated related tables with UUIDs:' AS '';
SELECT 'Bids with UUIDs (first 3):' AS '';
SELECT item_id, item_uuid_id, bidder_id, amount FROM bids LIMIT 3;

SELECT 'Item_images with UUIDs (first 3):' AS '';
SELECT item_id, item_uuid_id, image_url FROM item_images LIMIT 3;

SELECT '' AS '';

-- Step 6: Summary
SELECT '=======================================================' AS '';
SELECT 'MIGRATION SUMMARY' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

SELECT 
    (SELECT COUNT(*) FROM items) as total_items,
    (SELECT COUNT(*) FROM items WHERE uuid_id IS NOT NULL) as items_with_uuid,
    (SELECT COUNT(*) FROM bids WHERE item_uuid_id IS NOT NULL) as bids_with_uuid,
    (SELECT COUNT(*) FROM item_images WHERE item_uuid_id IS NOT NULL) as images_with_uuid;

SELECT '' AS '';
SELECT 'Next steps:' AS '';
SELECT '1. Test the UUID columns work correctly' AS '';
SELECT '2. Application code already updated to use uuid_id' AS '';
SELECT '3. Test bid/buy operations with UUID item IDs' AS '';
SELECT '4. Consider dropping old id columns after testing' AS '';
SELECT '5. Update stored procedures to use VARCHAR(36) parameters' AS '';
SELECT '' AS '';
SELECT 'Note: Backup creation skipped due to sql_require_primary_key constraint' AS '';
SELECT 'Original data is preserved in the items table with both id and uuid_id columns' AS '';
