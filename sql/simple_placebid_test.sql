-- Simple PlaceBid test using existing data
USE defaultdb;

-- Step 1: Check PlaceBid procedure exists
SELECT 'PlaceBid procedure exists:' as info;
SELECT COUNT(*) as count 
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- Step 2: Find an item user 22 can bid on
SELECT 'Available items for bidding:' as info;
SELECT 
    i.id, 
    i.title, 
    i.current_price, 
    i.seller_id,
    u.email as seller_email
FROM items i
JOIN users u ON i.seller_id = u.id
WHERE i.status = 'active' 
AND i.seller_id != 22
ORDER BY i.id
LIMIT 3;

-- Step 3: Get user 22 details
SELECT 'User 22 details:' as info;
SELECT id, email, alias, credits 
FROM users 
WHERE id = 22;

-- Step 4: Test PlaceBid with first available item
SET @item_id = (
    SELECT i.id 
    FROM items i 
    WHERE i.status = 'active' 
    AND i.seller_id != 22 
    ORDER BY i.id 
    LIMIT 1
);

SELECT CONCAT('Testing with item ID: ', @item_id) as info;

-- Show before state
SELECT 'Before bid:' as info;
SELECT id, email, credits FROM users WHERE id = 22;
SELECT id, title, current_price FROM items WHERE id = @item_id;

-- Place bid (must be higher than current price of 225.00)
CALL PlaceBid(@item_id, 22, 250.00, 'testalias444');

-- Show after state
SELECT 'After bid:' as info;
SELECT id, email, credits FROM users WHERE id = 22;
SELECT id, title, current_price, current_bidder_id FROM items WHERE id = @item_id;
SELECT * FROM bids WHERE item_id = @item_id ORDER BY created_at DESC;
