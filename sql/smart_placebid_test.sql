-- Smart PlaceBid test that calculates correct bid amount
USE defaultdb;

-- Step 1: Find an item user 22 can bid on
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

-- Step 2: Get user 22 details
SELECT 'User 22 details:' as info;
SELECT id, email, alias, credits 
FROM users 
WHERE id = 22;

-- Step 3: Test PlaceBid with first available item
SET @item_id = (
    SELECT i.id 
    FROM items i 
    WHERE i.status = 'active' 
    AND i.seller_id != 22 
    ORDER BY i.id 
    LIMIT 1
);

-- Get current price and calculate bid amount
SET @current_price = (SELECT current_price FROM items WHERE id = @item_id);
SET @bid_amount = @current_price + 25.00; -- Add 25.00 to current price

SELECT CONCAT('Testing with item ID: ', @item_id) as info;
SELECT CONCAT('Current price: ', @current_price) as info;
SELECT CONCAT('Bid amount: ', @bid_amount) as info;

-- Show before state
SELECT 'Before bid:' as info;
SELECT id, email, credits FROM users WHERE id = 22;
SELECT id, title, current_price FROM items WHERE id = @item_id;

-- Place bid
CALL PlaceBid(@item_id, 22, @bid_amount, 'testalias444');

-- Show after state
SELECT 'After bid:' as info;
SELECT id, email, credits FROM users WHERE id = 22;
SELECT id, title, current_price, current_bidder_id FROM items WHERE id = @item_id;
SELECT * FROM bids WHERE item_id = @item_id ORDER BY created_at DESC;
