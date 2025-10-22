-- Debug with actual user data from the screenshots
USE defaultdb;

-- Step 1: Find the user with 2599 credits (from the screenshot)
SELECT '=== USER WITH 2599 CREDITS ===' as section;
SELECT id, email, alias, credits 
FROM users 
WHERE credits >= 2500 
ORDER BY credits DESC;

-- Step 2: Find the bunny item (item_id 2 from previous tests)
SELECT '=== BUNNY ITEM ===' as section;
SELECT id, title, starting_price, current_price, status, seller_id, end_date
FROM items 
WHERE title LIKE '%bunny%' OR title LIKE '%Bunny%' OR id = 2;

-- Step 3: Check current bids for this item
SELECT '=== CURRENT BIDS FOR BUNNY ===' as section;
SELECT * FROM bids 
WHERE item_id = 2 
ORDER BY created_at DESC;

-- Step 4: Test PlaceBid with actual data
-- Get the user with highest credits
SET @test_user_id = (SELECT id FROM users WHERE credits >= 2500 ORDER BY credits DESC LIMIT 1);
SET @test_user_email = (SELECT email FROM users WHERE id = @test_user_id);
SET @test_user_alias = (SELECT alias FROM users WHERE id = @test_user_id);
SET @test_user_credits = (SELECT credits FROM users WHERE id = @test_user_id);

-- Get the bunny item
SET @test_item_id = 2; -- From previous tests
SET @test_item_title = (SELECT title FROM items WHERE id = @test_item_id);
SET @test_item_current_price = (SELECT current_price FROM items WHERE id = @test_item_id);
SET @test_item_seller_id = (SELECT seller_id FROM items WHERE id = @test_item_id);

-- Set bid amount (101.00 from screenshot)
SET @test_bid_amount = 101.00;

SELECT CONCAT('Test User ID: ', @test_user_id) as test_info;
SELECT CONCAT('Test User Email: ', @test_user_email) as test_info;
SELECT CONCAT('Test User Alias: ', @test_user_alias) as test_info;
SELECT CONCAT('Test User Credits: ', @test_user_credits) as test_info;
SELECT CONCAT('Test Item ID: ', @test_item_id) as test_info;
SELECT CONCAT('Test Item Title: ', @test_item_title) as test_info;
SELECT CONCAT('Test Item Current Price: ', @test_item_current_price) as test_info;
SELECT CONCAT('Test Item Seller ID: ', @test_item_seller_id) as test_info;
SELECT CONCAT('Test Bid Amount: ', @test_bid_amount) as test_info;

-- Step 5: Check if bidder is not the seller
SELECT '=== SELLER CHECK ===' as section;
SELECT 
    CASE 
        WHEN @test_user_id = @test_item_seller_id THEN 'ERROR: User is the seller - cannot bid on own item'
        ELSE 'OK: User is not the seller'
    END as seller_check;

-- Step 6: Check if bid amount is higher than current price
SELECT '=== BID AMOUNT CHECK ===' as section;
SELECT 
    CASE 
        WHEN @test_bid_amount <= @test_item_current_price THEN CONCAT('ERROR: Bid amount (', @test_bid_amount, ') must be higher than current price (', @test_item_current_price, ')')
        ELSE CONCAT('OK: Bid amount (', @test_bid_amount, ') is higher than current price (', @test_item_current_price, ')')
    END as bid_amount_check;

-- Step 7: Check if user has sufficient credits
SELECT '=== CREDIT CHECK ===' as section;
SELECT 
    CASE 
        WHEN @test_user_credits < @test_bid_amount THEN CONCAT('ERROR: Insufficient credits - need ', @test_bid_amount, ', have ', @test_user_credits)
        ELSE CONCAT('OK: Sufficient credits - need ', @test_bid_amount, ', have ', @test_user_credits)
    END as credit_check;

-- Step 8: Try to place the bid
SELECT '=== ATTEMPTING BID ===' as section;
CALL PlaceBid(@test_item_id, @test_user_id, @test_bid_amount, @test_user_alias);

-- Step 9: Check results
SELECT '=== RESULTS AFTER BID ===' as section;
SELECT id, email, alias, credits FROM users WHERE id = @test_user_id;
SELECT id, title, current_price, current_bidder_id FROM items WHERE id = @test_item_id;
SELECT * FROM bids WHERE item_id = @test_item_id ORDER BY created_at DESC LIMIT 3;
