-- Comprehensive analysis of user account and credit mismatching issues
-- This identifies potential mismatches between frontend and backend user data

USE defaultdb;

-- ==============================================
-- STEP 1: Check all users and their credit balances
-- ==============================================

SELECT '=== ALL USERS AND CREDITS ===' as section;

SELECT 
    id,
    email,
    alias,
    credits,
    created_at,
    updated_at
FROM users 
ORDER BY created_at DESC;

-- ==============================================
-- STEP 2: Check specific user (testuser444@example.com) in detail
-- ==============================================

SELECT '=== SPECIFIC USER ANALYSIS ===' as section;

SELECT 
    id,
    email,
    alias,
    credits,
    created_at,
    updated_at
FROM users 
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 3: Check all credit transactions for the user
-- ==============================================

SELECT '=== USER CREDIT TRANSACTIONS ===' as section;

SELECT 
    id,
    user_id,
    type,
    amount,
    status,
    reference,
    created_at,
    transaction_date
FROM credit_transactions 
WHERE user_id = (SELECT id FROM users WHERE email = 'testuser444@example.com')
ORDER BY created_at DESC;

-- ==============================================
-- STEP 4: Calculate what the user's balance should be
-- ==============================================

SELECT '=== CREDIT BALANCE CALCULATION ===' as section;

SELECT 
    u.id,
    u.email,
    u.credits as current_credits,
    COALESCE(SUM(
        CASE 
            WHEN ct.type = 'purchase' THEN ct.amount
            WHEN ct.type = 'bid' THEN -ct.amount
            WHEN ct.type = 'refund' THEN ct.amount
            ELSE 0
        END
    ), 0) as calculated_credits,
    CASE 
        WHEN u.credits = COALESCE(SUM(
            CASE 
                WHEN ct.type = 'purchase' THEN ct.amount
                WHEN ct.type = 'bid' THEN -ct.amount
                WHEN ct.type = 'refund' THEN ct.amount
                ELSE 0
            END
        ), 0) THEN 'SYNCHRONIZED'
        ELSE 'MISMATCH'
    END as sync_status
FROM users u
LEFT JOIN credit_transactions ct ON u.id = ct.user_id
WHERE u.email = 'testuser444@example.com'
GROUP BY u.id, u.email, u.credits;

-- ==============================================
-- STEP 5: Check if there are multiple users with similar emails
-- ==============================================

SELECT '=== DUPLICATE USER CHECK ===' as section;

SELECT 
    email,
    COUNT(*) as user_count,
    GROUP_CONCAT(id) as user_ids,
    GROUP_CONCAT(credits) as credit_balances
FROM users 
WHERE email LIKE '%testuser444%' OR email LIKE '%example.com%'
GROUP BY email
HAVING COUNT(*) > 1;

-- ==============================================
-- STEP 6: Check for any NULL or invalid user IDs in transactions
-- ==============================================

SELECT '=== TRANSACTION USER ID VALIDATION ===' as section;

SELECT 
    ct.id as transaction_id,
    ct.user_id,
    ct.type,
    ct.amount,
    u.email,
    u.credits,
    CASE 
        WHEN u.id IS NULL THEN 'INVALID_USER_ID'
        WHEN u.email IS NULL THEN 'USER_EMAIL_NULL'
        ELSE 'VALID'
    END as validation_status
FROM credit_transactions ct
LEFT JOIN users u ON ct.user_id = u.id
WHERE ct.user_id = (SELECT id FROM users WHERE email = 'testuser444@example.com')
ORDER BY ct.created_at DESC
LIMIT 10;

-- ==============================================
-- STEP 7: Check for any orphaned transactions
-- ==============================================

SELECT '=== ORPHANED TRANSACTIONS CHECK ===' as section;

SELECT 
    ct.id,
    ct.user_id,
    ct.type,
    ct.amount,
    ct.created_at
FROM credit_transactions ct
LEFT JOIN users u ON ct.user_id = u.id
WHERE u.id IS NULL
LIMIT 10;

-- ==============================================
-- STEP 8: Check PlaceBid procedure status
-- ==============================================

SELECT '=== PLACEBID PROCEDURE STATUS ===' as section;

SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE,
    CREATED,
    LAST_ALTERED
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- ==============================================
-- STEP 9: Check for any active items that might be causing issues
-- ==============================================

SELECT '=== ACTIVE ITEMS CHECK ===' as section;

SELECT 
    id,
    title,
    starting_price,
    current_price,
    status,
    seller_id,
    end_date
FROM items 
WHERE status = 'active'
ORDER BY created_at DESC
LIMIT 5;

-- ==============================================
-- STEP 10: Check for any existing bids
-- ==============================================

SELECT '=== EXISTING BIDS CHECK ===' as section;

SELECT 
    b.id,
    b.item_id,
    b.bidder_id,
    b.amount,
    b.status,
    b.created_at,
    u.email as bidder_email
FROM bids b
LEFT JOIN users u ON b.bidder_id = u.id
ORDER BY b.created_at DESC
LIMIT 10;
