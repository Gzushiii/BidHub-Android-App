-- Database Functionality Check Script
-- This script checks if item posting and bidding functionalities are working properly

USE defaultdb;

-- 1. Check if users exist in the database
SELECT '=== USERS CHECK ===' as section;
SELECT 
    id,
    username,
    email,
    alias,
    credits,
    created_at,
    last_login
FROM users 
ORDER BY created_at DESC;

-- 2. Check total user count
SELECT '=== USER COUNT ===' as section;
SELECT COUNT(*) as total_users FROM users;

-- 3. Check if items exist in the database
SELECT '=== ITEMS CHECK ===' as section;
SELECT 
    id,
    title,
    description,
    seller_id,
    starting_bid,
    current_bid,
    status,
    created_at,
    end_date,
    item_condition
FROM items 
ORDER BY created_at DESC;

-- 4. Check total item count by status
SELECT '=== ITEM STATUS COUNT ===' as section;
SELECT 
    status,
    COUNT(*) as count
FROM items 
GROUP BY status;

-- 5. Check if bids exist in the database
SELECT '=== BIDS CHECK ===' as section;
SELECT 
    id,
    item_id,
    bidder_id,
    bidder_alias,
    amount,
    created_at,
    status
FROM bids 
ORDER BY created_at DESC;

-- 6. Check total bid count
SELECT '=== BID COUNT ===' as section;
SELECT COUNT(*) as total_bids FROM bids;

-- 7. Check recent activity (last 24 hours)
SELECT '=== RECENT ACTIVITY (Last 24 hours) ===' as section;
SELECT 
    'Item' as type,
    id as record_id,
    title as description,
    seller_id as user_id,
    created_at
FROM items 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
UNION ALL
SELECT 
    'Bid' as type,
    id as record_id,
    CONCAT('Bid of ', amount, ' on item ', item_id) as description,
    bidder_id as user_id,
    created_at
FROM bids 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY created_at DESC;

-- 8. Check user credit balances
SELECT '=== USER CREDIT BALANCES ===' as section;
SELECT 
    username,
    email,
    credits,
    CASE 
        WHEN credits > 100 THEN 'High'
        WHEN credits > 50 THEN 'Medium'
        WHEN credits > 0 THEN 'Low'
        ELSE 'Zero'
    END as credit_level
FROM users 
ORDER BY credits DESC;

-- 9. Check items with bids
SELECT '=== ITEMS WITH BIDS ===' as section;
SELECT 
    i.id,
    i.title,
    i.current_bid,
    i.status,
    COUNT(b.id) as bid_count,
    MAX(b.amount) as highest_bid
FROM items i
LEFT JOIN bids b ON i.id = b.item_id
GROUP BY i.id, i.title, i.current_bid, i.status
HAVING bid_count > 0
ORDER BY bid_count DESC;

-- 10. Check for any data integrity issues
SELECT '=== DATA INTEGRITY CHECK ===' as section;
SELECT 
    'Items with invalid seller_id' as issue,
    COUNT(*) as count
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
WHERE u.id IS NULL AND i.seller_id IS NOT NULL
UNION ALL
SELECT 
    'Bids with invalid item_id' as issue,
    COUNT(*) as count
FROM bids b
LEFT JOIN items i ON b.item_id = i.id
WHERE i.id IS NULL
UNION ALL
SELECT 
    'Bids with invalid bidder_id' as issue,
    COUNT(*) as count
FROM bids b
LEFT JOIN users u ON b.bidder_id = u.id
WHERE u.id IS NULL;
