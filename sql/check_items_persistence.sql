-- Check Items Persistence After Logout
-- This script verifies that items are properly saved and can be retrieved

USE defaultdb;

-- 1. Check all items in the database
SELECT 
    i.id,
    i.title,
    i.description,
    i.seller_id,
    i.seller_email,
    i.starting_bid,
    i.current_bid,
    i.status,
    i.created_at,
    u.username,
    u.email as user_email,
    c.name as category_name
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
LEFT JOIN categories c ON i.category_id = c.id
ORDER BY i.created_at DESC;

-- 2. Check items by status
SELECT 
    status,
    COUNT(*) as count
FROM items 
GROUP BY status;

-- 3. Check items created in the last 24 hours
SELECT 
    i.id,
    i.title,
    i.seller_email,
    i.status,
    i.created_at,
    u.username
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
WHERE i.created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY i.created_at DESC;

-- 4. Check if items have proper seller information
SELECT 
    i.id,
    i.title,
    i.seller_id,
    i.seller_email,
    u.username,
    u.email as user_email,
    CASE 
        WHEN i.seller_id IS NOT NULL AND u.id IS NOT NULL THEN 'OK'
        WHEN i.seller_email IS NOT NULL THEN 'EMAIL_ONLY'
        ELSE 'MISSING_SELLER'
    END as seller_status
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
ORDER BY i.created_at DESC;

-- 5. Check item images
SELECT 
    i.id,
    i.title,
    ii.image_url,
    ii.display_order
FROM items i
LEFT JOIN item_images ii ON i.id = ii.item_id
ORDER BY i.id, ii.display_order;

-- 6. Summary statistics
SELECT 
    'Total Items' as metric,
    COUNT(*) as value
FROM items
UNION ALL
SELECT 
    'Active Items' as metric,
    COUNT(*) as value
FROM items 
WHERE status = 'active'
UNION ALL
SELECT 
    'Draft Items' as metric,
    COUNT(*) as value
FROM items 
WHERE status = 'draft'
UNION ALL
SELECT 
    'Items with Images' as metric,
    COUNT(DISTINCT i.id) as value
FROM items i
INNER JOIN item_images ii ON i.id = ii.item_id
UNION ALL
SELECT 
    'Items with Seller Info' as metric,
    COUNT(*) as value
FROM items 
WHERE seller_id IS NOT NULL OR seller_email IS NOT NULL;

