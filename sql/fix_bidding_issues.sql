-- Fix bidding issues
USE defaultdb;

-- Step 1: Check current state
SELECT '=== CURRENT STATE ===' as section;

-- Check if PlaceBid procedure exists
SELECT 'PlaceBid procedure exists:' as info;
SELECT COUNT(*) as count 
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- Check user 22 details
SELECT 'User 22 details:' as info;
SELECT id, email, alias, credits 
FROM users 
WHERE id = 22;

-- Check item 2 details
SELECT 'Item 2 details:' as info;
SELECT id, title, starting_price, current_price, status, seller_id 
FROM items 
WHERE id = 2;

-- Check if user 22 is the seller of item 2
SELECT 'Seller check:' as info;
SELECT 
    CASE 
        WHEN 22 = (SELECT seller_id FROM items WHERE id = 2) 
        THEN 'ERROR: User 22 is the seller of item 2 - cannot bid on own item'
        ELSE 'OK: User 22 is not the seller of item 2'
    END as seller_check;

-- Step 2: Create a test item that user 22 can bid on
SELECT '=== CREATING TEST ITEM ===' as section;

-- Create a test item with a different seller
INSERT INTO items (
    title, 
    description, 
    starting_price, 
    current_price, 
    buy_now_price, 
    category_id, 
    seller_id, 
    item_condition, 
    status, 
    end_date
) VALUES (
    'Test Item for Bidding',
    'This is a test item for bidding',
    50.00,
    50.00,
    200.00,
    1,
    1, -- Different seller (user 1)
    'good',
    'active',
    DATE_ADD(NOW(), INTERVAL 7 DAY)
);

-- Get the new item ID
SET @new_item_id = LAST_INSERT_ID();
SELECT CONCAT('Created test item with ID: ', @new_item_id) as info;

-- Step 3: Test PlaceBid with the new item
SELECT '=== TESTING PLACEBID ===' as section;

-- Show before state
SELECT 'Before bid:' as info;
SELECT id, email, credits FROM users WHERE id = 22;
SELECT id, title, current_price FROM items WHERE id = @new_item_id;

-- Place bid
CALL PlaceBid(@new_item_id, 22, 75.00, 'testalias444');

-- Show after state
SELECT 'After bid:' as info;
SELECT id, email, credits FROM users WHERE id = 22;
SELECT id, title, current_price, current_bidder_id FROM items WHERE id = @new_item_id;
SELECT * FROM bids WHERE item_id = @new_item_id ORDER BY created_at DESC;

-- Step 4: Clean up test item
SELECT '=== CLEANUP ===' as section;
DELETE FROM bids WHERE item_id = @new_item_id;
DELETE FROM items WHERE id = @new_item_id;
SELECT 'Test item cleaned up' as info;
