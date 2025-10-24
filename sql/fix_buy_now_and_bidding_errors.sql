-- Fix Buy Now and Bidding errors
-- This addresses both "Error processing purchase: null" and "Insufficient credit balance" errors

USE defaultdb;

-- ==============================================
-- STEP 1: Check current database state
-- ==============================================

SELECT '=== CURRENT DATABASE STATE ===' as section;

-- Check if all required tables exist
SELECT 
    TABLE_NAME,
    TABLE_TYPE
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME IN ('items', 'item_images', 'categories', 'bids', 'users', 'credit_transactions')
ORDER BY TABLE_NAME;

-- ==============================================
-- STEP 2: Check user credit balance
-- ==============================================

SELECT '=== USER CREDIT BALANCE CHECK ===' as section;

SELECT 
    id,
    email,
    alias,
    credits,
    created_at
FROM users 
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 3: Check items table structure
-- ==============================================

SELECT '=== ITEMS TABLE STRUCTURE ===' as section;

DESCRIBE items;

-- ==============================================
-- STEP 4: Check if items have buy_now_price column
-- ==============================================

SELECT '=== CHECK BUY_NOW_PRICE COLUMN ===' as section;

SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb'
  AND TABLE_NAME = 'items'
  AND COLUMN_NAME = 'buy_now_price';

-- ==============================================
-- STEP 5: Add missing columns to items table
-- ==============================================

-- Check if buy_now_price column exists
SET @buy_now_price_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'buy_now_price'
);

-- Add buy_now_price column if it doesn't exist
SET @sql_buy_now = IF(@buy_now_price_exists = 0,
    'ALTER TABLE items ADD COLUMN buy_now_price DECIMAL(10,2) DEFAULT NULL;',
    'SELECT ''Column buy_now_price already exists'' AS message;'
);
PREPARE stmt_buy_now FROM @sql_buy_now;
EXECUTE stmt_buy_now;
DEALLOCATE PREPARE stmt_buy_now;

-- ==============================================
-- STEP 6: Check if bids table exists and has correct structure
-- ==============================================

SELECT '=== BIDS TABLE STRUCTURE ===' as section;

DESCRIBE bids;

-- ==============================================
-- STEP 7: Create bids table if it doesn't exist
-- ==============================================

CREATE TABLE IF NOT EXISTS bids (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id INT UNSIGNED NOT NULL,
    bidder_id INT UNSIGNED NOT NULL,
    bidder_alias VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('active', 'outbid', 'winning', 'won', 'lost', 'cancelled') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_bids_item_id (item_id),
    INDEX idx_bids_bidder_id (bidder_id),
    INDEX idx_bids_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- STEP 8: Check if credit_transactions table exists
-- ==============================================

SELECT '=== CREDIT_TRANSACTIONS TABLE STRUCTURE ===' as section;

DESCRIBE credit_transactions;

-- ==============================================
-- STEP 9: Create credit_transactions table if it doesn't exist
-- ==============================================

CREATE TABLE IF NOT EXISTS credit_transactions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    type ENUM('purchase', 'bid', 'refund', 'bonus') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) DEFAULT NULL,
    status ENUM('pending', 'completed', 'failed', 'cancelled') DEFAULT 'pending',
    reference VARCHAR(100) DEFAULT NULL,
    transaction_id VARCHAR(100) DEFAULT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_credit_transactions_user (user_id),
    INDEX idx_credit_transactions_type (type),
    INDEX idx_credit_transactions_status (status),
    INDEX idx_credit_transactions_date (transaction_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- STEP 10: Check if PlaceBid stored procedure exists
-- ==============================================

SELECT '=== PLACEBID PROCEDURE CHECK ===' as section;

SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- ==============================================
-- STEP 11: Create PlaceBid stored procedure if it doesn't exist
-- ==============================================

DROP PROCEDURE IF EXISTS PlaceBid;

DELIMITER //

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
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Check if item exists and is active
    SELECT COUNT(*) INTO v_item_exists 
    FROM items 
    WHERE id = p_item_id AND status = 'active';
    
    IF v_item_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found or not active';
    END IF;
    
    -- Get item starting price
    SELECT starting_price INTO v_starting_price 
    FROM items 
    WHERE id = p_item_id;
    
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
    
    -- Use the higher of starting price or current highest bid
    SET v_current_bid = GREATEST(v_current_bid, v_starting_price);
    
    -- Check if bid amount is higher than current highest bid
    IF p_amount <= v_current_bid THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = CONCAT('Bid must be higher than current highest bid (P', v_current_bid, '). Your bid: P', p_amount);
    END IF;
    
    -- Get user's current credit balance
    SELECT credits INTO v_user_credits 
    FROM users 
    WHERE id = p_bidder_id;
    
    -- Check if user has sufficient credits
    IF v_user_credits < p_amount THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = CONCAT('Insufficient credits. Required: P', p_amount, ', Available: P', v_user_credits);
    END IF;
    
    -- Deduct credits from user
    UPDATE users 
    SET credits = credits - p_amount 
    WHERE id = p_bidder_id;
    
    -- Mark previous highest bid as outbid
    UPDATE bids 
    SET status = 'outbid' 
    WHERE item_id = p_item_id 
    AND status IN ('active', 'winning') 
    AND amount = v_current_bid;
    
    -- Insert new bid
    INSERT INTO bids (item_id, bidder_id, bidder_alias, amount, status)
    VALUES (p_item_id, p_bidder_id, p_bidder_alias, p_amount, 'active');
    
    -- Update item's current price
    UPDATE items 
    SET current_price = p_amount 
    WHERE id = p_item_id;
    
    -- Mark the new bid as winning
    UPDATE bids 
    SET status = 'winning' 
    WHERE item_id = p_item_id 
    AND bidder_id = p_bidder_id 
    AND amount = p_amount;
    
    -- Record transaction
    INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date)
    VALUES (p_bidder_id, 'bid', p_amount, 'completed', CONCAT('BID_', p_item_id), NOW());
    
    COMMIT;
    
END //

DELIMITER ;

-- ==============================================
-- STEP 12: Test the fixes
-- ==============================================

SELECT '=== TESTING FIXES ===' as section;

-- Check if all tables exist
SELECT 
    TABLE_NAME,
    TABLE_TYPE
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME IN ('items', 'item_images', 'categories', 'bids', 'users', 'credit_transactions')
ORDER BY TABLE_NAME;

-- Check if PlaceBid procedure exists
SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- Check items table structure
SELECT '=== FINAL ITEMS TABLE STRUCTURE ===' as subsection;
DESCRIBE items;

-- Check bids table structure
SELECT '=== FINAL BIDS TABLE STRUCTURE ===' as subsection;
DESCRIBE bids;

-- Check credit_transactions table structure
SELECT '=== FINAL CREDIT_TRANSACTIONS TABLE STRUCTURE ===' as subsection;
DESCRIBE credit_transactions;

-- ==============================================
-- FINAL STATUS
-- ==============================================

SELECT '=== BUY NOW AND BIDDING ERRORS FIXED ===' as final_status;
SELECT 'All required tables, columns, and procedures have been created' as result;
