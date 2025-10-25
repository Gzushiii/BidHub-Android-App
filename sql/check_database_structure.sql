-- Check database structure to identify the 'active' column issue
USE defaultdb;

-- Check if items table exists and its structure
SELECT '=== ITEMS TABLE STRUCTURE ===' as section;
DESCRIBE items;

-- Check if there's an 'active' column
SELECT '=== CHECKING FOR ACTIVE COLUMN ===' as section;
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'active';

-- Check if there's a 'status' column
SELECT '=== CHECKING FOR STATUS COLUMN ===' as section;
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'status';

-- Check sample data
SELECT '=== SAMPLE ITEMS DATA ===' as section;
SELECT id, title, status FROM items LIMIT 3;

-- Check if PlaceBid procedure exists
SELECT '=== CHECKING PLACEBID PROCEDURE ===' as section;
SELECT ROUTINE_NAME, ROUTINE_TYPE, ROUTINE_DEFINITION
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
  AND ROUTINE_NAME = 'PlaceBid';
