-- Quick test to verify the fixes
USE defaultdb;

-- Step 1: Check if we have test data
SELECT '=== QUICK TEST ===' as section;

-- Check users
SELECT 'Users with credits:' as info;
SELECT id, email, alias, credits FROM users WHERE credits > 0 LIMIT 3;

-- Check active items
SELECT 'Active items:' as info;
SELECT id, title, current_price, seller_id FROM items WHERE status = 'active' LIMIT 3;

-- Check if PlaceBid procedure exists
SELECT 'PlaceBid procedure exists:' as info;
SELECT COUNT(*) as procedure_count 
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- Step 2: Test a simple bid (if we have data)
-- This will only run if we have a user with credits and an active item
SET @test_user = (SELECT id FROM users WHERE credits > 100 LIMIT 1);
SET @test_item = (SELECT id FROM items WHERE status = 'active' AND seller_id != @test_user LIMIT 1);

SELECT CONCAT('Test user ID: ', @test_user) as test_info;
SELECT CONCAT('Test item ID: ', @test_item) as test_info;

-- Check if we have suitable test data
SELECT 
    CASE 
        WHEN @test_user IS NOT NULL AND @test_item IS NOT NULL THEN 'Suitable test data found'
        ELSE 'No suitable test data found'
    END as test_data_status;

-- If we have both, try a test bid (commented out for safety)
-- Uncomment the next line to test PlaceBid procedure:
-- CALL PlaceBid(@test_item, @test_user, 50.00, 'testuser');
