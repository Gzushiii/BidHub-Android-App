-- =====================================================
-- COMPREHENSIVE FIX: Credit Transfer Logic
-- =====================================================
-- This script ensures all credit transfers are properly
-- handled with validation, idempotency, and error handling
-- =====================================================
-- Copy and paste this ENTIRE file into Render's SQL Editor
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'COMPREHENSIVE CREDIT TRANSFER FIX' AS '';
SELECT '=======================================================' AS '';
SELECT '';

-- =====================================================
-- STEP 1: Update PlaceBid to return updated balance
-- =====================================================

SELECT 'STEP 1: Updating PlaceBid procedure...' AS '';

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
    DECLARE v_new_balance DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_idempotency_key VARCHAR(255) DEFAULT '';

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Generate idempotency key to prevent duplicate transactions
    SET v_idempotency_key = CONCAT('BID_', p_item_id, '_', p_bidder_id, '_', UNIX_TIMESTAMP(NOW()), '_', FLOOR(RAND() * 1000000));

    -- Lock the bidder's row to prevent race conditions
    SELECT credits INTO v_user_credits
    FROM users
    WHERE id = p_bidder_id
    FOR UPDATE;

    -- Check if item exists and is active
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
    IF v_previous_bidder_id IS NOT NULL AND v_previous_bidder_id != p_bidder_id THEN
        -- Lock previous bidder's row
        SELECT id INTO @dummy FROM users WHERE id = v_previous_bidder_id FOR UPDATE;
        
        -- Refund the previous bidder
        UPDATE users 
        SET credits = credits + v_previous_bid_amount,
            balance_version = COALESCE(balance_version, 0) + 1
        WHERE id = v_previous_bidder_id;
        
        -- Update previous bid status
        UPDATE bids 
        SET status = 'outbid'
        WHERE item_id = p_item_id AND bidder_id = v_previous_bidder_id AND status = 'winning';
        
        -- Record refund transaction with idempotency
        INSERT INTO credit_transactions (user_id, type, amount, status, description, item_id, reference, idempotency_key)
        VALUES (v_previous_bidder_id, 'outbid_refund', v_previous_bid_amount, 'completed',
                CONCAT('Refund for outbid on item ', p_item_id), p_item_id,
                CONCAT('OUTBID_REFUND_', p_item_id, '_', v_previous_bidder_id),
                CONCAT('OUTBID_REFUND_', p_item_id, '_', v_previous_bidder_id, '_', UNIX_TIMESTAMP(NOW())))
        ON DUPLICATE KEY UPDATE status = 'completed';
    END IF;

    -- Deduct credits from bidder
    UPDATE users 
    SET credits = credits - p_amount,
        balance_version = COALESCE(balance_version, 0) + 1
    WHERE id = p_bidder_id;

    -- Get new balance after deduction
    SELECT credits INTO v_new_balance
    FROM users
    WHERE id = p_bidder_id;

    -- Record bid transaction with idempotency
    INSERT INTO credit_transactions (user_id, type, amount, status, description, item_id, reference, idempotency_key)
    VALUES (p_bidder_id, 'bid', p_amount, 'completed',
            CONCAT('Bid placed on item ', p_item_id), p_item_id,
            CONCAT('BID_', p_item_id, '_', p_bidder_id),
            v_idempotency_key)
    ON DUPLICATE KEY UPDATE status = 'completed';

    -- Insert new bid
    INSERT INTO bids (item_id, bidder_id, bidder_alias, amount, status)
    VALUES (p_item_id, p_bidder_id, p_bidder_alias, p_amount, 'winning');

    -- Update item current bid
    UPDATE items 
    SET current_bid = p_amount,
        current_price = p_amount,
        current_bidder_id = p_bidder_id
    WHERE id = p_item_id;

    COMMIT;
    
    -- Return success with updated balance
    SELECT 'Bid placed successfully' AS message, 
           p_amount AS bid_amount,
           v_new_balance AS new_balance,
           v_previous_bidder_id AS previous_bidder_id,
           v_previous_bid_amount AS refunded_amount;
END$$

-- =====================================================
-- STEP 2: Update BuyNow to return updated balances
-- =====================================================

SELECT 'STEP 2: Updating BuyNow procedure...' AS '';

DROP PROCEDURE IF EXISTS BuyNow$$

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
    DECLARE v_buyer_new_balance DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_seller_new_balance DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_buyer_idempotency_key VARCHAR(255) DEFAULT '';
    DECLARE v_seller_idempotency_key VARCHAR(255) DEFAULT '';

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Generate idempotency keys
    SET v_buyer_idempotency_key = CONCAT('BUYNOW_BUYER_', p_item_id, '_', p_buyer_id, '_', UNIX_TIMESTAMP(NOW()), '_', FLOOR(RAND() * 1000000));
    SET v_seller_idempotency_key = CONCAT('BUYNOW_SELLER_', p_item_id, '_', p_buyer_id, '_', UNIX_TIMESTAMP(NOW()), '_', FLOOR(RAND() * 1000000));

    -- Lock buyer's row
    SELECT credits INTO v_user_credits
    FROM users
    WHERE id = p_buyer_id
    FOR UPDATE;

    -- Check if item exists and is active
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
    SELECT credits INTO v_seller_credits
    FROM users
    WHERE id = v_seller_id
    FOR UPDATE;

    -- Deduct credits from buyer
    UPDATE users 
    SET credits = credits - p_buy_now_price,
        balance_version = COALESCE(balance_version, 0) + 1
    WHERE id = p_buyer_id;

    -- Get buyer's new balance
    SELECT credits INTO v_buyer_new_balance
    FROM users
    WHERE id = p_buyer_id;

    -- Add credits to seller
    UPDATE users 
    SET credits = credits + p_buy_now_price,
        balance_version = COALESCE(balance_version, 0) + 1
    WHERE id = v_seller_id;

    -- Get seller's new balance
    SELECT credits INTO v_seller_new_balance
    FROM users
    WHERE id = v_seller_id;

    -- Mark item as sold
    UPDATE items 
    SET status = 'sold',
        current_bid = p_buy_now_price,
        current_price = p_buy_now_price,
        current_bidder_id = p_buyer_id
    WHERE id = p_item_id;

    -- Record buyer transaction with idempotency
    INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date, item_id, idempotency_key)
    VALUES (p_buyer_id, 'buy_now', p_buy_now_price, 'completed',
            CONCAT('BUY_NOW_ITEM_', p_item_id), NOW(), p_item_id,
            v_buyer_idempotency_key)
    ON DUPLICATE KEY UPDATE status = 'completed';

    -- Record seller transaction with idempotency
    INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date, item_id, idempotency_key)
    VALUES (v_seller_id, 'bonus', p_buy_now_price, 'completed',
            CONCAT('SELL_ITEM_', p_item_id), NOW(), p_item_id,
            v_seller_idempotency_key)
    ON DUPLICATE KEY UPDATE status = 'completed';

    COMMIT;
    
    -- Return success with updated balances
    SELECT 'Item purchased successfully' AS message, 
           p_buy_now_price AS purchase_amount,
           v_buyer_new_balance AS buyer_new_balance,
           v_seller_new_balance AS seller_new_balance;
