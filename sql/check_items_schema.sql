-- Check the actual items table schema
USE defaultdb;

-- Show the current structure of the items table
DESCRIBE items;

-- Check if the required columns exist
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'items'
ORDER BY ORDINAL_POSITION;

-- Check if there are any items in the table
SELECT COUNT(*) as item_count FROM items;

-- Show sample items if any exist
SELECT * FROM items LIMIT 5;

