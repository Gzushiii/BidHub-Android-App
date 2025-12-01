-- =====================================================
-- FIX: Add bidder_alias Column to bids Table
-- =====================================================
-- This script adds the missing bidder_alias column
-- that the PlaceBid procedure requires
-- =====================================================
-- Copy and paste this ENTIRE file into Render's SQL Editor
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'FIXING BIDS TABLE - Adding bidder_alias Column' AS '';
SELECT '=======================================================' AS '';
SELECT '';

-- =====================================================
-- STEP 1: Add bidder_alias column if it doesn't exist
-- =====================================================

SELECT 'STEP 1: Adding bidder_alias column...' AS '';

DELIMITER $$

DROP PROCEDURE IF EXISTS AddBidderAliasColumn$$

CREATE PROCEDURE AddBidderAliasColumn()
BEGIN
    DECLARE v_col_exists INT DEFAULT 0;
    
    -- Check if bidder_alias column exists
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'bids'
      AND COLUMN_NAME = 'bidder_alias';
    
    IF v_col_exists = 0 THEN
        -- Add bidder_alias column after bidder_id
        ALTER TABLE bids 
        ADD COLUMN bidder_alias VARCHAR(50) NOT NULL DEFAULT '' AFTER bidder_id;
        
        -- Update existing bids with alias from users table
        UPDATE bids b
        INNER JOIN users u ON b.bidder_id = u.id
        SET b.bidder_alias = COALESCE(u.alias, u.username, u.email, '')
        WHERE b.bidder_alias = '' OR b.bidder_alias IS NULL;
        
        -- For any remaining bids without alias, set a default
        UPDATE bids
        SET bidder_alias = CONCAT('user_', bidder_id)
        WHERE bidder_alias = '' OR bidder_alias IS NULL;
        
        SELECT '✓ bidder_alias column added and populated' AS '';
    ELSE
        SELECT '✓ bidder_alias column already exists' AS '';
        
        -- Still update any NULL or empty values
        UPDATE bids b
        INNER JOIN users u ON b.bidder_id = u.id
        SET b.bidder_alias = COALESCE(u.alias, u.username, u.email, b.bidder_alias)
        WHERE b.bidder_alias IS NULL OR b.bidder_alias = '';
        
        -- For any remaining bids without alias, set a default
        UPDATE bids
        SET bidder_alias = CONCAT('user_', bidder_id)
        WHERE bidder_alias IS NULL OR bidder_alias = '';
        
        SELECT '✓ Updated existing bidder_alias values' AS '';
    END IF;
END$$

DELIMITER ;

CALL AddBidderAliasColumn();

SELECT '';

-- =====================================================
-- STEP 2: Verify the column was added
-- =====================================================

SELECT 'STEP 2: Verifying bidder_alias column...' AS '';

SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_TYPE,
    ORDINAL_POSITION
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'bids'
  AND COLUMN_NAME = 'bidder_alias';

SELECT '';

-- Check if column exists
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '✓ bidder_alias column exists'
        ELSE '✗ bidder_alias column is missing'
    END AS verification_status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'bids'
  AND COLUMN_NAME = 'bidder_alias';

SELECT '';

-- =====================================================
-- STEP 3: Check for any bids with missing alias
-- =====================================================

SELECT 'STEP 3: Checking for bids with missing alias...' AS '';

SELECT 
    COUNT(*) AS bids_without_alias
FROM bids
WHERE bidder_alias IS NULL OR bidder_alias = '';

SELECT '';

-- =====================================================
-- CLEANUP: Drop temporary procedure
-- =====================================================

DROP PROCEDURE IF EXISTS AddBidderAliasColumn;

SELECT '=======================================================' AS '';
SELECT 'FIX COMPLETED!' AS '';
SELECT '=======================================================' AS '';
SELECT '';
SELECT 'The bids table now has the bidder_alias column.' AS '';
SELECT 'The PlaceBid procedure should now work correctly.' AS '';
SELECT '';
SELECT 'Next steps:' AS '';
SELECT '1. Test placing a bid via the API' AS '';
SELECT '2. Check backend logs to confirm no more column errors' AS '';
SELECT '3. Verify bids are being placed correctly' AS '';

