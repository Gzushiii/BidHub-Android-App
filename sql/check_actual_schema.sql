-- Check the actual database schema to see what columns exist
-- This will help us fix the flexible item resolver

-- Check items table structure
DESCRIBE items;

-- Check what columns actually exist
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'items'
ORDER BY ORDINAL_POSITION;

-- Check for any items with UUIDs
SELECT '=== ITEMS WITH UUID_ID ===' as section;
SELECT id, uuid_id, title, status, seller_id, created_at
FROM items 
WHERE uuid_id IS NOT NULL AND uuid_id != ''
LIMIT 5;

-- Check for items without UUIDs
SELECT '=== ITEMS WITHOUT UUID_ID ===' as section;
SELECT id, uuid_id, title, status, seller_id, created_at
FROM items 
WHERE uuid_id IS NULL OR uuid_id = ''
LIMIT 5;

-- Check total count
SELECT '=== SUMMARY ===' as section;
SELECT 
  COUNT(*) as total_items,
  COUNT(CASE WHEN uuid_id IS NOT NULL AND uuid_id != '' THEN 1 END) as items_with_uuid,
  COUNT(CASE WHEN uuid_id IS NULL OR uuid_id = '' THEN 1 END) as items_without_uuid
FROM items;