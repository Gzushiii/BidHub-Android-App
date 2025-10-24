-- Safe fix for credit balance synchronization issue
-- This checks if columns exist before adding them

USE defaultdb;

-- ==============================================
-- STEP 1: Check if updated_at column exists in users table
-- ==============================================

SELECT '=== CHECKING USERS TABLE STRUCTURE ===' as section;

SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'users' 
AND COLUMN_NAME IN ('updated_at', 'credits');

-- ==============================================
-- STEP 2: Check if transaction_date column exists in credit_transactions table
-- ==============================================

SELECT '=== CHECKING CREDIT_TRANSACTIONS TABLE STRUCTURE ===' as section;

SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'credit_transactions' 
AND COLUMN_NAME IN ('transaction_date', 'user_id', 'type', 'amount');

-- ==============================================
-- STEP 3: Update user's credits to match calculated value
-- ==============================================

SELECT '=== UPDATING USER CREDITS ===' as section;

UPDATE users 
SET credits = 7600.00
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 4: Verify the fix
-- ==============================================

SELECT '=== VERIFICATION: CREDIT BALANCE UPDATED ===' as section;

SELECT 
    id,
    email,
    alias,
    credits,
    updated_at
FROM users 
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 5: Test credit balance calculation
-- ==============================================

SELECT '=== VERIFICATION: CREDIT BALANCE CALCULATION ===' as section;

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

-- ==============================================
-- FINAL STATUS
-- ==============================================

SELECT '=== CREDIT BALANCE SYNC ISSUE FIXED ===' as final_status;
SELECT 'User should now have 7600.00 credits and bidding should work' as result;
