-- Quick status check for the specific item
USE defaultdb;

SET @item_id = '0b9cb399-7aa3-4235-a3bd-9650f114358b';

-- Check item in items table
SELECT 'Item in items table:' AS section;
SELECT id, title, status, seller_id, starting_price, current_price, buy_now_price
FROM items 
WHERE id = @item_id;

-- Check item in v_active_items view
SELECT 'Item in v_active_items view:' AS section;
SELECT id, title, status, seller_id, starting_price, current_price, buy_now_price
FROM v_active_items 
WHERE id = @item_id;

-- Check if item would match bid/buy filters
SELECT 'Would match bid/buy filters:' AS section;
SELECT 
    CASE 
        WHEN status IN ('active', 'draft') THEN 'YES - will work for bid/buy'
        ELSE 'NO - will fail with 404'
    END as result
FROM items 
WHERE id = @item_id;

-- Show all status values in database
SELECT 'All status values in database:' AS section;
SELECT DISTINCT status, COUNT(*) as count
FROM items
GROUP BY status
ORDER BY count DESC;
