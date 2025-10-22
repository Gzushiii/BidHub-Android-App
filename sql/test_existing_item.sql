-- Test PlaceBid with existing item (no INSERT needed)
USE defaultdb;

-- Step 1: Check if PlaceBid procedure exists
SELECT '=== PLACEBID PROCEDURE CHECK ===' as section;
SELECT ROUTINE_NAME, ROUTINE_TYPE 
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- Step 2: Find an existing item that user 22 can bid on
SELECT '=== FINDING BIDDABLE ITEM ===' as section;
SELECT 
    i.id, 
    i.title, 
    i.current_price, 
    i.seller_id,
    u.email as seller_email,
    CASE 
        WHEN i.seller_id = 22 THEN 'Cannot bid - user is seller'
        ELSE 'Can bid on this item'
    END as bid_status
FROM items i
JOIN users u ON i.seller_id = u.id
WHERE i.status = 'active' 
AND i.seller_id != 22
ORDER BY i.id
LIMIT 3;

-- Step 3: Get user 22 details
SELECT '=== USER 22 DETAILS ===' as section;
SELECT id, email, alias, credits 
FROM users 
WHERE id = 22;

-- Step 4: Test PlaceBid with first available item
-- Get the first item that user 22 can bid on
SET @test_item_id = (
    SELECT i.id 
    FROM items i 
    WHERE i.status = 'active' 
    AND i.seller_id != 22 
    ORDER BY i.id 
    LIMIT 1
);

SELECT CONCAT('Testing with item ID: ', @test_item_id) as test_info;

-- Get item details
SELECT 
    id, 
    title, 
    current_price, 
    seller_id 
FROM items 
WHERE id = @test_item_id;

-- Step 5: Test PlaceBid procedure
SELECT '=== TESTING PLACEBID ===' as section;

-- Show before state
SELECT 'Before bid:' as info;
SELECT id, email, credits FROM users WHERE id = 22;

-- Place bid (75.00 should be higher than current price)
CALL PlaceBid(@test_item_id, 22, 75.00, 'testalias444');

-- Show after state
SELECT 'After bid:' as info;
SELECT id, email, credits FROM users WHERE id = 22;
SELECT id, title, current_price, current_bidder_id FROM items WHERE id = @test_item_id;
SELECT * FROM bids WHERE item_id = @test_item_id ORDER BY created_at DESC LIMIT 3;
