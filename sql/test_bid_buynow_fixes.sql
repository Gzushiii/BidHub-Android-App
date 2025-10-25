-- Test script to verify that the bid and buy-now fixes are working
-- This script tests both procedures with sample data

-- First, let's check what items exist
SELECT '=== EXISTING ITEMS ===' as section;
SELECT id, uuid_id, title, status, current_bid, buy_now_price, seller_id 
FROM items 
WHERE status = 'active' 
LIMIT 5;

-- Check what users exist
SELECT '=== EXISTING USERS ===' as section;
SELECT id, username, email, credits 
FROM users 
LIMIT 5;

-- Test PlaceBid procedure (if we have test data)
SELECT '=== TESTING PLACEBID PROCEDURE ===' as section;

-- Example test (uncomment and modify with actual IDs):
-- CALL PlaceBid(1, 2, 100.00, 'test_bidder');

-- Test BuyNow procedure (if we have test data)
SELECT '=== TESTING BUYNOW PROCEDURE ===' as section;

-- Example test (uncomment and modify with actual IDs):
-- CALL BuyNow(1, 2, 200.00);

-- Check procedure definitions
SELECT '=== PROCEDURE DEFINITIONS ===' as section;
SHOW CREATE PROCEDURE PlaceBid;
SHOW CREATE PROCEDURE BuyNow;

-- Check if procedures exist
SELECT '=== PROCEDURE STATUS ===' as section;
SELECT 
  ROUTINE_NAME,
  ROUTINE_TYPE,
  CREATED,
  LAST_ALTERED
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = DATABASE() 
  AND ROUTINE_NAME IN ('PlaceBid', 'BuyNow');

-- Check for any recent errors in error log (if accessible)
SELECT '=== RECENT ACTIVITY ===' as section;
SELECT 'Check application logs for recent bid/buy-now attempts' as note;

-- Summary
SELECT '=== SUMMARY ===' as section;
SELECT 'If procedures exist and have correct signatures, the fixes are applied' as status;
SELECT 'Test with actual item and user IDs from your database' as next_step;
