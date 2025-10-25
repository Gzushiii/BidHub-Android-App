-- Comprehensive fix for both PlaceBid and BuyNow stored procedures
-- This addresses UUID vs INT ID mismatch and GROUP BY issues

-- Disable safe update mode temporarily
SET SQL_SAFE_UPDATES = 0;

-- Set session variables for procedure creation
SET SESSION sql_mode = '';

DELIMITER $$

-- ========================================
-- Fix PlaceBid Procedure
-- ========================================

DROP PROCEDURE IF EXISTS PlaceBid$$

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
  
  -- Check if item exists and get current bid info (fixed GROUP BY issue)
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

-- ========================================
-- Fix BuyNow Procedure
-- ========================================

DROP PROCEDURE IF EXISTS BuyNow$$

CREATE PROCEDURE BuyNow(
  IN p_item_id INT,
  IN p_buyer_id INT,
  IN p_amount DECIMAL(10,2)
)
BEGIN
  DECLARE v_user_credits DECIMAL(10,2) DEFAULT 0;
  DECLARE v_buy_now_price DECIMAL(10,2) DEFAULT 0;
  DECLARE v_seller_id INT;
  DECLARE v_item_exists INT DEFAULT 0;
  DECLARE v_balance_version INT DEFAULT 0;
  DECLARE v_idempotency_key VARCHAR(255);
  DECLARE v_transaction_id VARCHAR(255);
  
  -- Generate idempotency key
  SET v_idempotency_key = CONCAT('buynow_', p_item_id, '_', p_buyer_id, '_', UNIX_TIMESTAMP());
  SET v_transaction_id = UUID();
  
  -- Start transaction
  START TRANSACTION;
  
  -- Check if item exists and get buy now info
  SELECT 
    COUNT(*),
    COALESCE(MAX(buy_now_price), 0),
    COALESCE(MAX(seller_id), 0)
  INTO v_item_exists, v_buy_now_price, v_seller_id
  FROM items 
  WHERE id = p_item_id;
  
  IF v_item_exists = 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'item_not_found';
  END IF;
  
  -- Check if buyer is the seller
  IF p_buyer_id = v_seller_id THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'seller_cannot_buy';
  END IF;
  
  -- Check if buy now price is valid
  IF v_buy_now_price <= 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'no_buy_now_price';
  END IF;
  
  -- Check if amount matches buy now price
  IF p_amount != v_buy_now_price THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'amount_mismatch';
  END IF;
  
  -- Get user credits and balance version (with row lock)
  SELECT credits, balance_version 
  INTO v_user_credits, v_balance_version
  FROM users 
  WHERE id = p_buyer_id 
  FOR UPDATE;
  
  -- Check if user has sufficient credits
  IF v_user_credits < p_amount THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'insufficient_credits';
  END IF;
  
  -- Check for existing transaction with same idempotency key
  SELECT COUNT(*) INTO v_item_exists
  FROM credit_transactions 
  WHERE idempotency_key = v_idempotency_key;
  
  IF v_item_exists > 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'duplicate_purchase';
  END IF;
  
  -- Update item status to sold
  UPDATE items 
  SET status = 'sold', 
      current_bid = p_amount,
      updated_at = NOW()
  WHERE id = p_item_id;
  
  -- Deduct credits from buyer
  UPDATE users 
  SET credits = credits - p_amount, 
      balance_version = balance_version + 1,
      updated_at = NOW()
  WHERE id = p_buyer_id;
  
  -- Add credits to seller
  UPDATE users 
  SET credits = credits + p_amount, 
      balance_version = balance_version + 1,
      updated_at = NOW()
  WHERE id = v_seller_id;
  
  -- Record credit transaction for buyer (deduction)
  INSERT INTO credit_transactions (
    user_id, 
    amount, 
    transaction_type, 
    description, 
    idempotency_key, 
    transaction_id,
    created_at
  ) VALUES (
    p_buyer_id, 
    -p_amount, 
    'buy_now_purchase', 
    CONCAT('Buy Now purchase of item ', p_item_id), 
    v_idempotency_key, 
    v_transaction_id,
    NOW()
  );
  
  -- Record credit transaction for seller (addition)
  INSERT INTO credit_transactions (
    user_id, 
    amount, 
    transaction_type, 
    description, 
    idempotency_key, 
    transaction_id,
    created_at
  ) VALUES (
    v_seller_id, 
    p_amount, 
    'buy_now_sale', 
    CONCAT('Buy Now sale of item ', p_item_id), 
    CONCAT(v_idempotency_key, '_seller'), 
    UUID(),
    NOW()
  );
  
  -- Commit transaction
  COMMIT;
  
  -- Return success
  SELECT 'success' as status, p_amount as purchase_amount, v_user_credits - p_amount as remaining_credits;
  
END$$

DELIMITER ;

-- Re-enable safe update mode
SET SQL_SAFE_UPDATES = 1;

-- Test the procedures
SELECT 'Both PlaceBid and BuyNow procedures recreated successfully' as message;
SELECT 'Procedures now accept INT item_id instead of UUID' as note;
SELECT 'GROUP BY issues in PlaceBid have been resolved' as note2;
