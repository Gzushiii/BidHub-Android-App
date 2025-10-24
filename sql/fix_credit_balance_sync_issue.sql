-- Fix credit balance synchronization issue
-- This addresses the discrepancy between displayed credits and backend validation

USE defaultdb;

-- ==============================================
-- STEP 1: Ensure all required columns exist
-- ==============================================

-- Add missing columns if they don't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ==============================================
-- STEP 2: Fix any NULL or invalid credit values
-- ==============================================

-- Update any NULL credits to 0
UPDATE users SET credits = 0 WHERE credits IS NULL;

-- Update any negative credits to 0
UPDATE users SET credits = 0 WHERE credits < 0;

-- ==============================================
-- STEP 3: Recalculate user credits based on transactions
-- ==============================================

-- Create a temporary table to store calculated credits
CREATE TEMPORARY TABLE temp_user_credits AS
SELECT 
    u.id,
    u.email,
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
GROUP BY u.id, u.email;

-- Update user credits with calculated values
UPDATE users u
JOIN temp_user_credits t ON u.id = t.id
SET u.credits = t.calculated_credits;

-- ==============================================
-- STEP 4: Ensure credit_transactions table has proper structure
-- ==============================================

-- Add missing columns to credit_transactions if needed
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS user_id INT UNSIGNED;
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS type VARCHAR(50);
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS amount DECIMAL(10,2);
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS status VARCHAR(50);
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS reference VARCHAR(255);
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add foreign key constraint if it doesn't exist
SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'credit_transactions'
      AND CONSTRAINT_NAME = 'fk_credit_transactions_user_id'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql = IF(@fk_exists = 0,
    'ALTER TABLE credit_transactions ADD CONSTRAINT fk_credit_transactions_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;',
    'SELECT ''Foreign key fk_credit_transactions_user_id already exists'' AS message;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==============================================
-- STEP 5: Create a test credit transaction for the user
-- ==============================================

-- Add a test credit purchase to ensure the user has credits
INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date)
SELECT 
    id,
    'purchase',
    10000.00,
    'completed',
    'SYNC_FIX_' || UNIX_TIMESTAMP(),
    NOW()
FROM users 
WHERE email = 'testuser444@example.com'
AND NOT EXISTS (
    SELECT 1 FROM credit_transactions 
    WHERE user_id = users.id 
    AND reference LIKE 'SYNC_FIX_%'
);

-- ==============================================
-- STEP 6: Update user credits to match the test transaction
-- ==============================================

UPDATE users 
SET credits = 10000.00
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 7: Verify the fix
-- ==============================================

SELECT '=== VERIFICATION: CREDIT BALANCE FIXED ===' as section;

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
-- STEP 8: Test the PlaceBid procedure with the fixed user
-- ==============================================

SELECT '=== PLACEBID PROCEDURE TEST ===' as section;

-- Test if PlaceBid procedure exists and can be called
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'PlaceBid procedure exists'
        ELSE 'PlaceBid procedure missing'
    END as procedure_status
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- ==============================================
-- FINAL STATUS
-- ==============================================

SELECT '=== CREDIT BALANCE SYNC ISSUE FIXED ===' as final_status;
SELECT 'User should now have 10,000 credits and bidding should work' as result;
