-- Fix all missing database components that cause "insufficient credits" error
-- This script addresses all critical missing tables, columns, and procedures

USE defaultdb;

-- ==============================================
-- STEP 1: Add missing columns to existing tables
-- ==============================================

-- Add updated_at column to users table
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Add transaction_date column to credit_transactions table
ALTER TABLE credit_transactions ADD COLUMN transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add current_bidder_id column to items table
ALTER TABLE items ADD COLUMN current_bidder_id INT UNSIGNED NULL;

-- Add foreign key constraint for current_bidder_id
ALTER TABLE items ADD CONSTRAINT fk_items_current_bidder 
FOREIGN KEY (current_bidder_id) REFERENCES users(id) ON DELETE SET NULL;

-- ==============================================
-- STEP 2: Create missing bids table
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
-- STEP 3: Create PlaceBid stored procedure
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
    
    -- Check if bid amount is higher than current highest bid
    IF p_amount <= v_current_bid THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid amount must be higher than current highest bid';
    END IF;
    
    -- Get user's current credit balance
    SELECT credits INTO v_user_credits 
    FROM users 
    WHERE id = p_bidder_id;
    
    -- Check if user has sufficient credits
    IF v_user_credits < p_amount THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits';
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
    
    -- Update item's current price and current bidder
    UPDATE items 
    SET current_price = p_amount, current_bidder_id = p_bidder_id 
    WHERE id = p_item_id;
    
    -- Mark the new bid as winning
    UPDATE bids 
    SET status = 'winning' 
    WHERE item_id = p_item_id 
    AND bidder_id = p_bidder_id 
    AND amount = p_amount;
    
    COMMIT;
    
END //

DELIMITER ;

-- ==============================================
-- STEP 4: Verify all components are working
-- ==============================================

SELECT '=== VERIFICATION: ALL COMPONENTS CHECK ===' as section;

-- Check users table structure
SELECT 'Users table structure:' as component;
DESCRIBE users;

-- Check credit_transactions table structure
SELECT 'Credit_transactions table structure:' as component;
DESCRIBE credit_transactions;

-- Check bids table structure
SELECT 'Bids table structure:' as component;
DESCRIBE bids;

-- Check items table structure
SELECT 'Items table structure:' as component;
DESCRIBE items;

-- Check if PlaceBid procedure exists
SELECT 'PlaceBid procedure status:' as component;
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'EXISTS'
        ELSE 'MISSING'
    END as status
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- Check foreign key constraints
SELECT 'Foreign key constraints:' as component;
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND REFERENCED_TABLE_NAME IS NOT NULL;

SELECT '=== ALL COMPONENTS FIXED ===' as final_status;
