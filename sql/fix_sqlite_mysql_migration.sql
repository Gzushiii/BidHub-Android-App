-- =====================================================
-- FIX SQLITE TO MYSQL MIGRATION ISSUES
-- =====================================================
-- This script fixes all schema mismatches between
-- Android SQLite DatabaseHelper and MySQL backend
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'FIXING SQLITE TO MYSQL MIGRATION ISSUES' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 1: FIX USERS TABLE SCHEMA
-- =====================================================

SELECT 'STEP 1: Fixing users table schema...' AS '';

-- SQLite uses INTEGER, MySQL needs INT UNSIGNED
-- SQLite uses TEXT, MySQL uses VARCHAR
-- SQLite uses BLOB, MySQL uses VARCHAR for passwords (already using password_hash)
-- SQLite uses REAL, MySQL uses DECIMAL
-- SQLite uses INTEGER for booleans, MySQL uses BOOLEAN/TINYINT(1)
-- SQLite uses DATETIME, MySQL uses TIMESTAMP

-- Add any missing columns that exist in SQLite but not MySQL
DELIMITER $$

DROP PROCEDURE IF EXISTS EnsureColumnExists$$

CREATE PROCEDURE EnsureColumnExists(
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
        SELECT CONCAT('Added column: ', p_column_name) AS '';
    ELSE
        SELECT CONCAT('Column already exists: ', p_column_name) AS '';
    END IF;
END$$

DELIMITER ;

-- Ensure users table has all required columns
CALL EnsureColumnExists('users', 'password', 'password VARCHAR(255) NULL AFTER salt'); -- For compatibility (not used)
CALL EnsureColumnExists('users', 'is_verified', 'is_verified BOOLEAN DEFAULT FALSE'); -- Already exists but ensure type
CALL EnsureColumnExists('users', 'is_active', 'is_active BOOLEAN DEFAULT TRUE'); -- Already exists but ensure type

-- =====================================================
-- STEP 2: FIX ITEMS TABLE SCHEMA
-- =====================================================

SELECT 'STEP 2: Fixing items table schema...' AS '';

-- SQLite column name mappings:
-- starting_bid -> starting_price OR starting_bid (both should exist)
-- current_bid -> current_bid (ensure exists)
-- bid_deadline -> end_date OR bid_deadline (both should exist)
-- billing_deadline -> billing_deadline (ensure exists)
-- condition -> item_condition (MySQL uses item_condition)

-- Ensure both starting_price and starting_bid exist
CALL EnsureColumnExists('items', 'starting_price', 'starting_price DECIMAL(10,2) NOT NULL');
CALL EnsureColumnExists('items', 'starting_bid', 'starting_bid DECIMAL(10,2) NULL AFTER starting_price');
CALL EnsureColumnExists('items', 'current_bid', 'current_bid DECIMAL(10,2) DEFAULT 0.00');
CALL EnsureColumnExists('items', 'current_price', 'current_price DECIMAL(10,2) NULL AFTER current_bid'); -- Some APIs use this
CALL EnsureColumnExists('items', 'buy_now_price', 'buy_now_price DECIMAL(10,2) NULL');

-- Ensure deadline columns exist
CALL EnsureColumnExists('items', 'bid_deadline', 'bid_deadline DATETIME NOT NULL');
CALL EnsureColumnExists('items', 'end_date', 'end_date DATETIME NULL AFTER bid_deadline');
CALL EnsureColumnExists('items', 'billing_deadline', 'billing_deadline DATETIME NULL AFTER end_date');

-- Ensure condition column (MySQL uses item_condition)
CALL EnsureColumnExists('items', 'item_condition', 'item_condition VARCHAR(50) NULL');
CALL EnsureColumnExists('items', 'condition', 'condition VARCHAR(50) NULL AFTER item_condition'); -- Alias for compatibility

-- Ensure status column exists and is ENUM
CALL EnsureColumnExists('items', 'status', 'status ENUM(\'draft\', \'active\', \'ended\', \'sold\', \'cancelled\') DEFAULT \'draft\'');

-- Ensure seller_email exists (for compatibility)
CALL EnsureColumnExists('items', 'seller_email', 'seller_email VARCHAR(255) NULL AFTER seller_id');

-- Ensure images column exists
CALL EnsureColumnExists('items', 'images', 'images TEXT NULL'); -- JSON string of image paths

-- Ensure uuid_id exists for flexible ID lookup
CALL EnsureColumnExists('items', 'uuid_id', 'uuid_id VARCHAR(36) UNIQUE NULL AFTER id');

-- Copy data between compatible columns
UPDATE items 
SET starting_bid = starting_price 
WHERE starting_bid IS NULL AND starting_price IS NOT NULL;

UPDATE items 
SET end_date = bid_deadline 
WHERE end_date IS NULL AND bid_deadline IS NOT NULL;

UPDATE items 
SET condition = item_condition 
WHERE condition IS NULL AND item_condition IS NOT NULL;

-- =====================================================
-- STEP 3: FIX BIDS TABLE SCHEMA
-- =====================================================

SELECT 'STEP 3: Fixing bids table schema...' AS '';

-- Ensure bidder_email exists (for compatibility)
CALL EnsureColumnExists('bids', 'bidder_email', 'bidder_email VARCHAR(255) NULL AFTER bidder_id');
CALL EnsureColumnExists('bids', 'bidder_alias', 'bidder_alias VARCHAR(50) NULL AFTER bidder_email');

-- Ensure is_winning and status columns exist
CALL EnsureColumnExists('bids', 'is_winning', 'is_winning BOOLEAN DEFAULT FALSE');
CALL EnsureColumnExists('bids', 'status', 'status VARCHAR(50) DEFAULT \'ACTIVE\''); -- ACTIVE, OUTBID, WON, etc.

-- =====================================================
-- STEP 4: FIX CREDIT_TRANSACTIONS TABLE SCHEMA
-- =====================================================

SELECT 'STEP 4: Fixing credit_transactions table schema...' AS '';

-- SQLite uses TEXT for type, MySQL uses ENUM
-- Ensure all columns exist
CALL EnsureColumnExists('credit_transactions', 'type', 'type ENUM(\'purchase\', \'redemption\', \'bid\', \'refund\', \'transfer\', \'bonus\', \'outbid_refund\', \'buy_now\') NOT NULL');
CALL EnsureColumnExists('credit_transactions', 'description', 'description TEXT NULL');
CALL EnsureColumnExists('credit_transactions', 'payment_method', 'payment_method VARCHAR(50) NULL');
CALL EnsureColumnExists('credit_transactions', 'status', 'status ENUM(\'pending\', \'completed\', \'failed\', \'cancelled\') DEFAULT \'pending\'');
CALL EnsureColumnExists('credit_transactions', 'reference', 'reference VARCHAR(255) NULL');
CALL EnsureColumnExists('credit_transactions', 'transaction_id', 'transaction_id VARCHAR(255) NULL');

-- =====================================================
-- STEP 5: FIX CATEGORIES TABLE SCHEMA
-- =====================================================

SELECT 'STEP 5: Fixing categories table schema...' AS '';

-- Ensure is_active is BOOLEAN
CALL EnsureColumnExists('categories', 'is_active', 'is_active BOOLEAN DEFAULT TRUE');
CALL EnsureColumnExists('categories', 'sort_order', 'sort_order INT DEFAULT 0');
CALL EnsureColumnExists('categories', 'created_at', 'created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL EnsureColumnExists('categories', 'updated_at', 'updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- =====================================================
-- STEP 6: ADD MISSING TABLES
-- =====================================================

SELECT 'STEP 6: Adding missing tables...' AS '';

-- Redemption codes table
CREATE TABLE IF NOT EXISTS redemption_codes (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    user_id INT UNSIGNED NULL,
    credits DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'unused', -- unused, used, expired
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    used_at TIMESTAMP NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_redemption_codes_code (code),
    INDEX idx_redemption_codes_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Password recovery table
CREATE TABLE IF NOT EXISTS password_recovery (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NULL,
    verification_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_email BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_password_recovery_email (email),
    INDEX idx_password_recovery_code (verification_code),
    INDEX idx_password_recovery_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- STEP 7: FIX COLUMN TYPE MISMATCHES
-- =====================================================

SELECT 'STEP 7: Fixing column type mismatches...' AS '';

-- Fix credits columns to use DECIMAL instead of REAL
-- (MySQL already uses DECIMAL, but ensure precision)
ALTER TABLE users MODIFY COLUMN credits DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE items MODIFY COLUMN starting_price DECIMAL(10,2) NOT NULL;
ALTER TABLE items MODIFY COLUMN starting_bid DECIMAL(10,2) NULL;
ALTER TABLE items MODIFY COLUMN current_bid DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE items MODIFY COLUMN buy_now_price DECIMAL(10,2) NULL;
ALTER TABLE bids MODIFY COLUMN amount DECIMAL(10,2) NOT NULL;
ALTER TABLE credit_transactions MODIFY COLUMN amount DECIMAL(10,2) NOT NULL;

-- Fix boolean columns to use TINYINT(1) or BOOLEAN
-- MySQL BOOLEAN is alias for TINYINT(1)
ALTER TABLE users MODIFY COLUMN is_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users MODIFY COLUMN is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE categories MODIFY COLUMN is_active BOOLEAN DEFAULT TRUE;

-- =====================================================
-- STEP 8: CREATE COMPATIBILITY VIEWS
-- =====================================================

SELECT 'STEP 8: Creating compatibility views...' AS '';

-- View that maps SQLite column names to MySQL columns
CREATE OR REPLACE VIEW v_items_compatibility AS
SELECT 
    id,
    uuid_id,
    title,
    description,
    category_id,
    seller_id,
    seller_email,
    COALESCE(starting_price, starting_bid) as starting_price,
    COALESCE(starting_bid, starting_price) as starting_bid,
    current_bid,
    COALESCE(current_price, current_bid) as current_price,
    buy_now_price,
    COALESCE(end_date, bid_deadline) as end_date,
    bid_deadline,
    billing_deadline,
    COALESCE(item_condition, condition) as item_condition,
    COALESCE(condition, item_condition) as condition,
    images,
    status,
    created_at,
    updated_at
FROM items;

-- =====================================================
-- STEP 9: VERIFICATION
-- =====================================================

SELECT 'STEP 9: Verifying schema fixes...' AS '';

-- Check users table
SELECT 'Users table columns:' AS '';
DESCRIBE users;

-- Check items table
SELECT 'Items table columns:' AS '';
DESCRIBE items;

-- Check for any NULL values in required columns
SELECT 'Checking for NULL values in required columns...' AS '';
SELECT 
    COUNT(*) as items_with_null_starting_price
FROM items 
WHERE starting_price IS NULL;

SELECT 
    COUNT(*) as items_with_null_status
FROM items 
WHERE status IS NULL;

-- =====================================================
-- SUMMARY
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'SQLITE TO MYSQL MIGRATION FIXES COMPLETE' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';
SELECT 'Changes made:' AS '';
SELECT '  - Added compatibility columns (starting_bid, end_date, condition, etc.)' AS '';
SELECT '  - Fixed column types (DECIMAL for credits, BOOLEAN for flags)' AS '';
SELECT '  - Added missing tables (redemption_codes, password_recovery)' AS '';
SELECT '  - Created compatibility views' AS '';
SELECT '  - Ensured all SQLite columns have MySQL equivalents' AS '';
SELECT '' AS '';
SELECT 'Next steps:' AS '';
SELECT '  1. Test API endpoints with new schema' AS '';
SELECT '  2. Verify bidding and buy-now functions' AS '';
SELECT '  3. Update Android code if needed for new column names' AS '';
SELECT '' AS '';

DROP PROCEDURE IF EXISTS EnsureColumnExists;
DROP PROCEDURE IF EXISTS AddColumnIfNotExists;

SELECT '✓ Migration fixes applied successfully!' AS '';

