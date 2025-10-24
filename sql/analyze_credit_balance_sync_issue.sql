-- Analyze credit balance synchronization issue
-- User shows ₱7,175.00 in Profile but gets "Insufficient credits" error

USE defaultdb;

-- ==============================================
-- STEP 1: Check user's actual credit balance in database
-- ==============================================

SELECT '=== USER CREDIT BALANCE ANALYSIS ===' as section;

-- Check the specific user's credits
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
-- STEP 2: Check recent credit transactions
-- ==============================================

SELECT '=== RECENT CREDIT TRANSACTIONS ===' as section;

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
ORDER BY created_at DESC 
LIMIT 10;

-- ==============================================
-- STEP 3: Check if there are any missing columns
-- ==============================================

SELECT '=== CREDIT_TRANSACTIONS TABLE STRUCTURE ===' as section;

DESCRIBE credit_transactions;

-- ==============================================
-- STEP 4: Check for data type mismatches
-- ==============================================

SELECT '=== DATA TYPE ANALYSIS ===' as section;

-- Check if credits column has correct data type
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'users' 
AND COLUMN_NAME = 'credits';

-- ==============================================
-- STEP 5: Check for any NULL or invalid credit values
-- ==============================================

SELECT '=== CREDIT VALUE ANALYSIS ===' as section;

SELECT 
    id,
    email,
    credits,
    CASE 
        WHEN credits IS NULL THEN 'NULL'
        WHEN credits < 0 THEN 'NEGATIVE'
        WHEN credits = 0 THEN 'ZERO'
        ELSE 'POSITIVE'
    END as credit_status
FROM users 
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 6: Check if PlaceBid procedure is working correctly
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
-- STEP 7: Test credit balance calculation
-- ==============================================

SELECT '=== CREDIT BALANCE CALCULATION TEST ===' as section;

-- Calculate what the user's balance should be based on transactions
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
    ), 0) as calculated_credits
FROM users u
LEFT JOIN credit_transactions ct ON u.id = ct.user_id
WHERE u.email = 'testuser444@example.com'
GROUP BY u.id, u.email, u.credits;
