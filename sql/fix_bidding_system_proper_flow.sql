-- Fix bidding system to implement proper credit flow
-- This implements the correct bidding behavior as described

USE defaultdb;

-- ==============================================
-- STEP 1: Drop and recreate PlaceBid procedure with proper outbid logic
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
    DECLARE v_previous_bidder_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_previous_bid_amount DECIMAL(10,2) DEFAULT 0.00;
    
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
    
    -- Get previous bidder info for refund
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
    
    -- Get user's current credit balance
    SELECT credits INTO v_user_credits 
    FROM users 
    WHERE id = p_bidder_id;
    
    -- Check if user has sufficient credits
    IF v_user_credits < p_amount THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits';
    END IF;
    
    -- CRITICAL: Refund previous bidder if they exist
    IF v_previous_bidder_id IS NOT NULL AND v_previous_bidder_id != p_bidder_id THEN
        -- Refund the previous bidder
        UPDATE users 
        SET credits = credits + v_previous_bid_amount 
        WHERE id = v_previous_bidder_id;
        
        -- Record refund transaction
        INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date)
        VALUES (v_previous_bidder_id, 'refund', v_previous_bid_amount, 'completed', 'OUTBID_REFUND', NOW());
        
        -- Mark previous bid as outbid
        UPDATE bids 
        SET status = 'outbid' 
        WHERE item_id = p_item_id 
        AND bidder_id = v_previous_bidder_id 
        AND status = 'winning';
    END IF;
    
    -- Deduct credits from new bidder
    UPDATE users 
    SET credits = credits - p_amount 
    WHERE id = p_bidder_id;
    
    -- Insert new bid
    INSERT INTO bids (item_id, bidder_id, bidder_alias, amount, status)
    VALUES (p_item_id, p_bidder_id, p_bidder_alias, p_amount, 'winning');
    
    -- Update item's current price
    UPDATE items 
    SET current_price = p_amount,
        current_bidder_id = p_bidder_id
    WHERE id = p_item_id;
    
    -- Record bid transaction
    INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date)
    VALUES (p_bidder_id, 'bid', p_amount, 'completed', 'BID', NOW());
    
    COMMIT;
    
END //

DELIMITER ;

-- ==============================================
-- STEP 2: Create procedure to handle auction end and transfer credits to seller
-- ==============================================

DROP PROCEDURE IF EXISTS EndAuction;

DELIMITER //

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
    
    -- Check if item exists
    SELECT COUNT(*) INTO v_item_exists 
    FROM items 
    WHERE id = p_item_id;
    
    IF v_item_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found';
    END IF;
    
    -- Get winning bidder and amount
    SELECT bidder_id, amount INTO v_winning_bidder_id, v_winning_amount
    FROM bids 
    WHERE item_id = p_item_id AND status = 'winning'
    LIMIT 1;
    
    -- Get seller ID
    SELECT seller_id INTO v_seller_id
    FROM items 
    WHERE id = p_item_id;
    
    -- If there's a winning bidder, transfer credits to seller
    IF v_winning_bidder_id IS NOT NULL THEN
        -- Transfer credits to seller
        UPDATE users 
        SET credits = credits + v_winning_amount 
        WHERE id = v_seller_id;
        
        -- Record seller credit transaction
        INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date)
        VALUES (v_seller_id, 'bonus', v_winning_amount, 'completed', 'AUCTION_WIN', NOW());
        
        -- Mark bid as won
        UPDATE bids 
        SET status = 'won' 
        WHERE item_id = p_item_id AND bidder_id = v_winning_bidder_id;
        
        -- Mark item as sold
        UPDATE items 
        SET status = 'sold' 
        WHERE id = p_item_id;
    ELSE
        -- No winning bidder, mark item as ended
        UPDATE items 
        SET status = 'ended' 
        WHERE id = p_item_id;
    END IF;
    
    COMMIT;
    
END //

DELIMITER ;

-- ==============================================
-- STEP 3: Create procedure for Buy Now functionality
-- ==============================================

DROP PROCEDURE IF EXISTS BuyNow;

DELIMITER //

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
    
    -- Get item status and seller
    SELECT status, seller_id INTO v_item_status, v_seller_id
    FROM items 
    WHERE id = p_item_id;
    
    IF v_item_status != 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item is not available for purchase';
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
    
    -- Get buyer's credit balance
    SELECT credits INTO v_buyer_credits 
    FROM users 
    WHERE id = p_buyer_id;
    
    -- Check if buyer has sufficient credits
    IF v_buyer_credits < p_buy_now_price THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits for buy now';
    END IF;
    
    -- Deduct credits from buyer
    UPDATE users 
    SET credits = credits - p_buy_now_price 
    WHERE id = p_buyer_id;
    
    -- Transfer credits to seller
    UPDATE users 
    SET credits = credits + p_buy_now_price 
    WHERE id = v_seller_id;
    
    -- Mark item as sold
    UPDATE items 
    SET status = 'sold',
        current_price = p_buy_now_price,
        current_bidder_id = p_buyer_id
    WHERE id = p_item_id;
    
    -- Record buyer transaction
    INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date)
    VALUES (p_buyer_id, 'purchase', p_buy_now_price, 'completed', 'BUY_NOW', NOW());
    
    -- Record seller transaction
    INSERT INTO credit_transactions (user_id, type, amount, status, reference, transaction_date)
    VALUES (v_seller_id, 'bonus', p_buy_now_price, 'completed', 'SELL_ITEM', NOW());
    
    COMMIT;
    
END //

DELIMITER ;

-- ==============================================
-- STEP 4: Verify the procedures were created
-- ==============================================

SELECT '=== VERIFICATION: BIDDING SYSTEM FIXED ===' as section;

-- Check if all procedures exist
SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME IN ('PlaceBid', 'EndAuction', 'BuyNow')
ORDER BY ROUTINE_NAME;

-- ==============================================
-- FINAL STATUS
-- ==============================================

SELECT '=== BIDDING SYSTEM PROPER FLOW IMPLEMENTED ===' as final_status;
SELECT 'All procedures created with proper credit flow logic' as result;
