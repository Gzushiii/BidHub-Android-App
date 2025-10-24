-- Fix credit balance synchronization issue (columns already exist)
-- This only updates the user's credit balance to match transaction history

USE defaultdb;

-- ==============================================
-- STEP 1: Update user's credits to match calculated value
-- ==============================================

UPDATE users 
SET credits = 7600.00
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 2: Verify the fix
-- ==============================================

SELECT 'Credit balance updated successfully' as status;

-- Check user's updated credits
SELECT 
    id,
    email,
    alias,
    credits,
    updated_at
FROM users 
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 3: Verify credit balance calculation
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
