-- =====================================================
-- FIX: Add Missing current_bidder_id Column to items Table
-- =====================================================
-- This script adds the missing current_bidder_id column
-- that the backend API expects when placing bids
-- =====================================================
-- Copy and paste this ENTIRE file into Render's SQL Editor
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'FIXING ITEMS TABLE - ADDING current_bidder_id COLUMN' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 1: Add current_bidder_id column if it doesn't exist
-- =====================================================
DELIMITER $$
DROP PROCEDURE IF EXISTS AddCurrentBidderIdColumn$$
CREATE PROCEDURE AddCurrentBidderIdColumn()
BEGIN
    DECLARE v_col_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'current_bidder_id';

    IF v_col_exists = 0 THEN
        -- Add current_bidder_id column after current_bid or current_price
        -- Try to find a good position after current_price or current_bid
        SET @col_position = (
            SELECT COLUMN_NAME 
            FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = 'defaultdb' 
              AND TABLE_NAME = 'items'
              AND COLUMN_NAME IN ('current_bid', 'current_price', 'reserve_price')
            ORDER BY ORDINAL_POSITION DESC
            LIMIT 1
        );
        
        IF @col_position IS NOT NULL THEN
            SET @sql = CONCAT('ALTER TABLE items ADD COLUMN current_bidder_id INT NULL AFTER ', @col_position);
        ELSE
            -- Fallback: add after seller_id if current_price/current_bid don't exist
            SET @sql = 'ALTER TABLE items ADD COLUMN current_bidder_id INT NULL AFTER seller_id';
        END IF;
        
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        SELECT '✓ Added current_bidder_id column to items table' AS '';
    ELSE
        SELECT 'current_bidder_id column already exists in items table' AS '';
    END IF;
END$$
DELIMITER ;

CALL AddCurrentBidderIdColumn();
DROP PROCEDURE IF EXISTS AddCurrentBidderIdColumn$$;
SELECT '' AS '';

-- =====================================================
-- STEP 2: Add Foreign Key Constraint (Optional but Recommended)
-- =====================================================
DELIMITER $$
DROP PROCEDURE IF EXISTS AddCurrentBidderIdForeignKey$$
CREATE PROCEDURE AddCurrentBidderIdForeignKey()
BEGIN
    DECLARE v_fk_exists INT DEFAULT 0;
    DECLARE v_col_exists INT DEFAULT 0;
    
    -- Check if column exists
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'current_bidder_id';
    
    -- Check if foreign key already exists
    SELECT COUNT(*) INTO v_fk_exists
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND CONSTRAINT_NAME = 'fk_items_current_bidder'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY';
    
    IF v_col_exists > 0 AND v_fk_exists = 0 THEN
        -- Add foreign key constraint
        ALTER TABLE items 
        ADD CONSTRAINT fk_items_current_bidder 
        FOREIGN KEY (current_bidder_id) 
        REFERENCES users(id) 
        ON DELETE SET NULL 
        ON UPDATE CASCADE;
        
        SELECT '✓ Added foreign key constraint for current_bidder_id' AS '';
    ELSE
        IF v_col_exists = 0 THEN
            SELECT 'Skipping foreign key: current_bidder_id column not found' AS '';
        ELSE
            SELECT 'Foreign key constraint already exists for current_bidder_id' AS '';
        END IF;
    END IF;
END$$
DELIMITER ;

CALL AddCurrentBidderIdForeignKey();
DROP PROCEDURE IF EXISTS AddCurrentBidderIdForeignKey$$;
SELECT '' AS '';

-- =====================================================
-- STEP 3: Initialize current_bidder_id for existing items
--         Set it to the bidder_id of the highest bid (if any)
-- =====================================================
DELIMITER $$
DROP PROCEDURE IF EXISTS InitializeCurrentBidderId$$
CREATE PROCEDURE InitializeCurrentBidderId()
BEGIN
    DECLARE v_col_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'current_bidder_id';

    IF v_col_exists > 0 THEN
        -- Update items with the current highest bidder
        -- This finds the most recent winning bid for each item
        UPDATE items i
        INNER JOIN (
            SELECT 
                item_id,
                bidder_id,
                amount,
                created_at,
                ROW_NUMBER() OVER (PARTITION BY item_id ORDER BY amount DESC, created_at DESC) as rn
            FROM bids
            WHERE status IN ('active', 'winning')
        ) latest_bids ON i.id = latest_bids.item_id AND latest_bids.rn = 1
        SET i.current_bidder_id = latest_bids.bidder_id
        WHERE i.current_bidder_id IS NULL;
        
        SELECT CONCAT('✓ Initialized current_bidder_id for items with existing bids') AS '';
    ELSE
        SELECT 'Skipping initialization: current_bidder_id column not found' AS '';
    END IF;
END$$
DELIMITER ;

CALL InitializeCurrentBidderId();
DROP PROCEDURE IF EXISTS InitializeCurrentBidderId$$;
SELECT '' AS '';

-- =====================================================
-- STEP 4: Add Index for Performance (Optional but Recommended)
-- =====================================================
DELIMITER $$
DROP PROCEDURE IF EXISTS AddCurrentBidderIdIndex$$
CREATE PROCEDURE AddCurrentBidderIdIndex()
BEGIN
    DECLARE v_idx_exists INT DEFAULT 0;
    DECLARE v_col_exists INT DEFAULT 0;
    
    -- Check if column exists
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'current_bidder_id';
    
    -- Check if index already exists
    SELECT COUNT(*) INTO v_idx_exists
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND INDEX_NAME = 'idx_items_current_bidder_id';
    
    IF v_col_exists > 0 AND v_idx_exists = 0 THEN
        -- Add index for faster lookups
        CREATE INDEX idx_items_current_bidder_id ON items(current_bidder_id);
        
        SELECT '✓ Added index for current_bidder_id column' AS '';
    ELSE
        IF v_col_exists = 0 THEN
            SELECT 'Skipping index: current_bidder_id column not found' AS '';
        ELSE
            SELECT 'Index already exists for current_bidder_id' AS '';
        END IF;
    END IF;
END$$
DELIMITER ;

CALL AddCurrentBidderIdIndex();
DROP PROCEDURE IF EXISTS AddCurrentBidderIdIndex$$;
SELECT '' AS '';

SELECT '=======================================================' AS '';
SELECT 'SCHEMA FIX COMPLETED SUCCESSFULLY!' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';
SELECT 'Summary:' AS '';
SELECT '  - Added current_bidder_id column to items table' AS '';
SELECT '  - Added foreign key constraint (if applicable)' AS '';
SELECT '  - Initialized current_bidder_id for existing items' AS '';
SELECT '  - Added index for performance' AS '';
SELECT '' AS '';
SELECT 'The items table now has the current_bidder_id column' AS '';
SELECT 'needed for bid placement operations.' AS '';

