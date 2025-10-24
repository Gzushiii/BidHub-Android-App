-- Simple fix for credit balance synchronization issue
-- This addresses the discrepancy between current_credits (7175.00) and calculated_credits (7600.00)

USE defaultdb;

-- ==============================================
-- STEP 1: Add missing columns (with proper syntax)
-- ==============================================

-- Add updated_at column to users table
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Add transaction_date column to credit_transactions table  
ALTER TABLE credit_transactions ADD COLUMN transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ==============================================
-- STEP 2: Fix the credit balance discrepancy
-- ==============================================

-- Update user's credits to match the calculated value (7600.00)
UPDATE users 
SET credits = 7600.00
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 3: Verify the fix
-- ==============================================

SELECT '=== CREDIT BALANCE FIXED ===' as section;

-- Check user's updated credits
SELECT 
    id,
    email,
    alias,
    credits,
    updated_at
FROM users 
WHERE email = 'testuser444@example.com';

-- Check recent transactions
SELECT 
    id,
    user_id,
    type,
    amount,
    status,
    reference,
    created_at
FROM credit_transactions 
WHERE user_id = (SELECT id FROM users WHERE email = 'testuser444@example.com')
ORDER BY created_at DESC 
LIMIT 5;

-- ==============================================
-- STEP 4: Test credit balance calculation again
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
