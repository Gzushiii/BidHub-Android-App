-- Quick debug to check the current state
USE defaultdb;

-- Check if PlaceBid procedure exists
SELECT 'PlaceBid procedure exists:' as info;
SELECT COUNT(*) as count 
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- Check user with 2700 credits
SELECT 'User with 2700 credits:' as info;
SELECT id, email, alias, credits 
FROM users 
WHERE credits >= 2700;

-- Check item 2 (bunny)
SELECT 'Item 2 details:' as info;
SELECT id, title, starting_price, current_price, status, seller_id 
FROM items 
WHERE id = 2;

-- Check if user 22 is the seller of item 2
SELECT 'Seller check:' as info;
SELECT 
    CASE 
        WHEN 22 = (SELECT seller_id FROM items WHERE id = 2) 
        THEN 'ERROR: User 22 is the seller of item 2'
        ELSE 'OK: User 22 is not the seller of item 2'
    END as seller_check;
