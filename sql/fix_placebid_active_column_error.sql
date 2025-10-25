-- Fix PlaceBid stored procedure - "Unknown column 'active'" error
-- This script drops and recreates the PlaceBid procedure with correct column names

USE defaultdb;

-- Drop existing procedure
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

    -- Check if item exists and is active (using 'status' column, not 'active')
    SELECT COUNT(*), seller_id, starting_price
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

    -- Check if user has enough credits
    IF v_user_credits < p_amount THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits for bidding';
    END IF;

    -- Get current highest bid
    SELECT COALESCE(MAX(amount), 0) INTO v_current_bid
    FROM bids
    WHERE item_id = p_item_id AND status IN ('active', 'winning');

    -- Validate bid amount
    IF p_amount <= v_current_bid THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid must be higher than current bid';
    END IF;

    IF p_amount < v_starting_price THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid must be at least the starting price';
    END IF;

    -- Get previous winning bidder
    SELECT bidder_id, amount INTO v_previous_bidder_id, v_previous_bid_amount
    FROM bids
    WHERE item_id = p_item_id AND status = 'winning'
    LIMIT 1;

    -- Refund previous bidder if exists
    IF v_previous_bidder_id IS NOT NULL THEN
        UPDATE users
        SET credits = credits + v_previous_bid_amount
        WHERE id = v_previous_bidder_id;

        UPDATE bids
        SET status = 'outbid'
        WHERE item_id = p_item_id AND bidder_id = v_previous_bidder_id AND status = 'winning';

        INSERT INTO credit_transactions (user_id, amount, transaction_type, description, created_at)
        VALUES (v_previous_bidder_id, v_previous_bid_amount, 'refund', 
                CONCAT('Refund for being outbid on item ', p_item_id), NOW());
    END IF;

    -- Deduct credits from new bidder
    UPDATE users
    SET credits = credits - p_amount
    WHERE id = p_bidder_id;

    -- Record credit transaction
    INSERT INTO credit_transactions (user_id, amount, transaction_type, description, created_at)
    VALUES (p_bidder_id, -p_amount, 'bid', 
            CONCAT('Bid placed on item ', p_item_id), NOW());

    -- Insert new bid
    INSERT INTO bids (item_id, bidder_id, bidder_alias, amount, status, created_at)
    VALUES (p_item_id, p_bidder_id, p_bidder_alias, p_amount, 'winning', NOW());

    -- Update item's current price
    UPDATE items
    SET current_price = p_amount,
        current_bidder_id = p_bidder_id,
        updated_at = NOW()
    WHERE id = p_item_id;

    COMMIT;
END //

DELIMITER ;

-- Verify procedure was created
SELECT 'PlaceBid procedure recreated successfully' AS status;
SHOW PROCEDURE STATUS WHERE Name = 'PlaceBid';

