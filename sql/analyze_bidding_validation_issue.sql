-- Analyze the bidding validation issue causing "Insufficient credits" error
-- This script identifies the exact problem in the validation flow

USE defaultdb;

-- ==============================================
-- PROBLEM ANALYSIS
-- ==============================================

SELECT '=== BIDDING VALIDATION ISSUE ANALYSIS ===' as section;

-- The issue is in the validation order:
-- 1. User tries to bid ₱100
-- 2. Current bid is ₱1,000
-- 3. System should reject with "Bid must be higher than current bid"
-- 4. But instead it shows "Insufficient credits"

-- This suggests the validation is happening in the wrong order or the wrong amount is being checked

-- ==============================================
-- CHECK CURRENT BIDDING STATE
-- ==============================================

SELECT '=== CURRENT BIDDING STATE ===' as section;

-- Check if there are any active items with bids
SELECT 
    i.id as item_id,
    i.title,
    i.starting_price,
    i.current_price,
    i.status,
    i.end_date,
    COUNT(b.id) as bid_count,
    MAX(b.amount) as highest_bid
FROM items i
LEFT JOIN bids b ON i.id = b.item_id
WHERE i.status = 'active'
GROUP BY i.id, i.title, i.starting_price, i.current_price, i.status, i.end_date
ORDER BY i.created_at DESC
LIMIT 5;

-- Check recent bids
SELECT '=== RECENT BIDS ===' as section;
SELECT 
    b.id as bid_id,
    b.item_id,
    b.amount,
    b.status,
    b.created_at,
    u.email as bidder_email,
    i.title as item_title
FROM bids b
JOIN users u ON b.bidder_id = u.id
JOIN items i ON b.item_id = i.id
ORDER BY b.created_at DESC
LIMIT 10;

-- ==============================================
-- VALIDATION LOGIC ANALYSIS
-- ==============================================

SELECT '=== VALIDATION LOGIC ANALYSIS ===' as section;

-- The PlaceBid procedure checks:
-- 1. IF p_amount <= v_current_bid THEN "Bid amount must be higher than current highest bid"
-- 2. IF v_user_credits < p_amount THEN "Insufficient credits"

-- But the issue might be:
-- 1. The current_bid is not being calculated correctly
-- 2. The user's credit balance is being checked against the wrong amount
-- 3. The validation order is wrong

-- Let's check what the current highest bid actually is for active items
SELECT '=== CURRENT HIGHEST BIDS FOR ACTIVE ITEMS ===' as section;
SELECT 
    i.id as item_id,
    i.title,
    i.current_price as item_current_price,
    COALESCE(MAX(b.amount), 0) as actual_highest_bid,
    CASE 
        WHEN COALESCE(MAX(b.amount), 0) = 0 THEN 'No bids yet'
        WHEN i.current_price = COALESCE(MAX(b.amount), 0) THEN 'Prices match'
        ELSE 'MISMATCH - Item price does not match highest bid'
    END as price_consistency
FROM items i
LEFT JOIN bids b ON i.id = b.item_id AND b.status IN ('active', 'winning')
WHERE i.status = 'active'
GROUP BY i.id, i.title, i.current_price
ORDER BY i.created_at DESC;

-- ==============================================
-- USER CREDIT ANALYSIS
-- ==============================================

SELECT '=== USER CREDIT ANALYSIS ===' as section;
SELECT 
    u.id,
    u.email,
    u.alias,
    u.credits,
    COUNT(b.id) as total_bids,
    SUM(b.amount) as total_bid_amount
FROM users u
LEFT JOIN bids b ON u.id = b.bidder_id
WHERE u.credits > 0
GROUP BY u.id, u.email, u.alias, u.credits
ORDER BY u.credits DESC
LIMIT 10;

-- ==============================================
-- POTENTIAL ISSUES IDENTIFIED
-- ==============================================

SELECT '=== POTENTIAL ISSUES IDENTIFIED ===' as section;

-- Issue 1: Bid amount validation might be bypassed
SELECT 'Issue 1: Bid amount validation bypassed' as issue,
'User tries to bid ₱100 on item with ₱1,000 current bid' as scenario,
'System should reject with "Bid must be higher" but shows "Insufficient credits"' as problem;

-- Issue 2: Credit check happening before bid validation
SELECT 'Issue 2: Credit check before bid validation' as issue,
'Credit balance checked against ₱100 instead of required ₱1,001+' as scenario,
'User has insufficient credits for the required minimum bid' as problem;

-- Issue 3: Current bid calculation incorrect
SELECT 'Issue 3: Current bid calculation incorrect' as issue,
'System might be using wrong current bid value' as scenario,
'Validation logic uses incorrect bid amount for comparison' as problem;

-- Issue 4: PlaceBid procedure not being called
SELECT 'Issue 4: PlaceBid procedure not being called' as issue,
'Backend validation might be bypassing stored procedure' as scenario,
'Direct credit check without proper bid validation' as problem;
