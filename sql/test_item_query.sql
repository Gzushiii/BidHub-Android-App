-- Test the exact query that's failing
USE defaultdb;

-- Check the item structure
SELECT '=== CHECKING ITEM 5cae89dc-8017-4cbe-84c4-c4231444e76e ===' as section;
SELECT * FROM items WHERE id = '5cae89dc-8017-4cbe-84c4-c4231444e76e';

-- Check with status filter
SELECT '=== CHECKING WITH STATUS FILTER ===' as section;
SELECT * FROM items WHERE id = '5cae89dc-8017-4cbe-84c4-c4231444e76e' AND status = 'active';

-- Check the items table ID column type
SELECT '=== ITEMS TABLE ID COLUMN TYPE ===' as section;
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, COLUMN_KEY
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items' 
  AND COLUMN_NAME = 'id';
