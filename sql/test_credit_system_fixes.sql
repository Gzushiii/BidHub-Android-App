-- =====================================================
-- TEST SCRIPT FOR CREDIT SYSTEM FIXES
-- =====================================================
-- This script tests the key fixes implemented in fix_credit_system_comprehensive.sql
-- Run this after applying the comprehensive fix

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'TESTING CREDIT SYSTEM FIXES' AS '';
SELECT 'Starting at:', NOW() AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- =====================================================
-- TEST 1: VERIFY PROCEDURES EXIST
-- =====================================================

SELECT 'TEST 1: Verifying procedures exist...' AS '';

SELECT
    ROUTINE_NAME,
    ROUTINE_TYPE,
    LAST_ALTERED
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb'
AND ROUTINE_NAME IN ('PlaceBid', 'BuyNow', 'EndAuction')
ORDER BY ROUTINE_NAME;

SELECT '' AS '';

-- =====================================================
-- TEST 2: VERIFY NEW COLUMNS EXIST
-- =====================================================

SELECT 'TEST 2: Verifying new columns exist...' AS '';

-- Check for idempotency_key column
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '✓ idempotency_key column exists'
        ELSE '✗ idempotency_key column missing'
    END AS idempotency_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb'
AND TABLE_NAME = 'credit_transactions'
AND COLUMN_NAME = 'idempotency_key';

-- Check for balance_version column
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '✓ balance_version column exists'
        ELSE '✗ balance_version column missing'
    END AS version_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb'
AND TABLE_NAME = 'users'
AND COLUMN_NAME = 'balance_version';

SELECT '' AS '';

-- =====================================================
-- TEST 3: VERIFY INDICES EXIST
-- =====================================================

SELECT 'TEST 3: Verifying performance indices exist...' AS '';

SELECT 
    INDEX_NAME,
    TABLE_NAME,
    CASE 
        WHEN INDEX_NAME IS NOT NULL THEN '✓ Index exists'
        ELSE '✗ Index missing'
    END AS status
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'defaultdb'
AND INDEX_NAME IN (
    'idx_credit_transactions_idempotency',
    'idx_bids_item_status', 
    'idx_users_balance_version'
)
ORDER BY TABLE_NAME, INDEX_NAME;

SELECT '' AS '';

-- =====================================================
-- TEST 4: CHECK FOR DATA CORRUPTION
-- =====================================================

SELECT 'TEST 4: Checking for data corruption...' AS '';

-- Check for outbid users without refunds
SELECT 
    COUNT(*) AS outbid_without_refunds,
    CASE 
        WHEN COUNT(*) = 0 THEN '✓ No outbid users missing refunds'
        ELSE CONCAT('✗ ', COUNT(*), ' outbid users missing refunds')
    END AS status
FROM bids b
WHERE b.status = 'outbid'
AND NOT EXISTS (
    SELECT 1
    FROM credit_transactions ct
    WHERE ct.user_id = b.bidder_id
    AND ct.type = 'refund'
    AND ct.amount = b.amount
    AND (ct.reference LIKE '%OUTBID%' OR ct.reference LIKE CONCAT('%BID_', b.id, '%'))
    AND ct.transaction_date >= b.created_at
);

-- Check for negative credit balances
SELECT 
    COUNT(*) AS negative_balances,
    CASE 
        WHEN COUNT(*) = 0 THEN '✓ No negative credit balances'
        ELSE CONCAT('✗ ', COUNT(*), ' users with negative balances')
    END AS status
FROM users
WHERE credits < 0;

SELECT '' AS '';

-- =====================================================
-- TEST 5: VERIFY TRANSACTION INTEGRITY
-- =====================================================

SELECT 'TEST 5: Verifying transaction integrity...' AS '';

-- Check that all bid transactions have corresponding credit deductions
SELECT 
    COUNT(*) AS mismatched_transactions,
    CASE 
        WHEN COUNT(*) = 0 THEN '✓ All bid transactions properly recorded'
        ELSE CONCAT('✗ ', COUNT(*), ' bid transactions missing credit records')
    END AS status
FROM bids b
WHERE b.status IN ('winning', 'won')
AND NOT EXISTS (
    SELECT 1
    FROM credit_transactions ct
    WHERE ct.user_id = b.bidder_id
    AND ct.type = 'bid'
    AND ct.amount = b.amount
    AND ct.reference LIKE CONCAT('%ITEM_', b.item_id, '%')
);

SELECT '' AS '';

-- =====================================================
-- TEST 6: SAMPLE DATA VERIFICATION
-- =====================================================

SELECT 'TEST 6: Sample data verification...' AS '';

-- Show sample of recent credit transactions
SELECT 
    'Recent Credit Transactions' AS info,
    COUNT(*) AS total_transactions
FROM credit_transactions
WHERE transaction_date >= DATE_SUB(NOW(), INTERVAL 1 DAY);

-- Show sample of active bids
SELECT 
    'Active Bids' AS info,
    COUNT(*) AS total_bids
FROM bids
WHERE status IN ('active', 'winning');

-- Show sample of users with credits
SELECT 
    'Users with Credits' AS info,
    COUNT(*) AS users_with_credits,
    AVG(credits) AS avg_credits,
    MIN(credits) AS min_credits,
    MAX(credits) AS max_credits
FROM users
WHERE credits > 0;

SELECT '' AS '';

-- =====================================================
-- COMPLETION SUMMARY
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'CREDIT SYSTEM FIX VERIFICATION COMPLETE' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

SELECT 'If all tests show ✓, the credit system fixes are working correctly.' AS '';
SELECT 'If any tests show ✗, there may be remaining issues to address.' AS '';
SELECT '' AS '';

SELECT 'Next steps:' AS '';
SELECT '1. Test bidding functionality with multiple users' AS '';
SELECT '2. Test Buy Now functionality' AS '';
SELECT '3. Monitor for "Insufficient Credits" errors' AS '';
SELECT '4. Run load tests to verify race condition fixes' AS '';
SELECT '' AS '';

SELECT 'Completed at:', NOW() AS '';
SELECT '=======================================================' AS '';
