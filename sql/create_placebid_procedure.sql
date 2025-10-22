-- Create PlaceBid stored procedure
-- This procedure handles bid placement with proper credit deduction and validation

USE defaultdb;

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
    DECLARE v_exit_handler INT DEFAULT 0;
    
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
    
    COMMIT;
    
END //

DELIMITER ;

-- Verify the procedure was created
SHOW PROCEDURE STATUS WHERE Name = 'PlaceBid';

