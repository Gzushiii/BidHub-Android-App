-- =====================================================
-- COMPREHENSIVE DATABASE DIAGNOSTIC FOR CREDIT ISSUES
-- =====================================================
-- This script performs deep analysis of the database to identify
-- root causes of "Insufficient Credits" errors in bidding and Buy Now flows
--
-- Run this script and provide the full output for analysis
-- =====================================================

USE defaultdb;

SET @diagnostic_start = NOW();

SELECT '=======================================================' AS '';
SELECT 'COMPREHENSIVE DATABASE DIAGNOSTIC REPORT' AS '';
SELECT 'Generated at:', @diagnostic_start AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- =====================================================
-- SECTION 1: SCHEMA VERIFICATION
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'SECTION 1: SCHEMA VERIFICATION' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- 1.1: Check Users Table Structure
SELECT '1.1: Users Table Structure' AS '';
SELECT '---' AS '';
DESCRIBE users;
SELECT '' AS '';

-- 1.2: Check Credits Data Type and Constraints
SELECT '1.2: Credits Column Details' AS '';
SELECT '---' AS '';
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    NUMERIC_PRECISION,
    NUMERIC_SCALE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb'
AND TABLE_NAME = 'users'
AND COLUMN_NAME = 'credits';
SELECT '' AS '';

-- 1.3: Check Credit Transactions Table Structure
SELECT '1.3: Credit Transactions Table Structure' AS '';
SELECT '---' AS '';
DESCRIBE credit_transactions;
SELECT '' AS '';

-- 1.4: Check Bids Table Structure
SELECT '1.4: Bids Table Structure' AS '';
SELECT '---' AS '';
DESCRIBE bids;
SELECT '' AS '';

-- 1.5: Check Items Table Structure
SELECT '1.5: Items Table Structure (relevant columns)' AS '';
SELECT '---' AS '';
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'defaultdb'
AND TABLE_NAME = 'items'
AND COLUMN_NAME IN ('id', 'current_price', 'starting_price', 'current_bidder_id', 'seller_id', 'status', 'buy_now_price');
SELECT '' AS '';

-- 1.6: Check Foreign Key Constraints
SELECT '1.6: Foreign Key Constraints' AS '';
SELECT '---' AS '';
SELECT
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'defaultdb'
AND REFERENCED_TABLE_NAME IS NOT NULL
AND (TABLE_NAME IN ('users', 'bids', 'credit_transactions', 'items')
     OR REFERENCED_TABLE_NAME IN ('users', 'bids', 'credit_transactions', 'items'))
ORDER BY TABLE_NAME, CONSTRAINT_NAME;
SELECT '' AS '';

-- 1.7: Check for Orphaned Records
SELECT '1.7: Check for Orphaned Records' AS '';
SELECT '---' AS '';

SELECT 'Bids with non-existent items:' AS check_type, COUNT(*) AS count
FROM bids b
LEFT JOIN items i ON b.item_id = i.id
WHERE i.id IS NULL
UNION ALL
SELECT 'Bids with non-existent users:' AS check_type, COUNT(*) AS count
FROM bids b
LEFT JOIN users u ON b.bidder_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'Credit transactions with non-existent users:' AS check_type, COUNT(*) AS count
FROM credit_transactions ct
LEFT JOIN users u ON ct.user_id = u.id
WHERE u.id IS NULL;
SELECT '' AS '';

-- =====================================================
-- SECTION 2: STORED PROCEDURES ANALYSIS
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'SECTION 2: STORED PROCEDURES ANALYSIS' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- 2.1: List All Stored Procedures
SELECT '2.1: Stored Procedures in Database' AS '';
SELECT '---' AS '';
SELECT
    ROUTINE_NAME,
    ROUTINE_TYPE,
    DTD_IDENTIFIER AS return_type,
    IS_DETERMINISTIC,
    SQL_DATA_ACCESS,
    CREATED,
    LAST_ALTERED
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb'
ORDER BY ROUTINE_NAME;
SELECT '' AS '';

