-- Test script to verify the flexible item resolver works correctly
-- This tests the new backend item lookup functionality

-- First, let's see what items exist
SELECT '=== EXISTING ITEMS ===' as section;
SELECT id, uuid_id, title, status, seller_id 
FROM items 
WHERE status = 'active' 
LIMIT 5;

-- Test 1: Lookup by numeric ID
SELECT '=== TEST 1: NUMERIC ID LOOKUP ===' as section;
SELECT id, uuid_id, title, status 
FROM items 
WHERE id = 1 
LIMIT 1;

-- Test 2: Lookup by UUID (if any exist)
SELECT '=== TEST 2: UUID LOOKUP ===' as section;
SELECT id, uuid_id, title, status 
FROM items 
WHERE uuid_id IS NOT NULL AND uuid_id != '' 
LIMIT 1;

-- Test 3: Lookup by title (for debugging)
SELECT '=== TEST 3: TITLE LOOKUP ===' as section;
SELECT id, uuid_id, title, status 
FROM items 
WHERE title LIKE '%Bunny%' 
LIMIT 1;

-- Test 4: Test the flexible lookup logic (simulate what the resolver does)
SELECT '=== TEST 4: FLEXIBLE LOOKUP SIMULATION ===' as section;

-- Simulate UUID lookup
SET @test_id = 'fd2edd76-58b6-4197-9863-254413bc13d8';
SELECT 
  CASE 
    WHEN @test_id REGEXP '^[0-9a-f-]{36}$' THEN 'UUID_FORMAT'
    WHEN @test_id REGEXP '^[0-9]+$' THEN 'NUMERIC_FORMAT'
    ELSE 'OTHER_FORMAT'
  END as detected_format,
  @test_id as test_id;

-- Test 5: Check for items that might match the problematic UUID
SELECT '=== TEST 5: SEARCH FOR PROBLEMATIC UUID ===' as section;
SELECT id, uuid_id, title, status, seller_id, created_at
FROM items 
WHERE uuid_id = 'fd2edd76-58b6-4197-9863-254413bc13d8'
   OR title LIKE '%Bunny%'
   OR title LIKE '%bunny%';

-- Test 6: Check recent items that might be the "Bunny" item
SELECT '=== TEST 6: RECENT ITEMS (POTENTIAL BUNNY) ===' as section;
SELECT id, uuid_id, title, status, seller_id, created_at
FROM items 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY created_at DESC
LIMIT 10;

-- Test 7: Check if there are any items with "others" category that failed to sync
SELECT '=== TEST 7: ITEMS WITH POTENTIAL SYNC ISSUES ===' as section;
SELECT id, uuid_id, title, status, seller_id, created_at
FROM items 
WHERE title LIKE '%Bunny%' 
   OR title LIKE '%bunny%'
   OR (uuid_id IS NOT NULL AND uuid_id != '' AND status = 'draft')
ORDER BY created_at DESC;

-- Summary
SELECT '=== SUMMARY ===' as section;
SELECT 'If the problematic UUID is not found, the item was never successfully synced to the server' as note1;
SELECT 'The flexible resolver should now handle multiple ID formats correctly' as note2;
SELECT 'Client should check item existence before attempting bid/buy operations' as note3;
