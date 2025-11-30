-- =====================================================
-- BidHub Complete Database Schema
-- =====================================================
-- Comprehensive consolidated schema file that includes:
-- - All core tables (users, items, categories, bids, etc.)
-- - All API compatibility columns and fixes
-- - All stored procedures (PlaceBid, BuyNow, EndAuction) with latest fixes
-- - Manual top-up support tables and procedures
-- - All views (v_active_items, etc.)
-- - All performance indices
-- - Default categories
--
-- This file consolidates:
-- - bidhub_schema.sql
-- - fix_api_schema_compatibility.sql
-- - fix_credit_system_comprehensive.sql
-- - fix_placebid_sql_mode_error.sql
-- - add_manual_topup_tables.sql
-- - migrate_fix_generated_ref_size.sql
-- - add_missing_columns.sql
-- - create_bids_table.sql
-- - create_active_items_view.sql
--
-- Usage: mysql -u username -p defaultdb < bidhub_complete_schema.sql
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'BIDHUB COMPLETE DATABASE SCHEMA' AS '';
SELECT 'Starting installation at:', NOW() AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- =====================================================
-- HELPER PROCEDURES FOR IDEMPOTENT OPERATIONS
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
    END IF;
END$$

DELIMITER ;

SELECT '✓ Helper procedures ready' AS '';
SELECT '' AS '';

-- =====================================================
-- TABLE: users
-- =====================================================

SELECT 'Creating users table...' AS '';

