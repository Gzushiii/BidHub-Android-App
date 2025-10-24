-- Check if credits are being recorded in user accounts (FIXED VERSION)
-- This script provides comprehensive queries to verify credit system functionality
-- Fixed to work with actual database schema

USE defaultdb;

-- ==============================================
-- QUERY 1: Check all users and their credit balances
-- ==============================================
SELECT '=== ALL USERS AND CREDIT BALANCES ===' as section;
SELECT 
    id as user_id,
    email,
    alias,
    credits,
    created_at
FROM users 
ORDER BY created_at DESC;

-- ==============================================
-- QUERY 2: Check recent credit transactions
-- ==============================================
SELECT '=== RECENT CREDIT TRANSACTIONS ===' as section;
SELECT 
    ct.id as transaction_id,
    u.email as user_email,
    u.alias as user_alias,
    ct.type,
    ct.amount,
    ct.status,
    ct.reference,
    ct.transaction_date
FROM credit_transactions ct
JOIN users u ON ct.user_id = u.id
ORDER BY ct.transaction_date DESC
LIMIT 20;

-- ==============================================
-- QUERY 3: Check credit transactions by type
-- ==============================================
SELECT '=== CREDIT TRANSACTIONS BY TYPE ===' as section;
SELECT 
    type,
    COUNT(*) as transaction_count,
    SUM(amount) as total_amount,
    AVG(amount) as average_amount
FROM credit_transactions
GROUP BY type
ORDER BY transaction_count DESC;

-- ==============================================
-- QUERY 4: Check users with recent credit activity
-- ==============================================
SELECT '=== USERS WITH RECENT CREDIT ACTIVITY ===' as section;
SELECT 
    u.id as user_id,
    u.email,
    u.alias,
    u.credits as current_balance,
    COUNT(ct.id) as total_transactions,
    MAX(ct.transaction_date) as last_transaction_date,
    SUM(CASE WHEN ct.type = 'purchase' THEN ct.amount ELSE 0 END) as total_purchased,
    SUM(CASE WHEN ct.type = 'bid' THEN ct.amount ELSE 0 END) as total_bid
FROM users u
LEFT JOIN credit_transactions ct ON u.id = ct.user_id
GROUP BY u.id, u.email, u.alias, u.credits
HAVING total_transactions > 0
ORDER BY last_transaction_date DESC;

-- ==============================================
-- QUERY 5: Check for specific user (replace email)
-- ==============================================
SET @target_user_email = 'testuser444@example.com'; -- Change this to the user you want to check

SELECT '=== SPECIFIC USER CREDIT DETAILS ===' as section;
SELECT 
    id as user_id,
    email,
    alias,
    credits as current_balance,
    created_at
FROM users 
WHERE email = @target_user_email;

SELECT '=== SPECIFIC USER TRANSACTION HISTORY ===' as section;
SELECT 
    ct.id as transaction_id,
    ct.type,
    ct.amount,
    ct.status,
    ct.reference,
    ct.transaction_date
FROM credit_transactions ct
JOIN users u ON ct.user_id = u.id
WHERE u.email = @target_user_email
ORDER BY ct.transaction_date DESC
LIMIT 10;

-- ==============================================
-- QUERY 6: Check credit balance consistency
-- ==============================================
SELECT '=== CREDIT BALANCE CONSISTENCY CHECK ===' as section;
SELECT 
    u.id as user_id,
    u.email,
    u.credits as stored_balance,
    COALESCE(purchased.total, 0) as total_purchased,
    COALESCE(bid.total, 0) as total_bid,
    (COALESCE(purchased.total, 0) - COALESCE(bid.total, 0)) as calculated_balance,
    (u.credits - (COALESCE(purchased.total, 0) - COALESCE(bid.total, 0))) as balance_difference
FROM users u
LEFT JOIN (
    SELECT user_id, SUM(amount) as total
    FROM credit_transactions 
    WHERE type = 'purchase' AND status = 'completed'
    GROUP BY user_id
) purchased ON u.id = purchased.user_id
LEFT JOIN (
    SELECT user_id, SUM(amount) as total
    FROM credit_transactions 
    WHERE type = 'bid' AND status = 'completed'
    GROUP BY user_id
) bid ON u.id = bid.user_id
WHERE u.credits > 0 OR purchased.total > 0 OR bid.total > 0
ORDER BY balance_difference DESC;

-- ==============================================
-- QUERY 7: Check for failed credit transactions
-- ==============================================
SELECT '=== FAILED CREDIT TRANSACTIONS ===' as section;
SELECT 
    ct.id as transaction_id,
    u.email as user_email,
    ct.type,
    ct.amount,
    ct.status,
    ct.reference,
    ct.transaction_date
FROM credit_transactions ct
JOIN users u ON ct.user_id = u.id
WHERE ct.status != 'completed'
ORDER BY ct.transaction_date DESC
LIMIT 10;

-- ==============================================
-- QUERY 8: Summary statistics
-- ==============================================
SELECT '=== CREDIT SYSTEM SUMMARY ===' as section;
SELECT 
    'Total Users' as metric,
    COUNT(*) as value
FROM users
UNION ALL
SELECT 
    'Users with Credits > 0' as metric,
    COUNT(*) as value
FROM users
WHERE credits > 0
UNION ALL
SELECT 
    'Total Credit Transactions' as metric,
    COUNT(*) as value
FROM credit_transactions
UNION ALL
SELECT 
    'Total Credits Purchased' as metric,
    COALESCE(SUM(amount), 0) as value
FROM credit_transactions
WHERE type = 'purchase' AND status = 'completed'
UNION ALL
SELECT 
    'Total Credits Used in Bids' as metric,
    COALESCE(SUM(amount), 0) as value
FROM credit_transactions
WHERE type = 'bid' AND status = 'completed'
UNION ALL
SELECT 
    'Average User Credit Balance' as metric,
    ROUND(AVG(credits), 2) as value
FROM users
WHERE credits > 0;
