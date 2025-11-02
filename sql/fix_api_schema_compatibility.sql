-- =====================================================
-- FIX API SCHEMA COMPATIBILITY ISSUES
-- =====================================================
-- This script fixes the database schema to match
-- what the API endpoints expect
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'FIXING API SCHEMA COMPATIBILITY ISSUES' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 1: FIX ITEMS TABLE - Add Missing Columns
-- =====================================================

SELECT 'STEP 1: Adding missing columns to items table...' AS '';

-- Use stored procedure approach for conditional ALTER TABLE
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
    END IF;
END$$

DELIMITER ;

-- Add uuid_id column if it doesn't exist
CALL AddColumnIfNotExists('items', 'uuid_id', 'uuid_id VARCHAR(36) UNIQUE NULL AFTER id');

-- Add starting_bid column (API uses this instead of starting_price)
CALL AddColumnIfNotExists('items', 'starting_bid', 'starting_bid DECIMAL(10,2) NULL AFTER starting_price');

-- Copy data from starting_price to starting_bid for existing rows
DELIMITER $$

DROP PROCEDURE IF EXISTS CopyStartingPriceToBid$$

CREATE PROCEDURE CopyStartingPriceToBid()
BEGIN
    DECLARE v_col_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'items' 
      AND COLUMN_NAME = 'starting_bid';
    
    IF v_col_exists > 0 THEN
        UPDATE items 
        SET starting_bid = starting_price 
        WHERE starting_bid IS NULL 
          AND starting_price IS NOT NULL;
    END IF;
END$$

DELIMITER ;

CALL CopyStartingPriceToBid();

-- Add reserve_price column
CALL AddColumnIfNotExists('items', 'reserve_price', 'reserve_price DECIMAL(10,2) NULL');

-- Add end_date column (API uses this instead of bid_deadline)
CALL AddColumnIfNotExists('items', 'end_date', 'end_date DATETIME NULL AFTER bid_deadline');

-- Copy data from bid_deadline to end_date for existing rows
-- Only update if end_date column exists and is NULL
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'items' 
      AND COLUMN_NAME = 'end_date'
);

-- Create helper procedure for conditional UPDATE
DELIMITER $$

DROP PROCEDURE IF EXISTS UpdateEndDateFromDeadline$$

CREATE PROCEDURE UpdateEndDateFromDeadline()
BEGIN
    DECLARE v_col_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'items' 
      AND COLUMN_NAME = 'end_date';
    
    IF v_col_exists > 0 THEN
        -- Update where end_date is NULL and bid_deadline is not NULL
        UPDATE items 
        SET end_date = bid_deadline 
        WHERE bid_deadline IS NOT NULL 
          AND end_date IS NULL;
    END IF;
END$$

DELIMITER ;

-- Call the procedure to update end_date
CALL UpdateEndDateFromDeadline();

-- Generate UUIDs for existing items that don't have one
DELIMITER $$

DROP PROCEDURE IF EXISTS GenerateMissingUUIDs$$

CREATE PROCEDURE GenerateMissingUUIDs()
BEGIN
    DECLARE v_col_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO v_col_exists
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'items' 
      AND COLUMN_NAME = 'uuid_id';
    
    IF v_col_exists > 0 THEN
        UPDATE items 
        SET uuid_id = UUID() 
        WHERE uuid_id IS NULL;
    END IF;
END$$

DELIMITER ;

CALL GenerateMissingUUIDs();

