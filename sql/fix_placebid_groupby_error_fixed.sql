-- =====================================================
-- FIXED: PlaceBid and BuyNow GROUP BY Error
-- =====================================================
-- Copy and paste this ENTIRE file into Render's SQL Editor
-- This fixes: "In aggregated query without GROUP BY..."
-- AND fixes: "failed to post item" error (wrong table/column names)
-- =====================================================

USE defaultdb;

-- Drop existing procedures
DROP PROCEDURE IF EXISTS PlaceBid;
DROP PROCEDURE IF EXISTS BuyNow;

-- =====================================================
-- Create Fixed PlaceBid Procedure
-- =====================================================

DELIMITER $$

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

    -- FIXED: Split COUNT and column selection to avoid GROUP BY error
    SELECT COUNT(*) INTO v_item_exists
    FROM items
    WHERE id = p_item_id AND status = 'active';
    
    -- If item exists, get the item details
    IF v_item_exists > 0 THEN
        SELECT seller_id, COALESCE(starting_bid, starting_price, 0) as starting_price
        INTO v_seller_id, v_starting_price
        FROM items
        WHERE id = p_item_id AND status = 'active'
        FOR UPDATE;
    END IF;

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
        
        -- FIXED: Use credit_transactions table with correct column names
        INSERT INTO credit_transactions (user_id, type, amount, status, description, item_id, reference)
        VALUES (v_previous_bidder_id, 'outbid_refund', v_previous_bid_amount, 'completed',
                CONCAT('Refund for outbid on item ', p_item_id), p_item_id,
                CONCAT('OUTBID_REFUND_', p_item_id, '_', v_previous_bidder_id));
    END IF;

    -- Deduct credits from bidder
    UPDATE users 
    SET credits = credits - p_amount
    WHERE id = p_bidder_id;

    -- FIXED: Use credit_transactions table with correct column names
    INSERT INTO credit_transactions (user_id, type, amount, status, description, item_id, reference)
    VALUES (p_bidder_id, 'bid', p_amount, 'completed',
            CONCAT('Bid placed on item ', p_item_id), p_item_id,
            CONCAT('BID_', p_item_id, '_', p_bidder_id));

    -- FIXED: Include bidder_alias in INSERT statement
    INSERT INTO bids (item_id, bidder_id, bidder_alias, amount, status)
    VALUES (p_item_id, p_bidder_id, p_bidder_alias, p_amount, 'winning');

    -- Update item current bid
    UPDATE items 
    SET current_bid = p_amount,
        current_price = p_amount,
        current_bidder_id = p_bidder_id
    WHERE id = p_item_id;

    COMMIT;
    
    SELECT 'Bid placed successfully' AS message, p_amount AS bid_amount;
END$$

-- =====================================================
-- Create Fixed BuyNow Procedure
-- =====================================================

CREATE PROCEDURE BuyNow(
    IN p_item_id INT UNSIGNED,
    IN p_buyer_id INT UNSIGNED,
    IN p_buy_now_price DECIMAL(10,2)
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

    -- FIXED: Split COUNT and column selection to avoid GROUP BY error
    SELECT COUNT(*) INTO v_item_exists
    FROM items
    WHERE id = p_item_id AND status = 'active';
    
    -- If item exists, get the item details
    IF v_item_exists > 0 THEN
        SELECT seller_id, buy_now_price
        INTO v_seller_id, v_buy_now_price
        FROM items
        WHERE id = p_item_id AND status = 'active'
        FOR UPDATE;
    END IF;

    IF v_item_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found or not active';
    END IF;

    IF v_buy_now_price IS NULL OR v_buy_now_price <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item does not have buy now option';
    END IF;

    -- Verify buy now price matches
    IF ABS(p_buy_now_price - v_buy_now_price) > 0.01 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Price mismatch for buy now';
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
    IF v_user_credits < p_buy_now_price THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits';
    END IF;

    -- Lock seller's row
    SELECT id INTO @dummy FROM users WHERE id = v_seller_id FOR UPDATE;

    -- Deduct credits from buyer
    UPDATE users 
    SET credits = credits - p_buy_now_price,
        balance_version = COALESCE(balance_version, 0) + 1
    WHERE id = p_buyer_id;

    -- Add credits to seller
    UPDATE users 
    SET credits = credits + p_buy_now_price,
        balance_version = COALESCE(balance_version, 0) + 1
    WHERE id = v_seller_id;

    -- Mark item as sold
    UPDATE items 
    SET status = 'sold',
        current_bid = p_buy_now_price,
        current_price = p_buy_now_price,
        current_bidder_id = p_buyer_id
    WHERE id = p_item_id;

    -- FIXED: Use credit_transactions table with correct column names and idempotency
    INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date, item_id, idempotency_key)
    VALUES (p_buyer_id, 'buy_now', p_buy_now_price, 'completed',
            CONCAT('BUY_NOW_ITEM_', p_item_id), NOW(), p_item_id,
            CONCAT('BUYNOW_BUYER_', p_item_id, '_', p_buyer_id, '_', UNIX_TIMESTAMP()))
    ON DUPLICATE KEY UPDATE status = 'completed';

    -- FIXED: Use credit_transactions table with correct column names and idempotency
    INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date, item_id, idempotency_key)
    VALUES (v_seller_id, 'bonus', p_buy_now_price, 'completed',
            CONCAT('SELL_ITEM_', p_item_id), NOW(), p_item_id,
            CONCAT('BUYNOW_SELLER_', p_item_id, '_', v_seller_id, '_', UNIX_TIMESTAMP()))
    ON DUPLICATE KEY UPDATE status = 'completed';

    COMMIT;
    
    SELECT 'Item purchased successfully' AS message, p_buy_now_price AS purchase_amount;
END$$

DELIMITER ;

-- =====================================================
-- Verification
-- =====================================================

SELECT '✅ PlaceBid and BuyNow procedures fixed successfully!' AS status;

-- Verify procedures exist
SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE,
    CREATED
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb'
  AND ROUTINE_NAME IN ('PlaceBid', 'BuyNow');

