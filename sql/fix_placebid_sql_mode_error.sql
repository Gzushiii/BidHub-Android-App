-- =====================================================
-- FIX: PlaceBid SQL MODE ERROR
-- =====================================================
-- This script fixes the "only_full_group_by" SQL error
-- in the PlaceBid stored procedure
--
-- ERROR: "In aggregated query without GROUP BY, expression #2 
--         of SELECT list contains nonaggregated column 
--         'defaultdb.items.seller_id'"
--
-- ROOT CAUSE: Using COUNT(*) with non-aggregated columns
--             in the same SELECT statement
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'FIXING PlaceBid SQL MODE ERROR' AS '';
SELECT 'Starting at:', NOW() AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 1: DROP AND RECREATE PlaceBid PROCEDURE
-- =====================================================

SELECT 'STEP 1: Fixing PlaceBid procedure...' AS '';

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

END //

DELIMITER ;

SELECT '✓ PlaceBid procedure fixed - SQL mode error resolved' AS '';
SELECT '' AS '';

-- =====================================================
-- VERIFICATION
-- =====================================================

SELECT 'Verifying procedure creation...' AS '';

SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE,
    CREATED,
    LAST_ALTERED
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb'
AND ROUTINE_NAME = 'PlaceBid';

SELECT '' AS '';
SELECT '=======================================================' AS '';
SELECT 'FIX COMPLETE' AS '';
SELECT 'PlaceBid procedure has been updated to fix SQL mode error' AS '';
SELECT '=======================================================' AS '';