-- 2.2: Show PlaceBid Procedure Definition
SELECT '2.2: PlaceBid Procedure Source Code' AS '';
SELECT '---' AS '';
SELECT ROUTINE_DEFINITION
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb'
AND ROUTINE_NAME = 'PlaceBid';
SELECT '' AS '';

-- 2.3: Show BuyNow Procedure Definition
SELECT '2.3: BuyNow Procedure Source Code' AS '';
SELECT '---' AS '';
SELECT ROUTINE_DEFINITION
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb'
AND ROUTINE_NAME = 'BuyNow';
SELECT '' AS '';

-- 2.4: Show EndAuction Procedure Definition
SELECT '2.4: EndAuction Procedure Source Code' AS '';
SELECT '---' AS '';
SELECT ROUTINE_DEFINITION
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb'
AND ROUTINE_NAME = 'EndAuction';
SELECT '' AS '';

-- =====================================================
-- SECTION 3: DATA INTEGRITY AND BALANCE VERIFICATION
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'SECTION 3: DATA INTEGRITY AND BALANCE VERIFICATION' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- 3.1: User Count and Credit Distribution
SELECT '3.1: User Count and Credit Distribution' AS '';
SELECT '---' AS '';
SELECT
    COUNT(*) AS total_users,
    COUNT(CASE WHEN credits > 0 THEN 1 END) AS users_with_credits,
    COUNT(CASE WHEN credits = 0 THEN 1 END) AS users_with_zero,
    COUNT(CASE WHEN credits < 0 THEN 1 END) AS users_with_negative_balance,
    MIN(credits) AS min_balance,
    MAX(credits) AS max_balance,
    AVG(credits) AS avg_balance,
    SUM(credits) AS total_credits_in_system
FROM users;
SELECT '' AS '';

-- 3.2: List Users with Negative Balances (Data Corruption)
SELECT '3.2: Users with Negative Balances (CRITICAL ISSUE)' AS '';
SELECT '---' AS '';
SELECT
    id,
    username,
    email,
    alias,
    credits AS current_balance,
    created_at,
    updated_at
FROM users
WHERE credits < 0
ORDER BY credits ASC;
SELECT '' AS '';

-- 3.3: Credit Transaction Types Distribution
SELECT '3.3: Credit Transaction Types Distribution' AS '';
SELECT '---' AS '';
SELECT
    type,
    status,
    COUNT(*) AS transaction_count,
    SUM(amount) AS total_amount,
    MIN(amount) AS min_amount,
    MAX(amount) AS max_amount,
    AVG(amount) AS avg_amount
FROM credit_transactions
GROUP BY type, status
ORDER BY type, status;
SELECT '' AS '';

-- 3.4: Recompute Balance from Ledger and Compare (Simplified)
SELECT '3.4: Balance Reconciliation - Stored vs Computed from Ledger' AS '';
SELECT '---' AS '';
SELECT
    u.id AS user_id,
    u.username,
    u.email,
    u.credits AS stored_balance,
    COALESCE(ledger.ledger_balance, 0) AS ledger_computed_balance,
    u.credits - COALESCE(ledger.ledger_balance, 0) AS balance_mismatch,
    COALESCE(bid_stats.total_bids, 0) AS total_bids,
    COALESCE(bid_stats.winning_bids, 0) AS winning_bids,
    COALESCE(trans_stats.transaction_count, 0) AS transaction_count
FROM users u
LEFT JOIN (
    SELECT 
        user_id,
        SUM(CASE
            WHEN type IN ('refund', 'bonus', 'purchase') AND status = 'completed' THEN amount
            WHEN type = 'bid' AND status = 'completed' THEN -amount
            ELSE 0
        END) AS ledger_balance
    FROM credit_transactions
    GROUP BY user_id
) ledger ON u.id = ledger.user_id
LEFT JOIN (
    SELECT 
        bidder_id,
        COUNT(*) AS total_bids,
        SUM(CASE WHEN status = 'winning' THEN 1 ELSE 0 END) AS winning_bids
    FROM bids
    GROUP BY bidder_id
) bid_stats ON u.id = bid_stats.bidder_id
LEFT JOIN (
    SELECT 
        user_id,
        COUNT(*) AS transaction_count
    FROM credit_transactions
    GROUP BY user_id
) trans_stats ON u.id = trans_stats.user_id
WHERE u.id IN (
    SELECT DISTINCT user_id FROM credit_transactions
    UNION
    SELECT DISTINCT bidder_id FROM bids
)
ORDER BY ABS(u.credits - COALESCE(ledger.ledger_balance, 0)) DESC;
SELECT '' AS '';

