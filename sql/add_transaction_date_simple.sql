-- Simple version to add transaction_date column
USE defaultdb;

-- Add the transaction_date column
ALTER TABLE credit_transactions ADD COLUMN transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Verify the column was added
DESCRIBE credit_transactions;
