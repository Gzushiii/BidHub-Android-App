-- Test credit validation with actual user data
USE defaultdb;

-- Step 1: Create bids table if it doesn't exist
CREATE TABLE IF NOT EXISTS bids (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id INT UNSIGNED NOT NULL,
    bidder_id INT UNSIGNED NOT NULL,
    bidder_alias VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('active', 'outbid', 'winning', 'won', 'lost', 'cancelled') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_bids_item_id (item_id),
    INDEX idx_bids_bidder_id (bidder_id),
    INDEX idx_bids_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Check user with highest credits
SELECT '=== USER WITH HIGHEST CREDITS ===' as section;
SELECT id, email, alias, credits FROM users ORDER BY credits DESC LIMIT 3;

-- Step 3: Check active items
SELECT '=== ACTIVE ITEMS ===' as section;
SELECT id, title, starting_price, current_price, status, seller_id FROM items WHERE status = 'active' LIMIT 3;

-- Step 4: Test PlaceBid procedure with actual data
-- First, let's get a user ID and item ID
SELECT '=== TEST DATA PREPARATION ===' as section;
SET @test_user_id = (SELECT id FROM users ORDER BY credits DESC LIMIT 1);
SET @test_item_id = (SELECT id FROM items WHERE status = 'active' LIMIT 1);
SET @test_amount = 50.00;
SET @test_alias = (SELECT alias FROM users WHERE id = @test_user_id);

SELECT CONCAT('User ID: ', @test_user_id) as test_info;
SELECT CONCAT('Item ID: ', @test_item_id) as test_info;
SELECT CONCAT('Amount: ', @test_amount) as test_info;
SELECT CONCAT('Alias: ', @test_alias) as test_info;

-- Step 5: Check user credits before test
SELECT '=== USER CREDITS BEFORE TEST ===' as section;
SELECT id, email, credits FROM users WHERE id = @test_user_id;

-- Step 6: Test PlaceBid procedure (commented out for safety)
-- CALL PlaceBid(@test_item_id, @test_user_id, @test_amount, @test_alias);

-- Step 7: Check if procedure exists
SELECT '=== PLACEBID PROCEDURE CHECK ===' as section;
SELECT ROUTINE_NAME, ROUTINE_TYPE 
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';