-- Add index on uuid_id (if it doesn't already exist)
SET @index_exists = (
    SELECT COUNT(*) 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'items' 
      AND INDEX_NAME = 'idx_items_uuid_id'
);

SET @index_sql = IF(@index_exists = 0,
    'CREATE INDEX idx_items_uuid_id ON items(uuid_id)',
    'SELECT "Index idx_items_uuid_id already exists" AS status'
);

PREPARE index_stmt FROM @index_sql;
EXECUTE index_stmt;
DEALLOCATE PREPARE index_stmt;

SELECT '✓ Items table columns updated' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 2: CREATE item_images TABLE
-- =====================================================

SELECT 'STEP 2: Creating item_images table...' AS '';

CREATE TABLE IF NOT EXISTS item_images (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id INT UNSIGNED NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    display_order INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    INDEX idx_item_images_item_id (item_id),
    INDEX idx_item_images_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ item_images table created' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 3: CREATE/FIX credit_transactions TABLE
-- =====================================================

SELECT 'STEP 3: Creating credit_transactions table...' AS '';

-- Check if transactions table exists
SET @table_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLES 
    WHERE TABLE_SCHEMA = 'defaultdb' 
      AND TABLE_NAME = 'transactions'
);

-- Create credit_transactions table
CREATE TABLE IF NOT EXISTS credit_transactions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    type ENUM('purchase', 'redemption', 'bid', 'refund', 'transfer', 'bonus', 'outbid_refund', 'buy_now') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description TEXT NULL,
    payment_method VARCHAR(50) NULL,
    status ENUM('pending', 'completed', 'failed', 'cancelled') DEFAULT 'pending',
    reference VARCHAR(255) NULL,
    transaction_id VARCHAR(255) NULL,
    item_id INT UNSIGNED NULL,
    bid_id INT UNSIGNED NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE SET NULL,
    FOREIGN KEY (bid_id) REFERENCES bids(id) ON DELETE SET NULL,
    INDEX idx_credit_transactions_user_id (user_id),
    INDEX idx_credit_transactions_type (type),
    INDEX idx_credit_transactions_status (status),
    INDEX idx_credit_transactions_date (transaction_date),
    INDEX idx_credit_transactions_reference (reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migrate data from transactions to credit_transactions if transactions table exists
SET @sql = IF(@table_exists > 0,
    'INSERT INTO credit_transactions (user_id, type, amount, description, item_id, bid_id, transaction_date, created_at)
     SELECT user_id, transaction_type, amount, description, item_id, bid_id, transaction_date, created_at
     FROM transactions
     WHERE NOT EXISTS (
         SELECT 1 FROM credit_transactions ct 
         WHERE ct.user_id = transactions.user_id 
           AND ct.amount = transactions.amount 
           AND ct.transaction_date = transactions.transaction_date
     )',
    'SELECT "No transactions table to migrate from" AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '✓ credit_transactions table created' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 4: CREATE/FIX v_active_items VIEW
-- =====================================================

SELECT 'STEP 4: Creating v_active_items view...' AS '';

DROP VIEW IF EXISTS v_active_items;

CREATE VIEW v_active_items AS
SELECT 
    i.uuid_id as id,  -- API expects uuid_id as the id field
    i.uuid_id,
    i.id as integer_id,
    i.title,
    i.description,
    i.category_id,
    i.seller_id,
    COALESCE(i.seller_email, u.email) as seller_email,
    COALESCE(i.starting_bid, i.starting_price) as starting_bid,
    COALESCE(i.starting_bid, i.starting_price) as starting_price,
    i.current_bid as current_bid,
    i.current_bid as current_price,  -- API uses current_price for filtering
    i.buy_now_price,
    i.reserve_price,
    i.end_date,
    COALESCE(i.end_date, i.bid_deadline) as bid_deadline,
    i.item_condition,
    i.status,
    i.location,
    i.created_at,
    i.updated_at,
    u.username as seller_username,
    u.alias as seller_alias,
    u.email as seller_user_email,
    c.name as category_name,
    c.description as category_description
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
LEFT JOIN categories c ON i.category_id = c.id
WHERE i.status = 'active' AND i.uuid_id IS NOT NULL;

SELECT '✓ v_active_items view created' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 5: UPDATE STORED PROCEDURES
-- =====================================================

SELECT 'STEP 5: Updating stored procedures...' AS '';

-- Drop existing procedures
DROP PROCEDURE IF EXISTS PlaceBid;
DROP PROCEDURE IF EXISTS BuyNow;

DELIMITER $$

-- Create updated PlaceBid procedure
CREATE PROCEDURE PlaceBid(
    IN p_item_id INT UNSIGNED,
    IN p_bidder_id INT UNSIGNED,
    IN p_amount DECIMAL(10,2),
    IN p_bidder_alias VARCHAR(50)
)
BEGIN
    DECLARE v_current_bid DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_user_credits DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_item_exists INT DEFAULT 0;
    DECLARE v_user_exists INT DEFAULT 0;
    DECLARE v_starting_price DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_previous_bidder_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_previous_bid_amount DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_seller_id INT UNSIGNED DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Lock the bidder's row to prevent race conditions
    SELECT credits INTO v_user_credits
    FROM users
    WHERE id = p_bidder_id
    FOR UPDATE;

    -- Check if item exists and is active (support both starting_bid and starting_price)
    SELECT COUNT(*), seller_id, COALESCE(starting_bid, starting_price)
    INTO v_item_exists, v_seller_id, v_starting_price
    FROM items
    WHERE id = p_item_id AND status = 'active'
    FOR UPDATE;

    IF v_item_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found or not active';
    END IF;

    -- Check bidder is not the seller
    IF p_bidder_id = v_seller_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot bid on your own item';
    END IF;

    -- Check if user exists
    SELECT COUNT(*) INTO v_user_exists
    FROM users
    WHERE id = p_bidder_id;

    IF v_user_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User not found';
    END IF;

    -- Get current highest bid for the item
    SELECT COALESCE(MAX(amount), 0) INTO v_current_bid
    FROM bids
    WHERE item_id = p_item_id AND status IN ('active', 'winning');

    -- Get previous winning bidder info for refund
    SELECT bidder_id, amount INTO v_previous_bidder_id, v_previous_bid_amount
    FROM bids
    WHERE item_id = p_item_id AND status = 'winning'
    ORDER BY created_at DESC
    LIMIT 1;

    -- Validate bid amount
    IF p_amount <= v_current_bid THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid amount must be higher than current bid';
    END IF;

    IF p_amount <= v_starting_price THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid amount must be higher than starting price';
    END IF;

    -- Check if user has enough credits
    IF v_user_credits < p_amount THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits';
    END IF;

    -- Refund previous winning bidder if exists
    IF v_previous_bidder_id IS NOT NULL THEN
        UPDATE users 
        SET credits = credits + v_previous_bid_amount
        WHERE id = v_previous_bidder_id;
        
        -- Update previous bid status
        UPDATE bids 
        SET status = 'outbid'
        WHERE item_id = p_item_id AND bidder_id = v_previous_bidder_id AND status = 'winning';
        
        -- Record refund transaction
        INSERT INTO credit_transactions (user_id, transaction_type, amount, description, item_id)
        VALUES (v_previous_bidder_id, 'outbid_refund', v_previous_bid_amount, 
                CONCAT('Refund for outbid on item ', p_item_id), p_item_id);
    END IF;

    -- Deduct credits from bidder
    UPDATE users 
    SET credits = credits - p_amount
    WHERE id = p_bidder_id;

    -- Record bid transaction
    INSERT INTO credit_transactions (user_id, transaction_type, amount, description, item_id)
    VALUES (p_bidder_id, 'bid', p_amount, 
            CONCAT('Bid placed on item ', p_item_id), p_item_id);

    -- Insert new bid
    INSERT INTO bids (item_id, bidder_id, amount, status)
    VALUES (p_item_id, p_bidder_id, p_amount, 'winning');

    -- Update item current bid
    UPDATE items 
    SET current_bid = p_amount
    WHERE id = p_item_id;

    COMMIT;
    
    SELECT 'Bid placed successfully' AS message, p_amount AS bid_amount;
END$$

-- Create updated BuyNow procedure
CREATE PROCEDURE BuyNow(
    IN p_item_id INT UNSIGNED,
    IN p_buyer_id INT UNSIGNED,
    IN p_buyer_alias VARCHAR(50)
)
BEGIN
    DECLARE v_buy_now_price DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_user_credits DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_item_exists INT DEFAULT 0;
    DECLARE v_user_exists INT DEFAULT 0;
    DECLARE v_seller_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_seller_credits DECIMAL(10,2) DEFAULT 0.00;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Lock buyer's row
    SELECT credits INTO v_user_credits
    FROM users
    WHERE id = p_buyer_id
    FOR UPDATE;

    -- Check if item exists and get buy now price
    SELECT COUNT(*), seller_id, buy_now_price
    INTO v_item_exists, v_seller_id, v_buy_now_price
    FROM items
    WHERE id = p_item_id AND status = 'active'
    FOR UPDATE;

    IF v_item_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found or not active';
    END IF;

    IF v_buy_now_price IS NULL OR v_buy_now_price <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item does not have buy now option';
    END IF;

    -- Check buyer is not the seller
    IF p_buyer_id = v_seller_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot buy your own item';
    END IF;

    -- Check if user exists
    SELECT COUNT(*) INTO v_user_exists
    FROM users
    WHERE id = p_buyer_id;

    IF v_user_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User not found';
    END IF;

    -- Check if user has enough credits
    IF v_user_credits < v_buy_now_price THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits';
    END IF;

    -- Lock seller's row
    SELECT credits INTO v_seller_credits
    FROM users
    WHERE id = v_seller_id
    FOR UPDATE;

    -- Deduct credits from buyer
    UPDATE users 
    SET credits = credits - v_buy_now_price
    WHERE id = p_buyer_id;

    -- Add credits to seller
    UPDATE users 
    SET credits = credits + v_buy_now_price
    WHERE id = v_seller_id;

    -- Record buyer transaction
    INSERT INTO credit_transactions (user_id, transaction_type, amount, description, item_id)
    VALUES (p_buyer_id, 'buy_now', v_buy_now_price, 
            CONCAT('Buy now purchase of item ', p_item_id), p_item_id);

    -- Record seller transaction (as credit addition)
    INSERT INTO credit_transactions (user_id, transaction_type, amount, description, item_id)
    VALUES (v_seller_id, 'buy_now', v_buy_now_price, 
            CONCAT('Item ', p_item_id, ' sold via buy now'), p_item_id);

    -- Update item status
    UPDATE items 
    SET status = 'sold', current_bid = v_buy_now_price
    WHERE id = p_item_id;

    -- Cancel all active bids for this item
    UPDATE bids 
    SET status = 'cancelled'
    WHERE item_id = p_item_id AND status IN ('active', 'winning');

    COMMIT;
    
    SELECT 'Item purchased successfully' AS message, v_buy_now_price AS purchase_amount;
END$$

DELIMITER ;

SELECT '✓ Stored procedures updated' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 6: VERIFICATION
-- =====================================================

SELECT 'STEP 6: Verifying schema fixes...' AS '';

-- Check items table structure
SELECT 'Items table columns:' AS '';
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'items'
  AND COLUMN_NAME IN ('uuid_id', 'starting_bid', 'reserve_price', 'end_date')
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- Check item_images table
SELECT 'item_images table exists:' AS '';
SELECT COUNT(*) as table_exists
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'item_images';

SELECT '' AS '';

-- Check credit_transactions table
SELECT 'credit_transactions table exists:' AS '';
SELECT COUNT(*) as table_exists
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'credit_transactions';

SELECT '' AS '';

-- Check v_active_items view
SELECT 'v_active_items view exists:' AS '';
SELECT COUNT(*) as view_exists
FROM information_schema.VIEWS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'v_active_items';

SELECT '' AS '';

-- Test v_active_items view
SELECT 'Testing v_active_items view...' AS '';
SELECT COUNT(*) as active_items_count FROM v_active_items;

SELECT '' AS '';

SELECT '=======================================================' AS '';
SELECT 'SCHEMA FIXES COMPLETED SUCCESSFULLY!' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';
SELECT 'Changes applied:' AS '';
SELECT '1. Added uuid_id, starting_bid, reserve_price, end_date to items table' AS '';
SELECT '2. Created item_images table' AS '';
SELECT '3. Created credit_transactions table' AS '';
SELECT '4. Created v_active_items view' AS '';
SELECT '5. Updated stored procedures to use credit_transactions' AS '';
SELECT '' AS '';
SELECT 'Next steps:' AS '';
SELECT '1. Test API endpoints on Render' AS '';
SELECT '2. Verify all endpoints are working' AS '';
SELECT '3. Check application logs for any errors' AS '';

