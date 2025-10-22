-- Debug PlaceBid procedure with actual data
USE defaultdb;

-- Step 1: Check current database state
SELECT '=== CURRENT DATABASE STATE ===' as section;

-- Check users with credits
SELECT '--- USERS WITH CREDITS ---' as subsection;
SELECT id, email, alias, credits, created_at 
FROM users 
WHERE credits > 0 
ORDER BY credits DESC;

-- Check active items
SELECT '--- ACTIVE ITEMS ---' as subsection;
SELECT id, title, starting_price, current_price, status, seller_id, end_date
FROM items 
WHERE status = 'active' 
ORDER BY created_at DESC 
LIMIT 5;

-- Check existing bids
SELECT '--- EXISTING BIDS ---' as subsection;
SELECT COUNT(*) as total_bids FROM bids;
SELECT * FROM bids ORDER BY created_at DESC LIMIT 3;

-- Step 2: Prepare test data
SELECT '=== PREPARING TEST DATA ===' as section;

-- Get a user with sufficient credits for testing
SET @test_user_id = (SELECT id FROM users WHERE credits > 100 ORDER BY credits DESC LIMIT 1);
SET @test_user_email = (SELECT email FROM users WHERE id = @test_user_id);
SET @test_user_alias = (SELECT alias FROM users WHERE id = @test_user_id);
SET @test_user_credits = (SELECT credits FROM users WHERE id = @test_user_id);

-- Get an active item for testing
SET @test_item_id = (SELECT id FROM items WHERE status = 'active' AND seller_id != @test_user_id LIMIT 1);
SET @test_item_title = (SELECT title FROM items WHERE id = @test_item_id);
SET @test_item_current_price = (SELECT current_price FROM items WHERE id = @test_item_id);
SET @test_item_seller_id = (SELECT seller_id FROM items WHERE id = @test_item_id);

-- Set test bid amount (higher than current price)
SET @test_bid_amount = @test_item_current_price + 100.00;

SELECT CONCAT('Test User ID: ', @test_user_id) as test_info;
SELECT CONCAT('Test User Email: ', @test_user_email) as test_info;
SELECT CONCAT('Test User Alias: ', @test_user_alias) as test_info;
SELECT CONCAT('Test User Credits: ', @test_user_credits) as test_info;
SELECT CONCAT('Test Item ID: ', @test_item_id) as test_info;
SELECT CONCAT('Test Item Title: ', @test_item_title) as test_info;
SELECT CONCAT('Test Item Current Price: ', @test_item_current_price) as test_info;
SELECT CONCAT('Test Item Seller ID: ', @test_item_seller_id) as test_info;
SELECT CONCAT('Test Bid Amount: ', @test_bid_amount) as test_info;

-- Step 3: Check if PlaceBid procedure exists
SELECT '=== PLACEBID PROCEDURE CHECK ===' as section;
SELECT ROUTINE_NAME, ROUTINE_TYPE, ROUTINE_DEFINITION
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- Step 4: Test PlaceBid procedure
SELECT '=== TESTING PLACEBID PROCEDURE ===' as section;

-- Show state before bid
SELECT '--- BEFORE BID ---' as subsection;
SELECT id, email, alias, credits FROM users WHERE id = @test_user_id;
SELECT id, title, current_price, current_bidder_id FROM items WHERE id = @test_item_id;
SELECT COUNT(*) as existing_bids FROM bids WHERE item_id = @test_item_id;

-- Call PlaceBid procedure
SELECT '--- CALLING PLACEBID PROCEDURE ---' as subsection;
CALL PlaceBid(@test_item_id, @test_user_id, @test_bid_amount, @test_user_alias);

-- Show state after bid
SELECT '--- AFTER BID ---' as subsection;
SELECT id, email, alias, credits FROM users WHERE id = @test_user_id;
SELECT id, title, current_price, current_bidder_id FROM items WHERE id = @test_item_id;
SELECT * FROM bids WHERE item_id = @test_item_id ORDER BY created_at DESC LIMIT 3;

-- Step 5: Test with insufficient credits scenario
SELECT '=== TESTING INSUFFICIENT CREDITS SCENARIO ===' as section;

-- Temporarily reduce user credits
UPDATE users SET credits = 10.00 WHERE id = @test_user_id;
SELECT '--- USER CREDITS REDUCED TO 10 ---' as subsection;
SELECT id, email, alias, credits FROM users WHERE id = @test_user_id;

-- Try to bid with insufficient credits
SELECT '--- ATTEMPTING BID WITH INSUFFICIENT CREDITS ---' as subsection;
CALL PlaceBid(@test_item_id, @test_user_id, 50.00, @test_user_alias);

-- Restore user credits
UPDATE users SET credits = @test_user_credits WHERE id = @test_user_id;
SELECT '--- USER CREDITS RESTORED ---' as subsection;
SELECT id, email, alias, credits FROM users WHERE id = @test_user_id;
