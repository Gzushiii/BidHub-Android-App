-- Simple credit check - works with any database schema
USE defaultdb;

-- Check users table structure first
SELECT '=== USERS TABLE STRUCTURE ===' as info;
DESCRIBE users;

-- Check if credit_transactions table exists
SELECT '=== CREDIT_TRANSACTIONS TABLE STRUCTURE ===' as info;
DESCRIBE credit_transactions;

-- Simple user credit check
SELECT '=== USER CREDITS ===' as info;
SELECT 
    id,
    email,
    alias,
    credits,
    created_at
FROM users 
ORDER BY credits DESC;

-- Simple transaction check
SELECT '=== RECENT TRANSACTIONS ===' as info;
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