CREATE TABLE IF NOT EXISTS users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    alias VARCHAR(50) NOT NULL UNIQUE,
    credits DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
    balance_version INT UNSIGNED DEFAULT 0 NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE NOT NULL,
    profile_picture VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    last_login TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    
    INDEX idx_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_alias (alias),
    INDEX idx_users_phone_number (phone_number),
    INDEX idx_users_is_active (is_active),
    INDEX idx_users_created_at (created_at),
    INDEX idx_users_is_verified (is_verified),
    INDEX idx_users_balance_version (balance_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ users table ready' AS '';
SELECT '' AS '';

-- =====================================================
-- TABLE: categories
-- =====================================================

SELECT 'Creating categories table...' AS '';

CREATE TABLE IF NOT EXISTS categories (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    parent_id INT UNSIGNED NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    sort_order INT DEFAULT 0 NOT NULL,
    icon_path VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_categories_parent_id (parent_id),
    INDEX idx_categories_is_active (is_active),
    INDEX idx_categories_sort_order (sort_order),
    INDEX idx_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ categories table ready' AS '';
SELECT '' AS '';

-- =====================================================
-- TABLE: items
-- =====================================================

SELECT 'Creating items table...' AS '';

CREATE TABLE IF NOT EXISTS items (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid_id VARCHAR(36) UNIQUE NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category_id INT UNSIGNED NULL,
    seller_id INT UNSIGNED NOT NULL,
    starting_price DECIMAL(10,2) NOT NULL,
    starting_bid DECIMAL(10,2) NULL,
    reserve_price DECIMAL(10,2) NULL,
    current_price DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
    current_bid DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
    buy_now_price DECIMAL(10,2) NULL,
    current_bidder_id INT UNSIGNED NULL,
    bid_deadline DATETIME NOT NULL,
    end_date DATETIME NULL,
    billing_deadline DATETIME NOT NULL,
    condition VARCHAR(50) NOT NULL,
    item_condition VARCHAR(50) NULL,
    images JSON NULL,
    status ENUM('draft', 'active', 'paused', 'ended', 'sold', 'cancelled') DEFAULT 'draft' NOT NULL,
    currency VARCHAR(3) DEFAULT 'PHP' NOT NULL,
    view_count INT UNSIGNED DEFAULT 0 NOT NULL,
    bid_count INT UNSIGNED DEFAULT 0 NOT NULL,
    rating DECIMAL(3,2) DEFAULT 0.00 NOT NULL,
    review_count INT UNSIGNED DEFAULT 0 NOT NULL,
    shipping_info TEXT NULL,
    location VARCHAR(255) NULL,
    seller_email VARCHAR(255) NULL,
    tags JSON NULL,
    is_featured BOOLEAN DEFAULT FALSE NOT NULL,
    is_trending BOOLEAN DEFAULT FALSE NOT NULL,
    notes TEXT NULL,
    metadata JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (current_bidder_id) REFERENCES users(id) ON DELETE SET NULL,
    
    CONSTRAINT chk_items_starting_bid CHECK (starting_price >= 0),
    CONSTRAINT chk_items_current_bid CHECK (current_bid >= 0),
    CONSTRAINT chk_items_buy_now_price CHECK (buy_now_price IS NULL OR buy_now_price > 0),
    CONSTRAINT chk_items_rating CHECK (rating >= 0 AND rating <= 5),
    
    INDEX idx_items_seller_id (seller_id),
    INDEX idx_items_category_id (category_id),
    INDEX idx_items_status (status),
    INDEX idx_items_current_bidder_id (current_bidder_id),
    INDEX idx_items_bid_deadline (bid_deadline),
    INDEX idx_items_end_date (end_date),
    INDEX idx_items_created_at (created_at),
    INDEX idx_items_is_featured (is_featured),
    INDEX idx_items_is_trending (is_trending),
    INDEX idx_items_status_deadline (status, bid_deadline),
    INDEX idx_items_seller_status (seller_id, status),
    INDEX idx_items_uuid_id (uuid_id),
    FULLTEXT INDEX idx_items_title_description (title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Copy starting_price to starting_bid if starting_bid is NULL
UPDATE items SET starting_bid = starting_price WHERE starting_bid IS NULL AND starting_price IS NOT NULL;

-- Copy bid_deadline to end_date if end_date is NULL
UPDATE items SET end_date = bid_deadline WHERE end_date IS NULL AND bid_deadline IS NOT NULL;

-- Generate UUIDs for existing items that don't have one
UPDATE items SET uuid_id = UUID() WHERE uuid_id IS NULL;

SELECT '✓ items table ready' AS '';
SELECT '' AS '';

-- =====================================================
-- TABLE: item_images
-- =====================================================

SELECT 'Creating item_images table...' AS '';

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

SELECT '✓ item_images table ready' AS '';
SELECT '' AS '';

-- =====================================================
-- TABLE: bids
-- =====================================================

SELECT 'Creating bids table...' AS '';

CREATE TABLE IF NOT EXISTS bids (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id INT UNSIGNED NOT NULL,
    bidder_id INT UNSIGNED NOT NULL,
    bidder_alias VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    is_winning BOOLEAN DEFAULT FALSE NOT NULL,
    status ENUM('pending', 'active', 'winning', 'outbid', 'won', 'lost', 'cancelled', 'expired', 'invalid') DEFAULT 'pending' NOT NULL,
    notes TEXT NULL,
    metadata JSON NULL,
    
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE RESTRICT,
    
    CONSTRAINT chk_bids_amount CHECK (amount > 0),
    
    INDEX idx_bids_item_id (item_id),
    INDEX idx_bids_bidder_id (bidder_id),
    INDEX idx_bids_status (status),
    INDEX idx_bids_is_winning (is_winning),
    INDEX idx_bids_placed_at (placed_at),
    INDEX idx_bids_created_at (created_at),
    INDEX idx_bids_item_status (item_id, status),
    INDEX idx_bids_bidder_item (bidder_id, item_id),
    INDEX idx_bids_amount (amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ bids table ready' AS '';
SELECT '' AS '';

-- =====================================================
-- TABLE: credit_transactions
-- =====================================================

SELECT 'Creating credit_transactions table...' AS '';

CREATE TABLE IF NOT EXISTS credit_transactions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    type ENUM('purchase', 'redemption', 'bid', 'refund', 'transfer', 'bonus', 'outbid_refund', 'buy_now', 'adjustment') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description TEXT NULL,
    payment_method VARCHAR(50) NULL,
    status ENUM('pending', 'completed', 'failed', 'cancelled') DEFAULT 'pending' NOT NULL,
    reference VARCHAR(255) NULL,
    transaction_id VARCHAR(255) NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    from_user_id INT UNSIGNED NULL,
    to_user_id INT UNSIGNED NULL,
    item_id INT UNSIGNED NULL,
    bid_id INT UNSIGNED NULL,
    idempotency_key VARCHAR(255) UNIQUE NULL,
    audit_trail JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE SET NULL,
    FOREIGN KEY (bid_id) REFERENCES bids(id) ON DELETE SET NULL,
    
    CONSTRAINT chk_credit_transactions_amount CHECK (amount != 0),
    
    INDEX idx_credit_transactions_user_id (user_id),
    INDEX idx_credit_transactions_type (type),
    INDEX idx_credit_transactions_status (status),
    INDEX idx_credit_transactions_created_at (created_at),
    INDEX idx_credit_transactions_transaction_date (transaction_date),
    INDEX idx_credit_transactions_reference (reference),
    INDEX idx_credit_transactions_idempotency (idempotency_key),
    INDEX idx_credit_transactions_user_type (user_id, type),
    INDEX idx_credit_transactions_user_status (user_id, status),
    INDEX idx_credit_transactions_payment_method (payment_method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ credit_transactions table ready' AS '';
SELECT '' AS '';

-- =====================================================
-- TABLE: redemption_codes
-- =====================================================

SELECT 'Creating redemption_codes table...' AS '';

CREATE TABLE IF NOT EXISTS redemption_codes (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    user_id INT UNSIGNED NOT NULL,
    credits DECIMAL(10,2) NOT NULL,
    status ENUM('generated', 'sent', 'delivered', 'redeemed', 'expired', 'invalid') DEFAULT 'generated' NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at DATETIME NOT NULL,
    redeemed_at TIMESTAMP NULL,
    redeemed_by VARCHAR(255) NULL,
    delivery_method VARCHAR(50) NULL,
    delivery_address VARCHAR(500) NULL,
    generated_by VARCHAR(255) NULL,
    notes TEXT NULL,
    usage_count INT UNSIGNED DEFAULT 0 NOT NULL,
    max_usage INT UNSIGNED DEFAULT 1 NOT NULL,
    transaction_id VARCHAR(255) NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    
    CONSTRAINT chk_redemption_codes_credits CHECK (credits > 0),
    CONSTRAINT chk_redemption_codes_expires_at CHECK (expires_at > generated_at),
    CONSTRAINT chk_redemption_codes_usage_count CHECK (usage_count <= max_usage),
    
    INDEX idx_redemption_codes_code (code),
    INDEX idx_redemption_codes_user_id (user_id),
    INDEX idx_redemption_codes_status (status),
    INDEX idx_redemption_codes_expires_at (expires_at),
    INDEX idx_redemption_codes_is_active (is_active),
    INDEX idx_redemption_codes_transaction_id (transaction_id),
    INDEX idx_redemption_codes_delivery_method (delivery_method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ redemption_codes table ready' AS '';
SELECT '' AS '';

-- =====================================================
-- TABLE: topups (Manual Top-Up Support)
-- =====================================================

SELECT 'Creating topups table...' AS '';

CREATE TABLE IF NOT EXISTS topups (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) DEFAULT 'PHP',
    generated_ref VARCHAR(50) NOT NULL UNIQUE,
    user_receipt_ref VARCHAR(64) NULL,
    payment_method ENUM('gcash', 'maya', 'bank_transfer', 'other') NOT NULL,
    payment_number VARCHAR(50) NULL,
    status ENUM('PENDING', 'UNDER_REVIEW', 'CONFIRMED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING',
    confirmed_by INT UNSIGNED NULL,
    rejected_by INT UNSIGNED NULL,
    rejection_reason TEXT NULL,
    instructions TEXT NULL,
    notes TEXT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NULL,
    confirmed_at TIMESTAMP NULL,
    rejected_at TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_topups_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_topups_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_topups_rejected_by FOREIGN KEY (rejected_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_topups_user_id (user_id),
    INDEX idx_topups_status (status),
    INDEX idx_topups_generated_ref (generated_ref),
    INDEX idx_topups_user_receipt_ref (user_receipt_ref),
    INDEX idx_topups_created_at (created_at),
    INDEX idx_topups_payment_method (payment_method),
    INDEX idx_topups_user_status (user_id, status),
    INDEX idx_topups_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ topups table ready' AS '';
SELECT '' AS '';

-- =====================================================
-- TABLE: credit_ledger (Audit Trail)
-- =====================================================

SELECT 'Creating credit_ledger table...' AS '';

CREATE TABLE IF NOT EXISTS credit_ledger (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    delta DECIMAL(10,2) NOT NULL,
    balance_before DECIMAL(10,2) NOT NULL,
    balance_after DECIMAL(10,2) NOT NULL,
    reason ENUM('TOPUP', 'BID', 'REFUND', 'REFUND_OUTBID', 'BUY_NOW', 'ADJUSTMENT', 'TRANSFER', 'BONUS') NOT NULL,
    description TEXT NULL,
    ref_id INT UNSIGNED NULL,
    ref_type ENUM('topup', 'bid', 'item', 'refund', 'manual') NULL,
    credit_transaction_id INT UNSIGNED NULL,
    metadata JSON NULL,
    performed_by INT UNSIGNED NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (credit_transaction_id) REFERENCES credit_transactions(id) ON DELETE SET NULL,
    FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_credit_ledger_user_id (user_id),
    INDEX idx_credit_ledger_reason (reason),
    INDEX idx_credit_ledger_created_at (created_at),
    INDEX idx_credit_ledger_ref (ref_id, ref_type),
    INDEX idx_credit_ledger_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ credit_ledger table ready' AS '';
SELECT '' AS '';

-- =====================================================
-- VIEWS
-- =====================================================

SELECT 'Creating views...' AS '';

-- View for active items with seller and category information
DROP VIEW IF EXISTS v_active_items;

CREATE VIEW v_active_items AS
SELECT 
    i.uuid_id as id,
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
    i.current_bid as current_price,
    i.buy_now_price,
    i.reserve_price,
    i.end_date,
    COALESCE(i.end_date, i.bid_deadline) as bid_deadline,
    i.item_condition,
    COALESCE(i.item_condition, i.condition) as condition,
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
WHERE i.status = 'active' AND (i.uuid_id IS NOT NULL OR i.id IS NOT NULL);

-- View for user bid history
DROP VIEW IF EXISTS v_user_bids;

CREATE VIEW v_user_bids AS
SELECT 
    b.id,
    b.item_id,
    i.title AS item_title,
    b.bidder_id,
    b.bidder_alias,
    b.amount,
    b.placed_at,
    b.created_at,
    b.is_winning,
    b.status,
    i.status AS item_status,
    i.bid_deadline,
    i.end_date
FROM bids b
JOIN items i ON b.item_id = i.id
ORDER BY b.placed_at DESC;

-- View for credit transaction summary
DROP VIEW IF EXISTS v_credit_summary;

CREATE VIEW v_credit_summary AS
SELECT 
    user_id,
    type,
    COUNT(*) AS transaction_count,
    SUM(CASE WHEN status = 'completed' THEN amount ELSE 0 END) AS total_amount,
    MIN(created_at) AS first_transaction,
    MAX(created_at) AS last_transaction
FROM credit_transactions
GROUP BY user_id, type;

-- View for pending topups
DROP VIEW IF EXISTS v_pending_topups;

CREATE VIEW v_pending_topups AS
SELECT 
    t.*,
    u.username,
    u.alias,
    u.credits as user_current_credits
FROM topups t
JOIN users u ON t.user_id = u.id
WHERE t.status IN ('PENDING', 'UNDER_REVIEW')
ORDER BY t.created_at DESC;

-- View for user topup stats
DROP VIEW IF EXISTS v_user_topup_stats;

CREATE VIEW v_user_topup_stats AS
SELECT 
    user_id,
    user_email,
    COUNT(*) as total_topups,
    SUM(CASE WHEN status = 'CONFIRMED' THEN amount ELSE 0 END) as total_confirmed,
    SUM(CASE WHEN status = 'PENDING' THEN amount ELSE 0 END) as total_pending,
    SUM(CASE WHEN status = 'REJECTED' THEN amount ELSE 0 END) as total_rejected,
    MIN(created_at) as first_topup,
    MAX(created_at) as last_topup
FROM topups
GROUP BY user_id, user_email;

SELECT '✓ Views created' AS '';
SELECT '' AS '';

-- =====================================================
-- STORED PROCEDURES
-- =====================================================

SELECT 'Creating stored procedures...' AS '';

DELIMITER //

-- =====================================================
-- PROCEDURE: PlaceBid
-- =====================================================
-- Fixed to avoid SQL mode errors (removed COUNT(*) with non-aggregated columns)
-- Includes proper locking and outbid refunds
-- =====================================================

DROP PROCEDURE IF EXISTS PlaceBid//

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

    -- CRITICAL: Lock the bidder's row to prevent race conditions
    SELECT credits INTO v_user_credits
    FROM users
    WHERE id = p_bidder_id
    FOR UPDATE;

    -- FIXED: Check if item exists and is active (removed COUNT(*) with non-aggregated columns)
    SELECT seller_id, COALESCE(starting_bid, starting_price, 0)
    INTO v_seller_id, v_starting_price
    FROM items
    WHERE id = p_item_id AND status = 'active'
    FOR UPDATE; -- Lock the item row too

    -- Check if item was found (seller_id will be NULL if not found)
    IF v_seller_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found or not active';
    END IF;
    
    SET v_item_exists = 1; -- Item exists if we got here

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
    LIMIT 1;

    -- Use the higher of starting price or current highest bid
    SET v_current_bid = GREATEST(v_current_bid, v_starting_price);

    -- Check if bid amount is higher than current highest bid
    IF p_amount <= v_current_bid THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid must be higher than current highest bid';
    END IF;

    -- Check if user has sufficient credits (already locked above)
    IF v_user_credits < p_amount THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits for bidding';
    END IF;

    -- CRITICAL: Refund previous bidder if they exist and are different
    IF v_previous_bidder_id IS NOT NULL AND v_previous_bidder_id != p_bidder_id THEN
        -- Lock previous bidder's row
        SELECT id INTO @dummy FROM users WHERE id = v_previous_bidder_id FOR UPDATE;

        -- Refund the previous bidder
        UPDATE users
        SET credits = credits + v_previous_bid_amount,
            balance_version = COALESCE(balance_version, 0) + 1
        WHERE id = v_previous_bidder_id;

        -- Record refund transaction with idempotency
        INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date, idempotency_key)
        VALUES (
            v_previous_bidder_id,
            'refund',
            v_previous_bid_amount,
            'completed',
            CONCAT('OUTBID_REFUND_ITEM_', p_item_id),
            NOW(),
            CONCAT('OUTBID_', p_item_id, '_', v_previous_bidder_id, '_', UNIX_TIMESTAMP())
        )
        ON DUPLICATE KEY UPDATE status = 'completed';

        -- Mark previous bid as outbid
        UPDATE bids
        SET status = 'outbid'
        WHERE item_id = p_item_id
        AND bidder_id = v_previous_bidder_id
        AND status = 'winning';
    END IF;

    -- Deduct credits from new bidder
    UPDATE users
    SET credits = credits - p_amount,
        balance_version = COALESCE(balance_version, 0) + 1
    WHERE id = p_bidder_id;

    -- Insert new bid
    INSERT INTO bids (item_id, bidder_id, bidder_alias, amount, status)
    VALUES (p_item_id, p_bidder_id, p_bidder_alias, p_amount, 'winning');

    -- Update item's current price and bidder
    UPDATE items
    SET current_price = p_amount,
        current_bid = p_amount,
        current_bidder_id = p_bidder_id
    WHERE id = p_item_id;

    -- Record bid transaction with idempotency
    INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date, idempotency_key)
    VALUES (
        p_bidder_id,
        'bid',
        p_amount,
        'completed',
        CONCAT('BID_ITEM_', p_item_id),
        NOW(),
        CONCAT('BID_', p_item_id, '_', p_bidder_id, '_', UNIX_TIMESTAMP())
    )
    ON DUPLICATE KEY UPDATE status = 'completed';

    COMMIT;

END//

-- =====================================================
-- PROCEDURE: BuyNow
-- =====================================================
-- Includes proper locking and validation
-- =====================================================

DROP PROCEDURE IF EXISTS BuyNow//

CREATE PROCEDURE BuyNow(
    IN p_item_id INT UNSIGNED,
    IN p_buyer_id INT UNSIGNED,
    IN p_buy_now_price DECIMAL(10,2)
)
BEGIN
    DECLARE v_seller_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_buyer_credits DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_item_exists INT DEFAULT 0;
    DECLARE v_buyer_exists INT DEFAULT 0;
    DECLARE v_item_status VARCHAR(20) DEFAULT '';
    DECLARE v_actual_buy_now_price DECIMAL(10,2) DEFAULT 0.00;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- CRITICAL: Lock buyer's row to prevent race conditions
    SELECT credits INTO v_buyer_credits
    FROM users
    WHERE id = p_buyer_id
    FOR UPDATE;

    -- FIXED: Check if item exists, is active, and lock it (removed COUNT(*) with non-aggregated columns)
    SELECT status, seller_id, buy_now_price
    INTO v_item_status, v_seller_id, v_actual_buy_now_price
    FROM items
    WHERE id = p_item_id
    FOR UPDATE;

    -- Check if item was found (seller_id will be NULL if not found)
    IF v_seller_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found';
    END IF;
    
    SET v_item_exists = 1; -- Item exists if we got here

    IF v_item_status != 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item is not available for purchase';
    END IF;

    -- Verify buy now price matches
    IF v_actual_buy_now_price IS NULL OR v_actual_buy_now_price <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item does not have a buy now price';
    END IF;

    IF ABS(p_buy_now_price - v_actual_buy_now_price) > 0.01 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Price mismatch for buy now';
    END IF;

    -- Check if buyer is not the seller
    IF p_buyer_id = v_seller_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot buy your own item';
    END IF;

    -- Check if buyer exists
    SELECT COUNT(*) INTO v_buyer_exists
    FROM users
    WHERE id = p_buyer_id;

    IF v_buyer_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Buyer not found';
    END IF;

    -- Check if buyer has sufficient credits (already locked above)
    IF v_buyer_credits < p_buy_now_price THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits for buy now';
    END IF;

    -- Lock seller's row
    SELECT id INTO @dummy FROM users WHERE id = v_seller_id FOR UPDATE;

    -- Deduct credits from buyer
    UPDATE users
    SET credits = credits - p_buy_now_price,
        balance_version = COALESCE(balance_version, 0) + 1
    WHERE id = p_buyer_id;

    -- Transfer credits to seller
    UPDATE users
    SET credits = credits + p_buy_now_price,
        balance_version = COALESCE(balance_version, 0) + 1
    WHERE id = v_seller_id;

    -- Mark item as sold
    UPDATE items
    SET status = 'sold',
        current_price = p_buy_now_price,
        current_bid = p_buy_now_price,
        current_bidder_id = p_buyer_id
    WHERE id = p_item_id;

    -- Record buyer transaction with idempotency
    INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date, idempotency_key)
    VALUES (
        p_buyer_id,
        'buy_now',
        p_buy_now_price,
        'completed',
        CONCAT('BUY_NOW_ITEM_', p_item_id),
        NOW(),
        CONCAT('BUYNOW_BUYER_', p_item_id, '_', p_buyer_id, '_', UNIX_TIMESTAMP())
    )
    ON DUPLICATE KEY UPDATE status = 'completed';

    -- Record seller transaction with idempotency
    INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date, idempotency_key)
    VALUES (
        v_seller_id,
        'bonus',
        p_buy_now_price,
        'completed',
        CONCAT('SELL_ITEM_', p_item_id),
        NOW(),
        CONCAT('BUYNOW_SELLER_', p_item_id, '_', v_seller_id, '_', UNIX_TIMESTAMP())
    )
    ON DUPLICATE KEY UPDATE status = 'completed';

    COMMIT;

END//

-- =====================================================
-- PROCEDURE: EndAuction
-- =====================================================
-- Ends an auction and transfers credits to seller
-- =====================================================

DROP PROCEDURE IF EXISTS EndAuction//

CREATE PROCEDURE EndAuction(
    IN p_item_id INT UNSIGNED
)
BEGIN
    DECLARE v_winning_bidder_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_winning_amount DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_seller_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_item_exists INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- FIXED: Check if item exists and lock it (removed COUNT(*) with non-aggregated columns)
    SELECT seller_id INTO v_seller_id
    FROM items
    WHERE id = p_item_id
    FOR UPDATE;

    -- Check if item was found (seller_id will be NULL if not found)
    IF v_seller_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found';
    END IF;
    
    SET v_item_exists = 1; -- Item exists if we got here

    -- Get winning bidder and amount
    SELECT bidder_id, amount INTO v_winning_bidder_id, v_winning_amount
    FROM bids
    WHERE item_id = p_item_id AND status = 'winning'
    LIMIT 1;

    -- If there's a winning bidder, transfer credits to seller
    IF v_winning_bidder_id IS NOT NULL THEN
        -- Lock seller's row
        SELECT id INTO @dummy FROM users WHERE id = v_seller_id FOR UPDATE;

        -- Transfer credits to seller (bidder already paid when placing bid)
        UPDATE users
        SET credits = credits + v_winning_amount,
            balance_version = COALESCE(balance_version, 0) + 1
        WHERE id = v_seller_id;

        -- Record seller credit transaction with idempotency
        INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date, idempotency_key)
        VALUES (
            v_seller_id,
            'bonus',
            v_winning_amount,
            'completed',
            CONCAT('AUCTION_WIN_ITEM_', p_item_id),
            NOW(),
            CONCAT('AUCTION_WIN_', p_item_id, '_', v_seller_id, '_', UNIX_TIMESTAMP())
        )
        ON DUPLICATE KEY UPDATE status = 'completed';

        -- Mark bid as won
        UPDATE bids
        SET status = 'won'
        WHERE item_id = p_item_id AND bidder_id = v_winning_bidder_id;

        -- Mark all other bids as lost
        UPDATE bids
        SET status = 'lost'
        WHERE item_id = p_item_id AND bidder_id != v_winning_bidder_id AND status != 'outbid';

        -- Mark item as sold
        UPDATE items
        SET status = 'sold'
        WHERE id = p_item_id;
    ELSE
        -- No winning bidder, mark item as ended
        UPDATE items
        SET status = 'ended'
        WHERE id = p_item_id;

        -- Mark all bids as lost
        UPDATE bids
        SET status = 'lost'
        WHERE item_id = p_item_id AND status IN ('active', 'winning');
    END IF;

    COMMIT;

END//

-- =====================================================
-- PROCEDURE: sp_confirm_topup
-- =====================================================
-- Confirms a manual top-up request
-- =====================================================

DROP PROCEDURE IF EXISTS sp_confirm_topup//

CREATE PROCEDURE sp_confirm_topup(
    IN p_topup_id INT UNSIGNED,
    IN p_admin_user_id INT UNSIGNED
)
BEGIN
    DECLARE v_user_id INT UNSIGNED;
    DECLARE v_amount DECIMAL(10,2);
    DECLARE v_current_balance DECIMAL(10,2);
    DECLARE v_new_balance DECIMAL(10,2);
    DECLARE v_transaction_id INT UNSIGNED;
    DECLARE v_exists INT;

    START TRANSACTION;

    SELECT COUNT(*) INTO v_exists
    FROM topups
    WHERE id = p_topup_id 
      AND status = 'UNDER_REVIEW'
    FOR UPDATE;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Topup not found or not in UNDER_REVIEW status';
    END IF;

    SELECT user_id, amount INTO v_user_id, v_amount
    FROM topups
    WHERE id = p_topup_id
    FOR UPDATE;

    SELECT credits INTO v_current_balance
    FROM users
    WHERE id = v_user_id
    FOR UPDATE;

    SET v_new_balance = v_current_balance + v_amount;

    UPDATE users
    SET credits = v_new_balance,
        balance_version = COALESCE(balance_version, 0) + 1
    WHERE id = v_user_id;

    INSERT INTO credit_transactions (
        user_id, type, amount, status, payment_method, 
        reference, transaction_id, created_at
    ) VALUES (
        v_user_id, 'purchase', v_amount, 'completed',
        (SELECT payment_method FROM topups WHERE id = p_topup_id),
        (SELECT generated_ref FROM topups WHERE id = p_topup_id),
        (SELECT user_receipt_ref FROM topups WHERE id = p_topup_id),
        NOW()
    );

    SET v_transaction_id = LAST_INSERT_ID();

    INSERT INTO credit_ledger (
        user_id, user_email, delta, balance_before, balance_after,
        reason, description, ref_id, ref_type, credit_transaction_id,
        performed_by
    ) VALUES (
        v_user_id,
        (SELECT user_email FROM topups WHERE id = p_topup_id),
        v_amount,
        v_current_balance,
        v_new_balance,
        'TOPUP',
        CONCAT('Top-up confirmed: ₱', v_amount, ' via ', (SELECT payment_method FROM topups WHERE id = p_topup_id)),
        p_topup_id,
        'topup',
        v_transaction_id,
        p_admin_user_id
    );

    UPDATE topups
    SET status = 'CONFIRMED',
        confirmed_by = p_admin_user_id,
        confirmed_at = NOW()
    WHERE id = p_topup_id;

    COMMIT;

    SELECT 'success' AS status, v_new_balance AS new_balance;
END//

-- =====================================================
-- PROCEDURE: sp_reject_topup
-- =====================================================
-- Rejects a manual top-up request
-- =====================================================

DROP PROCEDURE IF EXISTS sp_reject_topup//

CREATE PROCEDURE sp_reject_topup(
    IN p_topup_id INT UNSIGNED,
    IN p_admin_user_id INT UNSIGNED,
    IN p_rejection_reason TEXT
)
BEGIN
    UPDATE topups
    SET status = 'REJECTED',
        rejected_by = p_admin_user_id,
        rejected_at = NOW(),
        rejection_reason = p_rejection_reason
    WHERE id = p_topup_id
      AND status IN ('PENDING', 'UNDER_REVIEW');

    SELECT 'success' AS status, 'Topup rejected' AS message;
END//

DELIMITER ;

SELECT '✓ Stored procedures created' AS '';
SELECT '' AS '';

-- =====================================================
-- DEFAULT DATA
-- =====================================================

SELECT 'Inserting default categories...' AS '';

INSERT IGNORE INTO categories (id, name, description, sort_order) VALUES 
(1, 'Electronics', 'Electronic devices and accessories', 1),
(2, 'Clothing & Accessories', 'Apparel, shoes, and fashion accessories', 2),
(3, 'Home & Garden', 'Home decor, furniture, and garden supplies', 3),
(4, 'Sports & Recreation', 'Sports equipment and recreational items', 4),
(5, 'Books & Media', 'Books, movies, music, and games', 5),
(6, 'Collectibles', 'Rare items, antiques, and collectibles', 6),
(7, 'Automotive', 'Car parts, accessories, and automotive items', 7),
(8, 'Health & Beauty', 'Health products and beauty items', 8),
(9, 'Toys & Games', 'Toys, board games, and gaming items', 9),
(10, 'Other', 'Miscellaneous items', 10);

SELECT '✓ Default categories inserted' AS '';
SELECT '' AS '';

-- =====================================================
-- VERIFICATION
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'VERIFICATION' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

SELECT 'Tables created:' AS '';
SHOW TABLES;

SELECT '' AS '';
SELECT 'Views created:' AS '';
SHOW FULL TABLES WHERE Table_type = 'VIEW';

SELECT '' AS '';
SELECT 'Stored procedures created:' AS '';
SELECT
    ROUTINE_NAME,
    ROUTINE_TYPE,
    CREATED,
    LAST_ALTERED
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb'
AND ROUTINE_NAME IN ('PlaceBid', 'BuyNow', 'EndAuction', 'sp_confirm_topup', 'sp_reject_topup')
ORDER BY ROUTINE_NAME;

SELECT '' AS '';
SELECT '=======================================================' AS '';
SELECT 'SCHEMA INSTALLATION COMPLETE' AS '';
SELECT 'Completed at:', NOW() AS '';
SELECT '=======================================================' AS '';

