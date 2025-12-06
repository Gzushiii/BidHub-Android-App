-- OPTIONAL: Add item_uuid_id column to bids table for UUID-based lookups
-- NOTE: This is optional since the API has been fixed to work with item_id only
-- This column would allow direct UUID lookups without resolving to integer ID first

USE defaultdb;

DELIMITER $$

DROP PROCEDURE IF EXISTS AddItemUuidIdToBids$$

CREATE PROCEDURE AddItemUuidIdToBids()
BEGIN
    DECLARE v_col_exists INT DEFAULT 0;
    
    -- Check if item_uuid_id column already exists
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'bids'
      AND COLUMN_NAME = 'item_uuid_id';
    
    IF v_col_exists = 0 THEN
        -- Add item_uuid_id column
        ALTER TABLE bids 
        ADD COLUMN item_uuid_id VARCHAR(36) NULL 
        AFTER item_id;
        
        -- Populate item_uuid_id for existing bids by joining with items table
        UPDATE bids b
        INNER JOIN items i ON b.item_id = i.id
        SET b.item_uuid_id = i.uuid_id
        WHERE b.item_uuid_id IS NULL AND i.uuid_id IS NOT NULL;
        
        -- Add index for UUID lookups
        CREATE INDEX idx_bids_item_uuid_id ON bids(item_uuid_id);
        
        SELECT '✓ item_uuid_id column added to bids table and populated' AS '';
    ELSE
        SELECT '✓ item_uuid_id column already exists in bids table' AS '';
    END IF;
END$$

DELIMITER ;

-- Execute the procedure
CALL AddItemUuidIdToBids();

-- Drop the procedure
DROP PROCEDURE IF EXISTS AddItemUuidIdToBids;

-- Verify the column was added
SELECT 'Verifying item_uuid_id column...' AS '';
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb'
  AND TABLE_NAME = 'bids'
  AND COLUMN_NAME = 'item_uuid_id';

SELECT '' AS '';
SELECT '=======================================================' AS '';
SELECT 'item_uuid_id COLUMN ADDITION COMPLETED (OPTIONAL)!' AS '';
SELECT '=======================================================' AS '';
SELECT 'NOTE: This is optional. The API has been fixed to work' AS '';
SELECT 'with item_id only, so this column is not required.' AS '';


