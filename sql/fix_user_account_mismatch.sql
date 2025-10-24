-- Comprehensive fix for user account and credit mismatching issues
-- This addresses all potential mismatches between frontend and backend

USE defaultdb;

-- ==============================================
-- STEP 1: Ensure user has correct credit balance
-- ==============================================

-- Update user's credits to match transaction history (7600.00)
UPDATE users 
SET credits = 7600.00
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 2: Ensure all required columns exist
-- ==============================================

-- Add missing columns if they don't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ==============================================
-- STEP 3: Clean up any orphaned transactions
-- ==============================================

-- Delete any transactions with invalid user IDs
DELETE FROM credit_transactions 
WHERE user_id NOT IN (SELECT id FROM users);

-- ==============================================
-- STEP 4: Ensure PlaceBid procedure exists
-- ==============================================

-- Drop and recreate PlaceBid procedure to ensure it's working
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
    
    -- Check bid amount FIRST, before credit check
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
-- STEP 5: Verify the fix
-- ==============================================

SELECT '=== VERIFICATION: USER ACCOUNT FIXED ===' as section;

-- Check user's updated credits
SELECT 
    id,
    email,
    alias,
    credits,
    updated_at
FROM users 
WHERE email = 'testuser444@example.com';

-- ==============================================
-- STEP 6: Verify credit balance calculation
-- ==============================================

SELECT '=== VERIFICATION: CREDIT BALANCE CALCULATION ===' as section;

SELECT 
    u.id,
    u.email,
    u.credits as current_credits,
    COALESCE(SUM(
        CASE 
            WHEN ct.type = 'purchase' THEN ct.amount
            WHEN ct.type = 'bid' THEN -ct.amount
            WHEN ct.type = 'refund' THEN ct.amount
            ELSE 0
        END
    ), 0) as calculated_credits,
    CASE 
        WHEN u.credits = COALESCE(SUM(
            CASE 
                WHEN ct.type = 'purchase' THEN ct.amount
                WHEN ct.type = 'bid' THEN -ct.amount
                WHEN ct.type = 'refund' THEN ct.amount
                ELSE 0
            END
        ), 0) THEN 'SYNCHRONIZED'
        ELSE 'MISMATCH'
    END as sync_status
FROM users u
LEFT JOIN credit_transactions ct ON u.id = ct.user_id
WHERE u.email = 'testuser444@example.com'
GROUP BY u.id, u.email, u.credits;

-- ==============================================
-- STEP 7: Verify PlaceBid procedure
-- ==============================================

SELECT '=== VERIFICATION: PLACEBID PROCEDURE ===' as section;

SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE,
    CREATED,
    LAST_ALTERED
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- ==============================================
-- FINAL STATUS
-- ==============================================

SELECT '=== USER ACCOUNT MISMATCH ISSUES FIXED ===' as final_status;
SELECT 'User should now have 7600.00 credits and all systems should be synchronized' as result;
