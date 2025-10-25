-- Fix PlaceBid stored procedure to handle GROUP BY issue
-- This addresses the sql_mode=only_full_group_by error

DELIMITER $$

-- Drop existing procedure
DROP PROCEDURE IF EXISTS PlaceBid$$

-- Recreate PlaceBid with proper GROUP BY handling
CREATE PROCEDURE PlaceBid(
  IN p_item_id INT,
  IN p_bidder_id INT,
  IN p_amount DECIMAL(10,2),
  IN p_alias VARCHAR(64)
)
BEGIN
  DECLARE v_user_credits DECIMAL(10,2) DEFAULT 0;
  DECLARE v_current_bid DECIMAL(10,2) DEFAULT 0;
  DECLARE v_seller_id INT;
  DECLARE v_item_exists INT DEFAULT 0;
  DECLARE v_bid_count INT DEFAULT 0;
  DECLARE v_balance_version INT DEFAULT 0;
  DECLARE v_idempotency_key VARCHAR(255);
  DECLARE v_transaction_id VARCHAR(255);
  
  -- Generate idempotency key
  SET v_idempotency_key = CONCAT('bid_', p_item_id, '_', p_bidder_id, '_', UNIX_TIMESTAMP());
  SET v_transaction_id = UUID();
  
  -- Start transaction
  START TRANSACTION;
  
  -- Check if item exists and get current bid info
  SELECT 
    COUNT(*),
    COALESCE(MAX(current_bid), 0),
    COALESCE(MAX(seller_id), 0)
  INTO v_item_exists, v_current_bid, v_seller_id
  FROM items 
  WHERE id = p_item_id;
  
  IF v_item_exists = 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'item_not_found';
  END IF;
  
  -- Check if bidder is the seller
  IF p_bidder_id = v_seller_id THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'seller_cannot_bid';
  END IF;
  
  -- Check if bid amount is valid
  IF p_amount <= v_current_bid THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'bid_too_low';
  END IF;
  
  -- Get user credits and balance version (with row lock)
  SELECT credits, balance_version 
  INTO v_user_credits, v_balance_version
  FROM users 
  WHERE id = p_bidder_id 
  FOR UPDATE;
  
  -- Check if user has sufficient credits
  IF v_user_credits < p_amount THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'insufficient_credits';
  END IF;
  
  -- Check for existing bid with same idempotency key
  SELECT COUNT(*) INTO v_bid_count
  FROM credit_transactions 
  WHERE idempotency_key = v_idempotency_key;
  
  IF v_bid_count > 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'duplicate_bid';
  END IF;
  
  -- Insert the bid
  INSERT INTO bids (item_id, bidder_id, amount, alias, created_at)
  VALUES (p_item_id, p_bidder_id, p_amount, p_alias, NOW());
  
  -- Update item current bid
  UPDATE items 
  SET current_bid = p_amount, updated_at = NOW()
  WHERE id = p_item_id;
  
  -- Deduct credits from bidder
  UPDATE users 
  SET credits = credits - p_amount, 
      balance_version = balance_version + 1,
      updated_at = NOW()
  WHERE id = p_bidder_id;
  
  -- Record credit transaction
  INSERT INTO credit_transactions (
    user_id, 
    amount, 
    transaction_type, 
    description, 
    idempotency_key, 
    transaction_id,
    created_at
  ) VALUES (
    p_bidder_id, 
    -p_amount, 
    'bid_placed', 
    CONCAT('Bid placed on item ', p_item_id), 
    v_idempotency_key, 
    v_transaction_id,
    NOW()
  );
  
  -- Commit transaction
  COMMIT;
  
  -- Return success
  SELECT 'success' as status, p_amount as bid_amount, v_user_credits - p_amount as remaining_credits;
  
END$$

DELIMITER ;

-- Test the procedure
SELECT 'PlaceBid procedure recreated successfully' as message;
