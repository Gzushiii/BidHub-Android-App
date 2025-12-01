-- =====================================================
-- FIX PlaceBid Stored Procedure and Bids Table Columns
-- =====================================================
-- This script fixes:
-- 1. PlaceBid stored procedure to remove idempotency_key reference from bids table
-- 2. Ensures bids table has correct columns (created_at, not placed_at)
-- 3. Ensures bidder_alias column exists in bids table
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'FIXING PlaceBid PROCEDURE AND BIDS TABLE' AS '';
SELECT '=======================================================' AS '';
SELECT '';

-- =====================================================
-- STEP 1: Ensure bids table has correct columns
-- =====================================================

SELECT 'STEP 1: Ensuring bids table has correct columns...' AS '';

-- Ensure bidder_alias column exists
DELIMITER $$

DROP PROCEDURE IF EXISTS EnsureBidsTableColumns$$

CREATE PROCEDURE EnsureBidsTableColumns()
BEGIN
    DECLARE v_bidder_alias_exists INT DEFAULT 0;
    DECLARE v_created_at_exists INT DEFAULT 0;
    
    -- Check if bidder_alias exists
    SELECT COUNT(*) INTO v_bidder_alias_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'bids'
      AND COLUMN_NAME = 'bidder_alias';
    
    IF v_bidder_alias_exists = 0 THEN
        ALTER TABLE bids ADD COLUMN bidder_alias VARCHAR(50) NOT NULL DEFAULT '' AFTER bidder_id;
        SELECT '✓ Added bidder_alias column to bids table' AS '';
    ELSE
        SELECT '✓ bidder_alias column already exists' AS '';
    END IF;
    
    -- Check if created_at exists (should already exist, but verify)
    SELECT COUNT(*) INTO v_created_at_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'bids'
      AND COLUMN_NAME = 'created_at';
    
    IF v_created_at_exists = 0 THEN
        ALTER TABLE bids ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER status;
        SELECT '✓ Added created_at column to bids table' AS '';
    ELSE
        SELECT '✓ created_at column already exists' AS '';
    END IF;
END$$

DELIMITER ;

CALL EnsureBidsTableColumns();
DROP PROCEDURE IF EXISTS EnsureBidsTableColumns;

SELECT '';

-- =====================================================
-- STEP 2: Create Fixed PlaceBid Stored Procedure
-- =====================================================

SELECT 'STEP 2: Creating fixed PlaceBid stored procedure...' AS '';

DELIMITER $$

DROP PROCEDURE IF EXISTS PlaceBid$$

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
    DECLARE v_current_bidder_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_new_balance DECIMAL(10,2) DEFAULT 0.00;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Lock user's credits
    SELECT credits INTO v_user_credits
    FROM users
    WHERE id = p_bidder_id
    FOR UPDATE;

    -- Check if item exists
    SELECT COUNT(*) INTO v_item_exists
    FROM items
    WHERE id = p_item_id AND status = 'active';
    
    IF v_item_exists > 0 THEN
        SELECT seller_id, 
               COALESCE(starting_price, starting_bid, 0),
               current_bidder_id
        INTO v_seller_id, v_starting_price, v_current_bidder_id
        FROM items
        WHERE id = p_item_id AND status = 'active'
        FOR UPDATE;
    END IF;

    IF v_item_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found or not active';
    END IF;

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

    -- Get current highest bid
    SELECT COALESCE(MAX(amount), 0) INTO v_current_bid
    FROM bids
    WHERE item_id = p_item_id AND status IN ('active', 'winning');

    -- Get previous winning bidder info
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

    IF v_user_credits < p_amount THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits';
    END IF;

    -- Refund previous bidder if exists
    IF v_previous_bidder_id IS NOT NULL AND v_previous_bidder_id != p_bidder_id THEN
        UPDATE users 
        SET credits = credits + v_previous_bid_amount
        WHERE id = v_previous_bidder_id;
        
        UPDATE bids 
        SET status = 'outbid'
        WHERE item_id = p_item_id 
          AND bidder_id = v_previous_bidder_id 
          AND status = 'winning';
        
        -- Record refund transaction
        INSERT INTO credit_transactions (user_id, type, amount, status, description, item_id, reference)
        VALUES (v_previous_bidder_id, 'outbid_refund', v_previous_bid_amount, 'completed',
                CONCAT('Refund for outbid on item ', p_item_id), p_item_id,
                CONCAT('OUTBID_REFUND_', p_item_id, '_', v_previous_bidder_id, '_', UNIX_TIMESTAMP()));
    END IF;

    -- Deduct credits from new bidder
    UPDATE users 
    SET credits = credits - p_amount
    WHERE id = p_bidder_id;

    -- Get new balance
    SELECT credits INTO v_new_balance
    FROM users
    WHERE id = p_bidder_id;

    -- Record bid transaction
    INSERT INTO credit_transactions (user_id, type, amount, status, description, item_id, reference)
    VALUES (p_bidder_id, 'bid', p_amount, 'completed',
            CONCAT('Bid placed on item ', p_item_id), p_item_id,
            CONCAT('BID_', p_item_id, '_', p_bidder_id, '_', UNIX_TIMESTAMP()));

    -- Insert new bid (FIXED: No idempotency_key in bids table)
    INSERT INTO bids (item_id, bidder_id, bidder_alias, amount, status)
    VALUES (p_item_id, p_bidder_id, COALESCE(p_bidder_alias, ''), p_amount, 'winning');

    -- Update item with new bid info
    UPDATE items 
    SET current_bid = p_amount,
        current_price = p_amount,
        current_bidder_id = p_bidder_id
    WHERE id = p_item_id;

    COMMIT;
    
    -- Return result
    SELECT 
        'Bid placed successfully' AS message, 
        p_amount AS bid_amount,
        v_new_balance AS new_balance,
        COALESCE(v_previous_bid_amount, 0) AS refunded_amount;
END$$

DELIMITER ;

SELECT '✓ PlaceBid stored procedure created' AS '';
SELECT '';

-- =====================================================
-- VERIFICATION
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'VERIFICATION' AS '';
SELECT '=======================================================' AS '';
SELECT '';

-- Check bids table columns
SELECT 'Bids table columns:' AS '';
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'bids'
  AND COLUMN_NAME IN ('bidder_alias', 'created_at', 'item_id', 'bidder_id', 'amount', 'status')
ORDER BY COLUMN_NAME;

SELECT '';

-- Check if PlaceBid procedure exists
SELECT 'Stored procedures:' AS '';
SELECT ROUTINE_NAME, ROUTINE_TYPE
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb' 
  AND ROUTINE_NAME = 'PlaceBid';

SELECT '';
SELECT '=======================================================' AS '';
SELECT 'FIX COMPLETED!' AS '';
SELECT '=======================================================' AS '';
SELECT '';
SELECT 'Summary:' AS '';
SELECT '  ✓ Ensured bids table has bidder_alias and created_at columns' AS '';
SELECT '  ✓ Created fixed PlaceBid procedure without idempotency_key reference' AS '';
SELECT '  ✓ PlaceBid now uses credit_transactions table for transaction records' AS '';
SELECT '';

