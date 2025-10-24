-- Step-by-step fix for credit balance synchronization
-- Run each section separately in MySQL Workbench

-- STEP 1: Use database
USE defaultdb;

-- STEP 2: Add updated_at column to users table
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- STEP 3: Add transaction_date column to credit_transactions table
ALTER TABLE credit_transactions ADD COLUMN transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- STEP 4: Update user's credits to match calculated value
UPDATE users 
SET credits = 7600.00
WHERE email = 'testuser444@example.com';

-- STEP 5: Verify the fix
SELECT 'Credit balance updated successfully' as status;
SELECT id, email, credits FROM users WHERE email = 'testuser444@example.com';
