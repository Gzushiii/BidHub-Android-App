-- Add balance_version column to users table for optimistic locking
-- This column helps prevent race conditions in credit balance updates

USE defaultdb;

DELIMITER $$

DROP PROCEDURE IF EXISTS AddBalanceVersionColumn$$

CREATE PROCEDURE AddBalanceVersionColumn()
BEGIN
    DECLARE v_col_exists INT DEFAULT 0;
    
    -- Check if balance_version column already exists
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'balance_version';
    
    IF v_col_exists = 0 THEN
        -- Add balance_version column
        ALTER TABLE users 
        ADD COLUMN balance_version INT UNSIGNED DEFAULT 0 NOT NULL 
        AFTER credits;
        
        -- Initialize all existing users with balance_version = 0
        UPDATE users SET balance_version = 0 WHERE balance_version IS NULL;
        
        SELECT '✓ balance_version column added to users table' AS '';
    ELSE
        SELECT '✓ balance_version column already exists in users table' AS '';
    END IF;
END$$

DELIMITER ;

-- Execute the procedure
CALL AddBalanceVersionColumn();

-- Drop the procedure
DROP PROCEDURE IF EXISTS AddBalanceVersionColumn;

-- Verify the column was added
SELECT 'Verifying balance_version column...' AS '';
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb'
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME = 'balance_version';

SELECT '' AS '';
SELECT '=======================================================' AS '';
SELECT 'balance_version COLUMN ADDITION COMPLETED!' AS '';
SELECT '=======================================================' AS '';

