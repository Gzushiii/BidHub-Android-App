-- Test database structure to identify the 'active' column issue
USE defaultdb;

-- Check items table structure
SELECT '=== ITEMS TABLE STRUCTURE ===' as section;
DESCRIBE items;

-- Check if status column exists
SELECT '=== STATUS COLUMN CHECK ===' as section;
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'status';

-- Check if active column exists
SELECT '=== ACTIVE COLUMN CHECK ===' as section;
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'active';

-- Test basic query
SELECT '=== BASIC ITEM QUERY TEST ===' as section;
SELECT id, title FROM items LIMIT 3;

-- Test status query
SELECT '=== STATUS QUERY TEST ===' as section;
SELECT id, title, status FROM items WHERE status = 'active' LIMIT 3;
