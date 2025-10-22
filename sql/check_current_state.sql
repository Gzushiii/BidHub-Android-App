-- Check current state of database and user credits
USE defaultdb;

-- Step 1: Check if bids table exists
SELECT '=== BIDS TABLE STATUS ===' as section;
SHOW TABLES LIKE 'bids';

-- Step 2: Check user credits
SELECT '=== USER CREDITS ===' as section;
SELECT id, email, alias, credits, created_at 
FROM users 
WHERE credits > 0 
ORDER BY credits DESC;

-- Step 3: Check active items
SELECT '=== ACTIVE ITEMS ===' as section;
SELECT id, title, starting_price, current_price, status, seller_id, end_date
FROM items 
WHERE status = 'active' 
ORDER BY created_at DESC 
LIMIT 5;

-- Step 4: Check recent bids (if table exists)
SELECT '=== RECENT BIDS ===' as section;
SELECT COUNT(*) as total_bids FROM bids;

-- Step 5: Check PlaceBid procedure
SELECT '=== PLACEBID PROCEDURE ===' as section;
SELECT ROUTINE_NAME, ROUTINE_TYPE 
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';
