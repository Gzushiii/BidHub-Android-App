-- Final fix: Update user credits to match transaction history
-- This resolves the ₱7,175.00 vs ₱7,600.00 discrepancy

USE defaultdb;

-- ==============================================
-- STEP 1: Update user's credits to match calculated value
-- ==============================================

UPDATE users 
SET credits = 7600.00
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 2: Verify the update
-- ==============================================

SELECT '=== USER CREDITS UPDATED ===' as section;

SELECT 
    id,
    email,
    alias,
    credits,
    updated_at
FROM users 
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 3: Verify credit balance calculation matches
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
-- FINAL STATUS
-- ==============================================

SELECT '=== CREDIT BALANCE SYNC ISSUE RESOLVED ===' as final_status;
SELECT 'User now has 7600.00 credits - bidding should work!' as result;
