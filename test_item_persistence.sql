-- Test Item Persistence After Logout
-- This script tests that items are properly saved and can be retrieved

USE defaultdb;

-- 1. Check current items in database
SELECT 
    'Current Items in Database' as test_name,
    COUNT(*) as count
FROM items;

-- 2. Check items by status
SELECT 
    'Items by Status' as test_name,
    status,
    COUNT(*) as count
FROM items 
GROUP BY status;

-- 3. Check items with seller information
SELECT 
    'Items with Seller Info' as test_name,
    CASE 
        WHEN seller_id IS NOT NULL AND seller_email IS NOT NULL THEN 'Both ID and Email'
        WHEN seller_id IS NOT NULL THEN 'ID Only'
        WHEN seller_email IS NOT NULL THEN 'Email Only'
        ELSE 'No Seller Info'
    END as seller_info_type,
    COUNT(*) as count
FROM items
GROUP BY seller_info_type;

-- 4. Check recent items (last 24 hours)
SELECT 
    'Recent Items (24h)' as test_name,
    COUNT(*) as count
FROM items 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR);

-- 5. Check items with images
SELECT 
    'Items with Images' as test_name,
    COUNT(DISTINCT i.id) as count
FROM items i
LEFT JOIN item_images ii ON i.id = ii.item_id
WHERE ii.image_url IS NOT NULL;

-- 6. Test the active items view
SELECT 
    'Active Items View' as test_name,
    COUNT(*) as count
FROM v_active_items;

-- 7. Test filtering by seller email
SELECT 
    'Items by Seller Email' as test_name,
    seller_email,
    COUNT(*) as count
FROM items 
WHERE seller_email IS NOT NULL
GROUP BY seller_email
ORDER BY count DESC;

-- 8. Check data integrity
SELECT 
    'Data Integrity Check' as test_name,
    CASE 
        WHEN COUNT(*) = COUNT(DISTINCT id) THEN 'PASS - No duplicate IDs'
        ELSE 'FAIL - Duplicate IDs found'
    END as id_check,
    CASE 
        WHEN COUNT(*) = COUNT(CASE WHEN title IS NOT NULL AND title != '' THEN 1 END) THEN 'PASS - All items have titles'
        ELSE 'FAIL - Some items missing titles'
    END as title_check,
    CASE 
        WHEN COUNT(*) = COUNT(CASE WHEN seller_id IS NOT NULL OR seller_email IS NOT NULL THEN 1 END) THEN 'PASS - All items have seller info'
        ELSE 'FAIL - Some items missing seller info'
    END as seller_check
FROM items;

-- 9. Sample data for verification
SELECT 
    'Sample Items' as test_name,
    id,
    title,
    seller_email,
    status,
    created_at
FROM items 
ORDER BY created_at DESC 
LIMIT 5;