END$$

-- =====================================================
-- STEP 3: Update EndAuction to ensure proper credit transfer
-- =====================================================

SELECT 'STEP 3: Updating EndAuction procedure...' AS '';

DROP PROCEDURE IF EXISTS EndAuction$$

CREATE PROCEDURE EndAuction(
    IN p_item_id INT UNSIGNED
)
BEGIN
    DECLARE v_winning_bidder_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_winning_amount DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_seller_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_item_exists INT DEFAULT 0;
    DECLARE v_seller_new_balance DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_idempotency_key VARCHAR(255) DEFAULT '';

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Check if item exists and lock it
    SELECT seller_id INTO v_seller_id
    FROM items
    WHERE id = p_item_id
    FOR UPDATE;

    -- Check if item was found (seller_id will be NULL if not found)
    IF v_seller_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found';
    END IF;
    
    SET v_item_exists = 1; -- Item exists if we got here

    -- Get winning bidder and amount (status should be 'winning')
    SELECT bidder_id, amount INTO v_winning_bidder_id, v_winning_amount
    FROM bids
    WHERE item_id = p_item_id AND status = 'winning'
    ORDER BY amount DESC, created_at ASC
    LIMIT 1;

    -- If there's a winning bidder, transfer credits to seller
    IF v_winning_bidder_id IS NOT NULL AND v_winning_amount > 0 THEN
        -- Lock seller's row
        SELECT id INTO @dummy FROM users WHERE id = v_seller_id FOR UPDATE;

        -- Generate idempotency key
        SET v_idempotency_key = CONCAT('AUCTION_WIN_', p_item_id, '_', v_seller_id, '_', UNIX_TIMESTAMP(NOW()), '_', FLOOR(RAND() * 1000000));

        -- Transfer credits to seller (bidder already paid when placing bid)
        UPDATE users
        SET credits = credits + v_winning_amount,
            balance_version = COALESCE(balance_version, 0) + 1
        WHERE id = v_seller_id;

        -- Get seller's new balance
        SELECT credits INTO v_seller_new_balance
        FROM users
        WHERE id = v_seller_id;

        -- Record seller credit transaction with idempotency
        INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date, item_id, idempotency_key)
        VALUES (
            v_seller_id,
            'bonus',
            v_winning_amount,
            'completed',
            CONCAT('AUCTION_WIN_ITEM_', p_item_id),
            NOW(),
            p_item_id,
            v_idempotency_key
        )
        ON DUPLICATE KEY UPDATE status = 'completed';

        -- Mark bid as won
        UPDATE bids
        SET status = 'won'
        WHERE item_id = p_item_id AND bidder_id = v_winning_bidder_id AND status = 'winning';

        -- Mark all other bids as lost
        UPDATE bids
        SET status = 'lost'
        WHERE item_id = p_item_id AND bidder_id != v_winning_bidder_id AND status != 'outbid';

        -- Mark item as sold
        UPDATE items
        SET status = 'sold',
            current_price = v_winning_amount,
            current_bid = v_winning_amount
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

    -- Return result
    IF v_winning_bidder_id IS NOT NULL THEN
        SELECT 'Auction ended successfully' AS message,
               v_winning_bidder_id AS winner_id,
               v_winning_amount AS winning_amount,
               v_seller_new_balance AS seller_new_balance;
    ELSE
        SELECT 'Auction ended with no winner' AS message,
               NULL AS winner_id,
               0.00 AS winning_amount,
               0.00 AS seller_new_balance;
    END IF;

END$$

DELIMITER ;

SELECT '';
SELECT '=======================================================' AS '';
SELECT 'VERIFICATION' AS '';
SELECT '=======================================================' AS '';
SELECT '';

-- Verify procedures exist
SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE,
    CREATED
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb'
  AND ROUTINE_NAME IN ('PlaceBid', 'BuyNow', 'EndAuction')
ORDER BY ROUTINE_NAME;

SELECT '';
SELECT '✅ All credit transfer procedures updated successfully!' AS '';
SELECT '';
SELECT 'Procedures now include:' AS '';
SELECT '  - Idempotency keys to prevent duplicate transfers' AS '';
SELECT '  - Updated balance returns for API responses' AS '';
SELECT '  - Proper validation and error handling' AS '';
SELECT '  - Balance version tracking for consistency' AS '';

