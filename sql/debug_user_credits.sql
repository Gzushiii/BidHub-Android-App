-- Debug script to check user credits and test PlaceBid procedure
USE defaultdb;

-- Step 1: Check all users and their credit balances
SELECT '=== ALL USERS AND CREDITS ===' as section;
SELECT id, email, alias, credits, created_at 
FROM users 
ORDER BY created_at DESC;

-- Step 2: Check if there are any items to bid on
SELECT '=== ACTIVE ITEMS ===' as section;
SELECT id, title, starting_price, current_price, status, seller_id 
FROM items 
WHERE status = 'active' 
LIMIT 5;

-- Step 3: Check if bids table exists and has data
SELECT '=== BIDS TABLE CHECK ===' as section;
SELECT COUNT(*) as total_bids FROM bids;

-- Step 4: Test PlaceBid procedure with sample data (replace with actual user ID)
-- First, let's see what user IDs we have
SELECT '=== USER IDS FOR TESTING ===' as section;
SELECT id, email, alias, credits FROM users WHERE credits > 0 LIMIT 3;

-- Step 5: Test the procedure (uncomment and modify the user_id and item_id)
-- CALL PlaceBid(1, 1, 50.00, 'testuser');

