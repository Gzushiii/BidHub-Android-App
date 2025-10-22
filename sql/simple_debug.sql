-- Simple debug script that works in MySQL Workbench
USE defaultdb;

-- Step 1: Check users with credits
SELECT '=== USERS WITH CREDITS ===' as section;
SELECT id, email, alias, credits 
FROM users 
WHERE credits > 0 
ORDER BY credits DESC 
LIMIT 5;

-- Step 2: Check active items
SELECT '=== ACTIVE ITEMS ===' as section;
SELECT id, title, starting_price, current_price, status, seller_id 
FROM items 
WHERE status = 'active' 
ORDER BY created_at DESC 
LIMIT 5;

-- Step 3: Check bids table
SELECT '=== BIDS TABLE ===' as section;
SELECT COUNT(*) as total_bids FROM bids;

-- Step 4: Check PlaceBid procedure exists
SELECT '=== PLACEBID PROCEDURE ===' as section;
SELECT ROUTINE_NAME, ROUTINE_TYPE 
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- Step 5: Get test data for manual testing
SELECT '=== TEST DATA FOR MANUAL TESTING ===' as section;
SELECT 
    'User with highest credits:' as info,
    id as user_id,
    email as user_email,
    alias as user_alias,
    credits as user_credits
FROM users 
WHERE credits > 100 
ORDER BY credits DESC 
LIMIT 1;

SELECT 
    'Active item for testing:' as info,
    id as item_id,
    title as item_title,
    current_price as item_current_price,
    seller_id as item_seller_id
FROM items 
WHERE status = 'active' 
ORDER BY created_at DESC 
LIMIT 1;

-- Step 6: Manual test instructions
SELECT '=== MANUAL TEST INSTRUCTIONS ===' as section;
SELECT 'To test PlaceBid procedure manually, use:' as instruction;
SELECT 'CALL PlaceBid(item_id, user_id, bid_amount, user_alias);' as example;
SELECT 'Make sure bid_amount > current_price of the item' as note;