-- 3.5: Find Users with Mismatched Balances >= 1 Credit (Simplified)
SELECT '3.5: Users with Balance Mismatches >= 1 Credit (CRITICAL)' AS '';
SELECT '---' AS '';
SELECT
    u.id,
    u.username,
    u.credits AS stored,
    COALESCE(ledger.ledger_balance, 0) AS computed,
    ABS(u.credits - COALESCE(ledger.ledger_balance, 0)) AS mismatch
FROM users u
LEFT JOIN (
    SELECT 
        user_id,
        SUM(CASE
            WHEN type IN ('refund', 'bonus', 'purchase') AND status = 'completed' THEN amount
            WHEN type = 'bid' AND status = 'completed' THEN -amount
            ELSE 0
        END) AS ledger_balance
    FROM credit_transactions
    GROUP BY user_id
) ledger ON u.id = ledger.user_id
HAVING ABS(u.credits - COALESCE(ledger.ledger_balance, 0)) >= 1
ORDER BY ABS(u.credits - COALESCE(ledger.ledger_balance, 0)) DESC;
SELECT '' AS '';

-- =====================================================
-- SECTION 4: BIDDING FLOW ANALYSIS
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'SECTION 4: BIDDING FLOW ANALYSIS' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- 4.1: Bid Status Distribution
SELECT '4.1: Bid Status Distribution' AS '';
SELECT '---' AS '';
SELECT
    status,
    COUNT(*) AS bid_count,
    SUM(amount) AS total_amount,
    MIN(amount) AS min_bid,
    MAX(amount) AS max_bid,
    AVG(amount) AS avg_bid
FROM bids
GROUP BY status
ORDER BY status;
SELECT '' AS '';

-- 4.2: Items with Multiple "Winning" Bids (Data Corruption)
SELECT '4.2: Items with Multiple Winning Bids (CRITICAL ISSUE)' AS '';
SELECT '---' AS '';
SELECT
    item_id,
    COUNT(*) AS winning_bid_count,
    GROUP_CONCAT(CONCAT('Bid#', id, ' by User#', bidder_id, ': ', amount) SEPARATOR '; ') AS winning_bids
FROM bids
WHERE status = 'winning'
GROUP BY item_id
HAVING COUNT(*) > 1;
SELECT '' AS '';

-- 4.3: Check for Missing Refunds on Outbid
SELECT '4.3: Outbid Bids Without Corresponding Refunds' AS '';
SELECT '---' AS '';
SELECT
    b.id AS bid_id,
    b.item_id,
    b.bidder_id,
    u.username,
    b.amount AS bid_amount,
    b.status,
    b.created_at AS bid_placed_at,
    (SELECT COUNT(*)
     FROM credit_transactions ct
     WHERE ct.user_id = b.bidder_id
     AND ct.type = 'refund'
     AND ct.amount = b.amount
     AND ct.transaction_date >= b.created_at) AS refund_count
FROM bids b
JOIN users u ON b.bidder_id = u.id
WHERE b.status = 'outbid'
HAVING refund_count = 0
ORDER BY b.created_at DESC;
SELECT '' AS '';

-- 4.4: Bidders with Active/Winning Bids and Their Available Balance
SELECT '4.4: Users with Active/Winning Bids and Their Balance Status' AS '';
SELECT '---' AS '';
SELECT
    u.id AS user_id,
    u.username,
    u.credits AS available_balance,
    COUNT(b.id) AS active_winning_bids,
    SUM(b.amount) AS total_held_in_bids,
    u.credits - COALESCE(SUM(b.amount), 0) AS effective_available_balance
