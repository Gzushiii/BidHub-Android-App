-- Check if v_active_items view exists and its structure
USE defaultdb;

-- Check if view exists
SELECT '=== CHECKING FOR v_active_items VIEW ===' as section;
SELECT TABLE_NAME, TABLE_TYPE
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'v_active_items';

-- Check view definition
SELECT '=== v_active_items VIEW DEFINITION ===' as section;
SELECT VIEW_DEFINITION
FROM information_schema.VIEWS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'v_active_items';

-- Test the view
SELECT '=== TESTING v_active_items VIEW ===' as section;
SELECT * FROM v_active_items LIMIT 3;
