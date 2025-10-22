-- Test the PlaceBid stored procedure
USE defaultdb;

-- Step 1: Check if we have users with credits
SELECT '=== USERS WITH CREDITS ===' as section;
SELECT id, email, alias, credits 
FROM users 
WHERE credits > 0 
ORDER BY credits DESC;

-- Step 2: Check if we have active items
SELECT '=== ACTIVE ITEMS ===' as section;
SELECT id, title, starting_price, current_price, status, seller_id 
FROM items 
WHERE status = 'active' 
LIMIT 3;

-- Step 3: Test PlaceBid procedure with actual data
-- Replace the IDs below with actual user and item IDs from the queries above
-- Example: CALL PlaceBid(1, 1, 50.00, 'testuser');

-- Step 4: Check user credits after test (if you run the procedure)
-- SELECT id, email, alias, credits FROM users WHERE id = 1;

