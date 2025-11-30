-- =====================================================
-- FIX: Add Missing Columns to items Table
-- =====================================================
-- This script adds the missing current_price and current_bid
-- columns that the backend API expects when creating items
-- =====================================================
-- Copy and paste this ENTIRE file into Render's SQL Editor
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'FIXING ITEMS TABLE - Adding Missing Columns' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 1: Add current_price column if it doesn't exist
-- =====================================================

SELECT 'STEP 1: Adding current_price column...' AS '';

DELIMITER $$

DROP PROCEDURE IF EXISTS AddCurrentPriceColumn$$

CREATE PROCEDURE AddCurrentPriceColumn()
BEGIN
    DECLARE v_col_exists INT DEFAULT 0;
    
    -- Check if current_price column exists
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'current_price';
    
    IF v_col_exists = 0 THEN
        -- Add current_price column after starting_price or starting_bid
        SET @sql = 'ALTER TABLE items ADD COLUMN current_price DECIMAL(10,2) DEFAULT 0.00 NOT NULL';
        
        -- Try to add after starting_price first, if that fails, try after starting_bid
        SET @col_position = (
            SELECT COLUMN_NAME 
            FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = 'defaultdb' 
              AND TABLE_NAME = 'items'
              AND COLUMN_NAME IN ('starting_price', 'starting_bid', 'reserve_price')
            ORDER BY ORDINAL_POSITION DESC
            LIMIT 1
        );
        
        IF @col_position IS NOT NULL THEN
            SET @sql = CONCAT('ALTER TABLE items ADD COLUMN current_price DECIMAL(10,2) DEFAULT 0.00 NOT NULL AFTER ', @col_position);
        END IF;
        
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        SELECT '✓ current_price column added' AS '';
    ELSE
        SELECT '✓ current_price column already exists' AS '';
    END IF;
END$$

DELIMITER ;

CALL AddCurrentPriceColumn();

SELECT '' AS '';

-- =====================================================
-- STEP 2: Add current_bid column if it doesn't exist
-- =====================================================

SELECT 'STEP 2: Adding current_bid column...' AS '';

DELIMITER $$

DROP PROCEDURE IF EXISTS AddCurrentBidColumn$$

CREATE PROCEDURE AddCurrentBidColumn()
BEGIN
    DECLARE v_col_exists INT DEFAULT 0;
    
    -- Check if current_bid column exists
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'current_bid';
    
    IF v_col_exists = 0 THEN
        -- Add current_bid column after current_price if it exists, otherwise after starting_price/starting_bid
        SET @sql = 'ALTER TABLE items ADD COLUMN current_bid DECIMAL(10,2) DEFAULT 0.00 NOT NULL';
        
        -- Try to add after current_price if it exists
        SET @col_position = (
            SELECT COLUMN_NAME 
            FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = 'defaultdb' 
              AND TABLE_NAME = 'items'
              AND COLUMN_NAME = 'current_price'
            LIMIT 1
        );
        
        IF @col_position IS NOT NULL THEN
            SET @sql = CONCAT('ALTER TABLE items ADD COLUMN current_bid DECIMAL(10,2) DEFAULT 0.00 NOT NULL AFTER current_price');
        ELSE
            -- Fallback: add after starting_price or starting_bid
            SET @col_position = (
                SELECT COLUMN_NAME 
                FROM information_schema.COLUMNS 
                WHERE TABLE_SCHEMA = 'defaultdb' 
                  AND TABLE_NAME = 'items'
                  AND COLUMN_NAME IN ('starting_price', 'starting_bid', 'reserve_price')
                ORDER BY ORDINAL_POSITION DESC
                LIMIT 1
            );
            
            IF @col_position IS NOT NULL THEN
                SET @sql = CONCAT('ALTER TABLE items ADD COLUMN current_bid DECIMAL(10,2) DEFAULT 0.00 NOT NULL AFTER ', @col_position);
            END IF;
        END IF;
        
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        SELECT '✓ current_bid column added' AS '';
    ELSE
        SELECT '✓ current_bid column already exists' AS '';
    END IF;
