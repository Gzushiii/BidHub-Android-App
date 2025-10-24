-- Quick credit check queries
-- Use these for fast verification of credit recording

USE defaultdb;

-- Quick check: All users with their credit balances
SELECT '=== QUICK USER CREDIT CHECK ===' as info;
SELECT 
    id,
    email,
    alias,
    credits,
    updated_at
FROM users 
ORDER BY credits DESC;

-- Quick check: Recent credit transactions
SELECT '=== RECENT CREDIT TRANSACTIONS ===' as info;
SELECT 
    u.email,
    ct.type,
    ct.amount,
    ct.status,
    ct.transaction_date
FROM credit_transactions ct
JOIN users u ON ct.user_id = u.id
ORDER BY ct.transaction_date DESC
LIMIT 10;

-- Quick check: Check specific user (change email as needed)
SELECT '=== SPECIFIC USER CHECK ===' as info;
SELECT 
    u.email,
    u.credits as current_balance,
    COUNT(ct.id) as transaction_count,
    MAX(ct.transaction_date) as last_transaction
FROM users u
LEFT JOIN credit_transactions ct ON u.id = ct.user_id
WHERE u.email = 'testuser444@example.com' -- Change this email
GROUP BY u.id, u.email, u.credits;
