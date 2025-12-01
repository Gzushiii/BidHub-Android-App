-- =====================================================
-- COMPREHENSIVE API REQUIREMENTS FIX
-- =====================================================
-- This script ensures all database tables, columns, and views
-- required by the fixed API routes exist
-- =====================================================
-- Based on API fixes in:
-- - bidhub-backend/src/routes/items.js
-- - bidhub-backend/src/routes/bids.js
-- - bidhub-backend/src/routes/credits.js
-- - bidhub-backend/src/services/auctionEndService.js
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'COMPREHENSIVE API REQUIREMENTS FIX' AS '';
SELECT '=======================================================' AS '';
SELECT '';

-- =====================================================
-- STEP 1: Helper Procedure for Adding Columns
-- =====================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS AddColumnIfNotExists$$

CREATE PROCEDURE AddColumnIfNotExists(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    DECLARE v_col_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name;
    
    IF v_col_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('✓ Added column ', p_column_name, ' to ', p_table_name) AS '';
    ELSE
        SELECT CONCAT('✓ Column ', p_column_name, ' already exists in ', p_table_name) AS '';
    END IF;
END$$

DELIMITER ;

SELECT 'STEP 1: Helper procedure created' AS '';
SELECT '';

-- =====================================================
-- STEP 2: Fix USERS Table
-- =====================================================

SELECT 'STEP 2: Fixing users table...' AS '';

-- Add balance_version column (used by credits.js)
CALL AddColumnIfNotExists('users', 'balance_version', 
    'balance_version INT UNSIGNED DEFAULT 0 NOT NULL AFTER credits');

-- Initialize balance_version for existing users
UPDATE users SET balance_version = 0 WHERE balance_version IS NULL;

SELECT '';

-- =====================================================
-- STEP 3: Fix ITEMS Table - Add All Required Columns
-- =====================================================

SELECT 'STEP 3: Fixing items table - adding required columns...' AS '';

-- Add uuid_id column (required by items.js for UUID-based lookups)
CALL AddColumnIfNotExists('items', 'uuid_id', 
    'uuid_id VARCHAR(36) UNIQUE NULL AFTER id');

-- Generate UUIDs for existing items that don't have one
UPDATE items 
SET uuid_id = UUID() 
WHERE uuid_id IS NULL;

-- Add starting_bid column (API uses both starting_price and starting_bid)
CALL AddColumnIfNotExists('items', 'starting_bid', 
    'starting_bid DECIMAL(10,2) NULL AFTER starting_price');

-- Copy starting_price to starting_bid if starting_bid is NULL
UPDATE items 
SET starting_bid = starting_price 
WHERE starting_bid IS NULL AND starting_price IS NOT NULL;

-- Add current_bid column (API uses both current_price and current_bid)
CALL AddColumnIfNotExists('items', 'current_bid', 
    'current_bid DECIMAL(10,2) DEFAULT 0.00 NULL AFTER current_price');

-- Copy current_price to current_bid if current_bid is NULL
UPDATE items 
SET current_bid = COALESCE(current_price, starting_price, starting_bid, 0)
WHERE current_bid IS NULL;

-- Add reserve_price column (required by items.js)
CALL AddColumnIfNotExists('items', 'reserve_price', 
    'reserve_price DECIMAL(10,2) NULL');

-- Add end_date column (required by items.js and bids.js)
CALL AddColumnIfNotExists('items', 'end_date', 
    'end_date DATETIME NULL AFTER bid_deadline');

-- Copy bid_deadline to end_date if end_date is NULL
UPDATE items 
SET end_date = bid_deadline 
WHERE end_date IS NULL AND bid_deadline IS NOT NULL;

-- Add current_bidder_id column (required by items.js and bids.js)
CALL AddColumnIfNotExists('items', 'current_bidder_id', 
    'current_bidder_id INT UNSIGNED NULL');

-- Initialize current_bidder_id from highest bid if NULL
UPDATE items i
INNER JOIN (
    SELECT item_id, bidder_id,
           ROW_NUMBER() OVER (PARTITION BY item_id ORDER BY amount DESC, created_at DESC) as rn
    FROM bids
    WHERE status IN ('active', 'winning')
) latest_bids ON i.id = latest_bids.item_id AND latest_bids.rn = 1
SET i.current_bidder_id = latest_bids.bidder_id
WHERE i.current_bidder_id IS NULL;

-- Add winner_id and winner_email columns (optional, used by auctionEndService.js)
CALL AddColumnIfNotExists('items', 'winner_id', 
    'winner_id INT UNSIGNED NULL AFTER current_bidder_id');

CALL AddColumnIfNotExists('items', 'winner_email', 
    'winner_email VARCHAR(255) NULL AFTER winner_id');

-- Ensure item_condition column exists (used by view)
-- Check if it exists, if not try to create from 'condition' column or add new
DELIMITER $$

DROP PROCEDURE IF EXISTS EnsureItemConditionColumn$$

CREATE PROCEDURE EnsureItemConditionColumn()
BEGIN
    DECLARE v_item_condition_exists INT DEFAULT 0;
    DECLARE v_condition_exists INT DEFAULT 0;
    
    -- Check if item_condition exists
    SELECT COUNT(*) INTO v_item_condition_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'item_condition';
    
    -- Check if condition exists
    SELECT COUNT(*) INTO v_condition_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'condition';
    
    -- If item_condition doesn't exist, add it
    IF v_item_condition_exists = 0 THEN
        IF v_condition_exists > 0 THEN
            -- Copy from condition column
            ALTER TABLE items ADD COLUMN item_condition VARCHAR(50) NULL;
            UPDATE items SET item_condition = `condition` WHERE item_condition IS NULL;
        ELSE
            -- Add new column with default
            ALTER TABLE items ADD COLUMN item_condition VARCHAR(50) NULL DEFAULT 'good';
        END IF;
        SELECT '✓ Added item_condition column' AS '';
    ELSE
        SELECT '✓ item_condition column already exists' AS '';
    END IF;
END$$

DELIMITER ;

CALL EnsureItemConditionColumn();
DROP PROCEDURE IF EXISTS EnsureItemConditionColumn;

SELECT '';

-- =====================================================
-- STEP 4: Create credit_transactions Table
-- =====================================================

SELECT 'STEP 4: Creating credit_transactions table...' AS '';

CREATE TABLE IF NOT EXISTS credit_transactions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'completed',
    payment_method VARCHAR(50) NULL,
    transaction_id VARCHAR(255) NULL,
    reference VARCHAR(255) NULL,
    item_id INT UNSIGNED NULL,
    description TEXT NULL,
    transaction_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_reference (reference),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ credit_transactions table ensured' AS '';
SELECT '';

-- =====================================================
-- STEP 5: Create item_images Table
-- =====================================================

SELECT 'STEP 5: Creating item_images table...' AS '';

CREATE TABLE IF NOT EXISTS item_images (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id INT UNSIGNED NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    display_order INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_item_id (item_id),
    INDEX idx_display_order (display_order),
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ item_images table ensured' AS '';
SELECT '';

-- =====================================================
-- STEP 6: Create notifications Table (Optional)
-- =====================================================

SELECT 'STEP 6: Creating notifications table (optional)...' AS '';

CREATE TABLE IF NOT EXISTS notifications (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSON NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_read_at (read_at),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ notifications table ensured' AS '';
SELECT '';

-- =====================================================
-- STEP 7: Fix BIDS Table - Add bidder_alias if missing
-- =====================================================

SELECT 'STEP 7: Fixing bids table...' AS '';

-- Add bidder_alias column if missing (used by bids.js)
CALL AddColumnIfNotExists('bids', 'bidder_alias', 
    'bidder_alias VARCHAR(50) NOT NULL DEFAULT "" AFTER bidder_id');

-- Populate bidder_alias from users table if empty
UPDATE bids b
INNER JOIN users u ON b.bidder_id = u.id
SET b.bidder_alias = COALESCE(NULLIF(u.alias, ''), u.username, u.email, CONCAT('user_', u.id))
WHERE b.bidder_alias = '' OR b.bidder_alias IS NULL;

-- Add item_uuid_id column (optional, for UUID-based lookups)
CALL AddColumnIfNotExists('bids', 'item_uuid_id', 
    'item_uuid_id VARCHAR(36) NULL AFTER item_id');

-- Populate item_uuid_id from items table
UPDATE bids b
INNER JOIN items i ON b.item_id = i.id
SET b.item_uuid_id = i.uuid_id
WHERE b.item_uuid_id IS NULL AND i.uuid_id IS NOT NULL;

-- Add index for UUID lookups
DELIMITER $$

DROP PROCEDURE IF EXISTS AddBidsItemUuidIndex$$

CREATE PROCEDURE AddBidsItemUuidIndex()
BEGIN
    DECLARE v_idx_exists INT DEFAULT 0;
    DECLARE v_col_exists INT DEFAULT 0;
    
    -- Check if column exists
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'bids'
      AND COLUMN_NAME = 'item_uuid_id';
    
    -- Check if index exists
    SELECT COUNT(*) INTO v_idx_exists
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'bids'
      AND INDEX_NAME = 'idx_bids_item_uuid_id';
    
    IF v_col_exists > 0 AND v_idx_exists = 0 THEN
        CREATE INDEX idx_bids_item_uuid_id ON bids(item_uuid_id);
        SELECT '✓ Added index idx_bids_item_uuid_id' AS '';
    ELSE
        IF v_col_exists = 0 THEN
            SELECT 'Skipping index: item_uuid_id column not found' AS '';
        ELSE
            SELECT 'Index idx_bids_item_uuid_id already exists' AS '';
        END IF;
    END IF;
END$$

DELIMITER ;

CALL AddBidsItemUuidIndex();
DROP PROCEDURE IF EXISTS AddBidsItemUuidIndex;

SELECT '';

-- =====================================================
-- STEP 8: Create or Fix v_active_items View
-- =====================================================

SELECT 'STEP 8: Creating/fixing v_active_items view...' AS '';

DROP VIEW IF EXISTS v_active_items;

-- Create view using a procedure to handle column existence dynamically
DELIMITER $$

DROP PROCEDURE IF EXISTS CreateActiveItemsView$$

CREATE PROCEDURE CreateActiveItemsView()
BEGIN
    DECLARE v_item_condition_exists INT DEFAULT 0;
    
    -- Check if item_condition column exists
    SELECT COUNT(*) INTO v_item_condition_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'item_condition';
    
    -- Build view SQL based on available columns
    IF v_item_condition_exists > 0 THEN
        SET @view_sql = 'CREATE VIEW v_active_items AS
SELECT
    i.id,
    i.uuid_id,
    i.title,
    i.description,
    i.category_id,
    i.seller_id,
    COALESCE(i.starting_price, i.starting_bid, 0) as starting_price,
    COALESCE(i.starting_bid, i.starting_price, 0) as starting_bid,
    i.reserve_price,
    COALESCE(i.current_price, i.current_bid, COALESCE(i.starting_price, i.starting_bid, 0)) as current_price,
    COALESCE(i.current_bid, i.current_price, COALESCE(i.starting_bid, i.starting_price, 0)) as current_bid,
    i.buy_now_price,
    COALESCE(i.item_condition, ''good'') as item_condition,
    COALESCE(i.item_condition, ''good'') as `condition`,
    i.status,
    i.end_date,
    COALESCE(i.bid_deadline, i.end_date) as bid_deadline,
    i.current_bidder_id,
    i.created_at,
    i.updated_at,
    COALESCE(i.seller_email, u.email) as seller_email,
    u.username as seller_username,
    u.alias as seller_alias,
    c.name as category_name,
    c.description as category_description
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
LEFT JOIN categories c ON i.category_id = c.id
WHERE i.status = ''active''';
    ELSE
        SET @view_sql = 'CREATE VIEW v_active_items AS
SELECT
    i.id,
    i.uuid_id,
    i.title,
    i.description,
    i.category_id,
    i.seller_id,
    COALESCE(i.starting_price, i.starting_bid, 0) as starting_price,
    COALESCE(i.starting_bid, i.starting_price, 0) as starting_bid,
    i.reserve_price,
    COALESCE(i.current_price, i.current_bid, COALESCE(i.starting_price, i.starting_bid, 0)) as current_price,
    COALESCE(i.current_bid, i.current_price, COALESCE(i.starting_bid, i.starting_price, 0)) as current_bid,
    i.buy_now_price,
    ''good'' as item_condition,
    ''good'' as `condition`,
    i.status,
    i.end_date,
    COALESCE(i.bid_deadline, i.end_date) as bid_deadline,
    i.current_bidder_id,
    i.created_at,
    i.updated_at,
    COALESCE(i.seller_email, u.email) as seller_email,
    u.username as seller_username,
    u.alias as seller_alias,
    c.name as category_name,
    c.description as category_description
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
LEFT JOIN categories c ON i.category_id = c.id
WHERE i.status = ''active''';
    END IF;
    
    PREPARE stmt FROM @view_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SELECT '✓ v_active_items view created' AS '';
END$$

DELIMITER ;

CALL CreateActiveItemsView();
DROP PROCEDURE IF EXISTS CreateActiveItemsView;

SELECT '✓ v_active_items view created' AS '';
SELECT '';

-- =====================================================
-- STEP 9: Add Foreign Keys and Indexes
-- =====================================================

SELECT 'STEP 9: Adding foreign keys and indexes...' AS '';

-- Add foreign key for current_bidder_id if column exists and FK doesn't exist
DELIMITER $$

DROP PROCEDURE IF EXISTS AddCurrentBidderForeignKeyAndIndex$$

CREATE PROCEDURE AddCurrentBidderForeignKeyAndIndex()
BEGIN
    DECLARE v_fk_exists INT DEFAULT 0;
    DECLARE v_col_exists INT DEFAULT 0;
    DECLARE v_idx_exists INT DEFAULT 0;
    DECLARE v_col_type VARCHAR(50) DEFAULT '';
    DECLARE v_col_is_unsigned TINYINT DEFAULT 0;
    
    -- Check if column exists
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'current_bidder_id';
    
    -- Get column type if it exists
    IF v_col_exists > 0 THEN
        SELECT CASE WHEN COLUMN_TYPE LIKE '%unsigned%' THEN 1 ELSE 0 END
        INTO v_col_is_unsigned
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'defaultdb'
          AND TABLE_NAME = 'items'
          AND COLUMN_NAME = 'current_bidder_id';
    END IF;
    
    -- If column exists but is not UNSIGNED, modify it
    IF v_col_exists > 0 AND v_col_is_unsigned = 0 THEN
        -- Check if there are any existing foreign keys or indexes we need to drop first
        -- Drop foreign key if exists
        SET @fk_check = (
            SELECT COUNT(*)
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = 'defaultdb'
              AND TABLE_NAME = 'items'
              AND CONSTRAINT_NAME LIKE '%current_bidder%'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        );
        
        IF @fk_check > 0 THEN
            SET @drop_fk = (
                SELECT CONCAT('ALTER TABLE items DROP FOREIGN KEY ', CONSTRAINT_NAME)
                FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = 'defaultdb'
                  AND TABLE_NAME = 'items'
                  AND CONSTRAINT_NAME LIKE '%current_bidder%'
                  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
                LIMIT 1
            );
            SET @sql = @drop_fk;
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Modify column to UNSIGNED
        ALTER TABLE items MODIFY COLUMN current_bidder_id INT UNSIGNED NULL;
        SELECT '✓ Modified current_bidder_id to INT UNSIGNED' AS '';
    END IF;
    
    -- Check if foreign key exists
    SELECT COUNT(*) INTO v_fk_exists
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND CONSTRAINT_NAME = 'fk_items_current_bidder'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY';
    
    -- Add foreign key if needed
    IF v_col_exists > 0 AND v_fk_exists = 0 THEN
        ALTER TABLE items 
        ADD CONSTRAINT fk_items_current_bidder 
        FOREIGN KEY (current_bidder_id) 
        REFERENCES users(id) 
        ON DELETE SET NULL 
        ON UPDATE CASCADE;
        SELECT '✓ Added foreign key fk_items_current_bidder' AS '';
    ELSE
        IF v_col_exists = 0 THEN
            SELECT 'Skipping foreign key: current_bidder_id column not found' AS '';
        ELSE
            SELECT 'Foreign key fk_items_current_bidder already exists' AS '';
        END IF;
    END IF;
    
    -- Check if index exists
    SELECT COUNT(*) INTO v_idx_exists
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND INDEX_NAME = 'idx_items_current_bidder_id';
    
    -- Add index if needed
    IF v_col_exists > 0 AND v_idx_exists = 0 THEN
        CREATE INDEX idx_items_current_bidder_id ON items(current_bidder_id);
        SELECT '✓ Added index idx_items_current_bidder_id' AS '';
    ELSE
        IF v_col_exists = 0 THEN
            SELECT 'Skipping index: current_bidder_id column not found' AS '';
        ELSE
            SELECT 'Index idx_items_current_bidder_id already exists' AS '';
        END IF;
    END IF;
END$$

DELIMITER ;

CALL AddCurrentBidderForeignKeyAndIndex();
DROP PROCEDURE IF EXISTS AddCurrentBidderForeignKeyAndIndex;

SELECT '';

-- =====================================================
-- STEP 10: Cleanup and Verification
-- =====================================================

SELECT 'STEP 10: Cleanup...' AS '';

DROP PROCEDURE IF EXISTS AddColumnIfNotExists;

SELECT '';

-- =====================================================
-- VERIFICATION
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'VERIFICATION' AS '';
SELECT '=======================================================' AS '';
SELECT '';

-- Verify users table
SELECT 'Users table columns:' AS '';
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb' AND TABLE_NAME = 'users'
  AND COLUMN_NAME IN ('credits', 'balance_version')
ORDER BY COLUMN_NAME;

SELECT '';

-- Verify items table columns
SELECT 'Items table columns:' AS '';
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb' AND TABLE_NAME = 'items'
  AND COLUMN_NAME IN ('uuid_id', 'starting_bid', 'current_bid', 'reserve_price', 
                      'end_date', 'current_bidder_id', 'winner_id', 'winner_email')
ORDER BY COLUMN_NAME;

SELECT '';

-- Verify tables exist
SELECT 'Required tables:' AS '';
SELECT TABLE_NAME 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME IN ('credit_transactions', 'item_images', 'notifications', 'items', 'users', 'bids')
ORDER BY TABLE_NAME;

SELECT '';

-- Verify view exists
SELECT 'Views:' AS '';
SELECT TABLE_NAME 
FROM information_schema.VIEWS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'v_active_items';

SELECT '';
SELECT '=======================================================' AS '';
SELECT 'ALL API REQUIREMENTS FIX COMPLETED!' AS '';
SELECT '=======================================================' AS '';
SELECT '';
SELECT 'Summary:' AS '';
SELECT '  ✓ Users table: balance_version column' AS '';
SELECT '  ✓ Items table: uuid_id, starting_bid, current_bid, reserve_price,' AS '';
SELECT '                  end_date, current_bidder_id, winner_id, winner_email' AS '';
SELECT '  ✓ credit_transactions table created' AS '';
SELECT '  ✓ item_images table created' AS '';
SELECT '  ✓ notifications table created (optional)' AS '';
SELECT '  ✓ bids table: bidder_alias, item_uuid_id columns' AS '';
SELECT '  ✓ v_active_items view created/fixed' AS '';
SELECT '';
SELECT 'All database requirements for the API are now met!' AS '';