FROM users u
LEFT JOIN bids b ON u.id = b.bidder_id AND b.status IN ('active', 'winning')
GROUP BY u.id, u.username, u.credits
HAVING active_winning_bids > 0
ORDER BY effective_available_balance ASC;
SELECT '' AS '';

-- 4.5: Recent Bidding Activity (Last 50 bids)
SELECT '4.5: Recent Bidding Activity (Last 50 Bids)' AS '';
SELECT '---' AS '';
SELECT
    b.id,
    b.item_id,
    i.title AS item_title,
    b.bidder_id,
    u.username AS bidder_username,
    b.bidder_alias,
    b.amount,
    b.status,
    b.created_at,
    u.credits AS current_bidder_balance
FROM bids b
JOIN users u ON b.bidder_id = u.id
LEFT JOIN items i ON b.item_id = i.id
ORDER BY b.created_at DESC
LIMIT 50;
SELECT '' AS '';

-- =====================================================
-- SECTION 5: BUY NOW FLOW ANALYSIS
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'SECTION 5: BUY NOW FLOW ANALYSIS' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- 5.1: Items with Buy Now Price
SELECT '5.1: Items with Buy Now Price' AS '';
SELECT '---' AS '';
SELECT
    COUNT(*) AS items_with_buy_now,
    MIN(buy_now_price) AS min_buy_now_price,
    MAX(buy_now_price) AS max_buy_now_price,
    AVG(buy_now_price) AS avg_buy_now_price
FROM items
WHERE buy_now_price IS NOT NULL AND buy_now_price > 0;
SELECT '' AS '';

-- 5.2: Buy Now Transactions (Purchase type)
SELECT '5.2: Buy Now Transactions' AS '';
SELECT '---' AS '';
SELECT
    ct.id,
    ct.user_id,
    u.username,
    ct.amount,
    ct.status,
    ct.reference,
    ct.transaction_date,
    u.credits AS current_user_balance
FROM credit_transactions ct
JOIN users u ON ct.user_id = u.id
WHERE ct.type = 'purchase'
ORDER BY ct.transaction_date DESC
LIMIT 50;
SELECT '' AS '';

-- 5.3: Items Marked as Sold
SELECT '5.3: Items Marked as Sold' AS '';
SELECT '---' AS '';
SELECT
    i.id,
    i.title,
    i.current_price,
    i.buy_now_price,
    i.seller_id,
    seller.username AS seller_name,
    i.current_bidder_id,
    buyer.username AS buyer_name,
    i.updated_at AS sold_at
FROM items i
LEFT JOIN users seller ON i.seller_id = seller.id
LEFT JOIN users buyer ON i.current_bidder_id = buyer.id
WHERE i.status = 'sold'
ORDER BY i.updated_at DESC
LIMIT 20;
SELECT '' AS '';

-- =====================================================
-- SECTION 6: TRANSACTION ISOLATION AND CONCURRENCY
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'SECTION 6: TRANSACTION ISOLATION AND CONCURRENCY' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- 6.1: Check Transaction Isolation Level
SELECT '6.1: Current Transaction Isolation Level' AS '';
SELECT '---' AS '';
SELECT @@transaction_isolation AS transaction_isolation_level;
SELECT @@global.transaction_isolation AS global_transaction_isolation_level;
SELECT '' AS '';

-- 6.2: Check InnoDB Lock Wait Timeout
SELECT '6.2: InnoDB Lock Wait Timeout' AS '';
SELECT '---' AS '';
SELECT @@innodb_lock_wait_timeout AS lock_wait_timeout_seconds;
SELECT '' AS '';

-- 6.3: Check for Recent Deadlocks (requires PROCESS privilege)
SELECT '6.3: InnoDB Status (Check for Deadlocks)' AS '';
SELECT '---' AS '';
-- Note: SHOW ENGINE INNODB STATUS requires PROCESS privilege
-- This will show the last detected deadlock
-- We cannot capture this in a SELECT, user must run: SHOW ENGINE INNODB STATUS;
SELECT 'Run: SHOW ENGINE INNODB STATUS; to check for deadlock information' AS note;
SELECT '' AS '';

