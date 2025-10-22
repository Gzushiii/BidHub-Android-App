-- Check if bids table exists and verify the setup
USE defaultdb;

-- Step 1: Check if bids table exists
SELECT '=== BIDS TABLE CHECK ===' as section;
SHOW TABLES LIKE 'bids';

-- Step 2: If it exists, show its structure
DESCRIBE bids;

-- Step 3: Check if there are any bids
SELECT COUNT(*) as total_bids FROM bids;

-- Step 4: Check users with credits
SELECT '=== USERS WITH CREDITS ===' as section;
SELECT id, email, alias, credits 
FROM users 
WHERE credits > 0 
ORDER BY credits DESC;

-- Step 5: Check active items
SELECT '=== ACTIVE ITEMS ===' as section;
SELECT id, title, starting_price, current_price, status, seller_id 
FROM items 
WHERE status = 'active' 
LIMIT 3;

