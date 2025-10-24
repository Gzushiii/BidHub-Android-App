-- Add transaction_date column to credit_transactions table
USE defaultdb;

-- Check if the column already exists (optional, for safety)
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN 'Column transaction_date already exists'
        ELSE 'Column transaction_date does not exist - will add it'
    END as column_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb'
AND TABLE_NAME = 'credit_transactions'
AND COLUMN_NAME = 'transaction_date';

-- Add the transaction_date column
-- This will set the default value to the current timestamp upon insertion
ALTER TABLE credit_transactions ADD COLUMN transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Verify the column was added
DESCRIBE credit_transactions;
