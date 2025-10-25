-- Find actual existing items to test with
USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'FINDING ACTUAL EXISTING ITEMS FOR TESTING' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Show all items in the database
SELECT 'All items in database:' AS section;
SELECT id, title, status, seller_id, starting_price, current_price, buy_now_price, created_at
FROM items
ORDER BY created_at DESC
LIMIT 10;

SELECT '' AS '';

-- Show items in v_active_items view
SELECT 'Items in v_active_items view:' AS section;
SELECT id, title, status, seller_id, starting_price, current_price, buy_now_price
FROM v_active_items
ORDER BY created_at DESC
LIMIT 10;

SELECT '' AS '';

-- Show items that would work for bid/buy
SELECT 'Items that would work for bid/buy (status IN active, draft):' AS section;
SELECT id, title, status, seller_id, starting_price, current_price, buy_now_price
FROM items
WHERE status IN ('active', 'draft')
ORDER BY created_at DESC
LIMIT 10;

SELECT '' AS '';

-- Summary
SELECT 'SUMMARY:' AS section;
SELECT 
    (SELECT COUNT(*) FROM items) as total_items,
    (SELECT COUNT(*) FROM v_active_items) as items_in_view,
    (SELECT COUNT(*) FROM items WHERE status IN ('active', 'draft')) as transactable_items;
