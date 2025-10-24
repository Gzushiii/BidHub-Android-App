-- Fix the bidding validation logic issue
-- This addresses the "Insufficient credits" error when bid amount is too low

USE defaultdb;

-- ==============================================
-- ROOT CAUSE: VALIDATION ORDER ISSUE
-- ==============================================

-- The problem is that the backend is checking credits BEFORE validating bid amount
-- This causes "Insufficient credits" instead of "Bid must be higher than current bid"

-- ==============================================
-- SOLUTION 1: Fix the backend validation order
-- ==============================================

-- The backend should validate in this order:
-- 1. Check if bid amount > current highest bid
-- 2. THEN check if user has sufficient credits
-- 3. THEN proceed with bid placement

-- ==============================================
-- SOLUTION 2: Improve PlaceBid procedure error messages
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
    
    -- Check if user exists
    SELECT COUNT(*) INTO v_user_exists 
    FROM users 
    WHERE id = p_bidder_id;
    
    IF v_user_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User not found';
    END IF;
    
    -- Get item starting price
    SELECT starting_price INTO v_starting_price 
    FROM items 
    WHERE id = p_item_id;
    
    -- Get current highest bid for the item
    SELECT COALESCE(MAX(amount), 0) INTO v_current_bid 
    FROM bids 
    WHERE item_id = p_item_id AND status IN ('active', 'winning');
    
    -- Use the higher of starting price or current highest bid
    SET v_current_bid = GREATEST(v_current_bid, v_starting_price);
    
    -- CRITICAL FIX: Check bid amount FIRST, before credit check
    IF p_amount <= v_current_bid THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = CONCAT('Bid must be higher than current highest bid (₱', v_current_bid, '). Your bid: ₱', p_amount);
    END IF;
    
    -- Get user's current credit balance
    SELECT credits INTO v_user_credits 
    FROM users 
    WHERE id = p_bidder_id;
    
    -- Check if user has sufficient credits
    IF v_user_credits < p_amount THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = CONCAT('Insufficient credits. Required: ₱', p_amount, ', Available: ₱', v_user_credits);
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
-- SOLUTION 3: Add better error handling in backend
-- ==============================================

-- The backend should also be updated to handle these errors properly:
-- 1. Catch "Bid must be higher" errors and show appropriate message
-- 2. Catch "Insufficient credits" errors and show appropriate message
-- 3. Don't show "Insufficient credits" when the real issue is bid amount too low

-- ==============================================
-- VERIFICATION
-- ==============================================

SELECT '=== VERIFICATION: PLACEBID PROCEDURE UPDATED ===' as section;
SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE,
    CREATED,
    LAST_ALTERED
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- ==============================================
-- TEST SCENARIO
-- ==============================================

SELECT '=== TEST SCENARIO ===' as section;
SELECT 
    'User tries to bid ₱100 on item with ₱1,000 current bid' as scenario,
    'Expected: "Bid must be higher than current highest bid (₱1000). Your bid: ₱100"' as expected_error,
    'Previous: "Insufficient credits" (incorrect)' as previous_error;