END$$

DELIMITER ;

CALL AddCurrentBidColumn();

SELECT '' AS '';

-- =====================================================
-- STEP 3: Initialize current_price and current_bid for existing items
-- =====================================================

SELECT 'STEP 3: Initializing current_price and current_bid for existing items...' AS '';

DELIMITER $$

DROP PROCEDURE IF EXISTS InitializeCurrentPriceAndBid$$

CREATE PROCEDURE InitializeCurrentPriceAndBid()
BEGIN
    DECLARE v_current_price_exists INT DEFAULT 0;
    DECLARE v_current_bid_exists INT DEFAULT 0;
    
    -- Check if columns exist
    SELECT COUNT(*) INTO v_current_price_exists
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'current_price';
    
    SELECT COUNT(*) INTO v_current_bid_exists
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'current_bid';
    
    -- Initialize current_price from starting_price if current_price is 0 or NULL
    IF v_current_price_exists > 0 THEN
        UPDATE items 
        SET current_price = COALESCE(starting_price, starting_bid, 0.00)
        WHERE (current_price IS NULL OR current_price = 0.00)
          AND (starting_price IS NOT NULL OR starting_bid IS NOT NULL);
    END IF;
    
    -- Initialize current_bid from starting_price if current_bid is 0 or NULL
    IF v_current_bid_exists > 0 THEN
        UPDATE items 
        SET current_bid = COALESCE(starting_price, starting_bid, 0.00)
        WHERE (current_bid IS NULL OR current_bid = 0.00)
          AND (starting_price IS NOT NULL OR starting_bid IS NOT NULL);
    END IF;
    
    SELECT '✓ Initialized current_price and current_bid for existing items' AS '';
END$$

DELIMITER ;

CALL InitializeCurrentPriceAndBid();

SELECT '' AS '';

-- =====================================================
-- STEP 4: Add constraints if they don't exist
-- =====================================================

SELECT 'STEP 4: Adding constraints...' AS '';

-- Add check constraint for current_price if it doesn't exist
SET @constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'items'
      AND CONSTRAINT_NAME = 'chk_items_current_price'
);

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE items ADD CONSTRAINT chk_items_current_price CHECK (current_price >= 0)',
    'SELECT "Constraint chk_items_current_price already exists" AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add check constraint for current_bid if it doesn't exist
SET @constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'items'
      AND CONSTRAINT_NAME = 'chk_items_current_bid'
);

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE items ADD CONSTRAINT chk_items_current_bid CHECK (current_bid >= 0)',
    'SELECT "Constraint chk_items_current_bid already exists" AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '✓ Constraints added' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 5: Verification
-- =====================================================

SELECT 'STEP 5: Verifying columns were added...' AS '';

SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_TYPE
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items'
  AND COLUMN_NAME IN ('current_price', 'current_bid')
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- Check if columns exist
SELECT 
    CASE 
        WHEN COUNT(*) = 2 THEN '✓ Both current_price and current_bid columns exist'
        WHEN COUNT(*) = 1 THEN '⚠ Only one column exists - check which one is missing'
        ELSE '✗ Columns are missing'
    END AS verification_status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items'
  AND COLUMN_NAME IN ('current_price', 'current_bid');

SELECT '' AS '';

-- =====================================================
-- CLEANUP: Drop temporary procedures
-- =====================================================

DROP PROCEDURE IF EXISTS AddCurrentPriceColumn;
DROP PROCEDURE IF EXISTS AddCurrentBidColumn;
DROP PROCEDURE IF EXISTS InitializeCurrentPriceAndBid;

SELECT '=======================================================' AS '';
SELECT 'FIX COMPLETED!' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';
SELECT 'The items table now has current_price and current_bid columns.' AS '';
SELECT 'The backend API should now be able to create items successfully.' AS '';
SELECT '' AS '';
SELECT 'Next steps:' AS '';
SELECT '1. Test creating an item via the API' AS '';
SELECT '2. Check backend logs to confirm no more column errors' AS '';
SELECT '3. Verify items are being created correctly' AS '';

