-- Check the actual ID column type in items table
USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'CHECKING ID COLUMN TYPE AND DATA' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Check items table structure
SELECT 'Items table structure:' AS section;
DESCRIBE items;

SELECT '' AS '';

-- Check ID column details
SELECT 'ID column details:' AS section;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    CHARACTER_MAXIMUM_LENGTH,
    NUMERIC_PRECISION,
    NUMERIC_SCALE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb'
  AND TABLE_NAME = 'items'
  AND COLUMN_NAME = 'id';

SELECT '' AS '';

-- Show sample IDs and their types
SELECT 'Sample IDs from items table:' AS section;
SELECT 
    id,
    LENGTH(id) as id_length,
    title,
    status
FROM items
LIMIT 5;

SELECT '' AS '';

-- Check if there are any UUID-style IDs
SELECT 'Looking for UUID-style IDs:' AS section;
SELECT 
    id,
    title,
    status
FROM items
WHERE id LIKE '%-%-%-%-%'
LIMIT 5;

SELECT '' AS '';

-- Check if there are any string IDs
SELECT 'Looking for string IDs:' AS section;
SELECT 
    id,
    title,
    status
FROM items
WHERE id REGEXP '^[a-f0-9-]+$'
LIMIT 5;