-- =====================================================
-- SECTION 7: INDICES AND PERFORMANCE
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'SECTION 7: INDICES AND PERFORMANCE' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- 7.1: Check Indices on Key Tables
SELECT '7.1: Indices on Users Table' AS '';
SELECT '---' AS '';
SHOW INDEX FROM users;
SELECT '' AS '';

SELECT '7.2: Indices on Bids Table' AS '';
SELECT '---' AS '';
SHOW INDEX FROM bids;
SELECT '' AS '';

SELECT '7.3: Indices on Credit Transactions Table' AS '';
SELECT '---' AS '';
SHOW INDEX FROM credit_transactions;
SELECT '' AS '';

SELECT '7.4: Indices on Items Table' AS '';
SELECT '---' AS '';
SHOW INDEX FROM items;
SELECT '' AS '';

-- =====================================================
-- SECTION 8: RECENT ERRORS AND PATTERNS
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'SECTION 8: RECENT ERRORS AND PATTERNS' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- 8.1: Failed Transactions
SELECT '8.1: Failed Credit Transactions' AS '';
SELECT '---' AS '';
SELECT
    ct.id,
    ct.user_id,
    u.username,
    ct.type,
    ct.amount,
    ct.status,
    ct.reference,
    ct.transaction_date,
    u.credits AS current_user_balance
FROM credit_transactions ct
JOIN users u ON ct.user_id = u.id
WHERE ct.status = 'failed'
ORDER BY ct.transaction_date DESC;
SELECT '' AS '';

-- 8.2: Pending Transactions (Stuck?)
SELECT '8.2: Pending Credit Transactions (Potentially Stuck)' AS '';
SELECT '---' AS '';
SELECT
    ct.id,
    ct.user_id,
    u.username,
    ct.type,
    ct.amount,
    ct.status,
    ct.reference,
    ct.transaction_date,
    TIMESTAMPDIFF(MINUTE, ct.transaction_date, NOW()) AS minutes_pending,
    u.credits AS current_user_balance
FROM credit_transactions ct
JOIN users u ON ct.user_id = u.id
WHERE ct.status = 'pending'
ORDER BY ct.transaction_date DESC;
SELECT '' AS '';

-- =====================================================
-- SECTION 9: SUMMARY AND RECOMMENDATIONS
-- =====================================================

SELECT '=======================================================' AS '';
SELECT 'SECTION 9: DIAGNOSTIC SUMMARY' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

SELECT 'Total Users:' AS metric, COUNT(*) AS value FROM users
UNION ALL
SELECT 'Total Bids:' AS metric, COUNT(*) AS value FROM bids
UNION ALL
SELECT 'Total Credit Transactions:' AS metric, COUNT(*) AS value FROM credit_transactions
UNION ALL
SELECT 'Total Items:' AS metric, COUNT(*) AS value FROM items
UNION ALL
SELECT 'Users with Negative Balance:' AS metric, COUNT(*) AS value FROM users WHERE credits < 0
UNION ALL
SELECT 'Outbid Bids Without Refunds:' AS metric,
    (SELECT COUNT(*) FROM bids b WHERE b.status = 'outbid' AND NOT EXISTS (
        SELECT 1 FROM credit_transactions ct
        WHERE ct.user_id = b.bidder_id
        AND ct.type = 'refund'
        AND ct.amount = b.amount
        AND ct.transaction_date >= b.created_at
    )) AS value
UNION ALL
SELECT 'Failed Transactions:' AS metric, COUNT(*) AS value FROM credit_transactions WHERE status = 'failed'
UNION ALL
SELECT 'Pending Transactions:' AS metric, COUNT(*) AS value FROM credit_transactions WHERE status = 'pending';

SELECT '' AS '';
SELECT '=======================================================' AS '';
SELECT 'END OF DIAGNOSTIC REPORT' AS '';
SELECT 'Generated at:', @diagnostic_start AS '';
SELECT 'Completed at:', NOW() AS '';
SELECT '=======================================================' AS '';
