-- Fix BuyNow stored procedure to handle INT item_id properly
-- This ensures the procedure accepts numeric INT IDs instead of UUIDs

DELIMITER $$

-- Drop existing procedure
DROP PROCEDURE IF EXISTS BuyNow$$

-- Recreate BuyNow with proper INT handling
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

-- Test the procedure
SELECT 'BuyNow procedure recreated successfully' as message;
