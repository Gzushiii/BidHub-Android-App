-- SAFE DATABASE QUERIES
-- These queries only use basic columns that are likely to exist

USE defaultdb;

-- 1. Check users (basic columns only)
SELECT '=== USERS CHECK ===' as section;
SELECT 
    id,
    username,
    email,
    credits,
    created_at
FROM users 
ORDER BY created_at DESC;

-- 2. Check total user count
SELECT '=== USER COUNT ===' as section;
SELECT COUNT(*) as total_users FROM users;

-- 3. Check items (basic columns only)
SELECT '=== ITEMS CHECK ===' as section;
SELECT 
    id,
    title,
    description,
    seller_id,
    starting_bid,
    current_bid,
    status,
    created_at
FROM items 
ORDER BY created_at DESC;

-- 4. Check total item count by status
SELECT '=== ITEM STATUS COUNT ===' as section;
SELECT 
    status,
    COUNT(*) as count
FROM items 
GROUP BY status;

-- 5. Check bids (basic columns only)
SELECT '=== BIDS CHECK ===' as section;
SELECT 
    id,
    item_id,
    bidder_id,
    amount,
    created_at
FROM bids 
ORDER BY created_at DESC;

-- 6. Check total bid count
SELECT '=== BID COUNT ===' as section;
SELECT COUNT(*) as total_bids FROM bids;

-- 7. Check recent activity (last 24 hours) - basic columns only
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
