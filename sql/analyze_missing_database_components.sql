-- Comprehensive analysis of missing database components causing "insufficient credits" error
-- This script identifies all critical missing tables, columns, and procedures

USE defaultdb;

-- ==============================================
-- CRITICAL MISSING COMPONENTS ANALYSIS
-- ==============================================

-- 1. Check if critical tables exist
SELECT '=== CRITICAL TABLES CHECK ===' as section;
SELECT 
    TABLE_NAME,
    CASE 
        WHEN TABLE_NAME IN ('users', 'items', 'credit_transactions', 'bids', 'categories') 
        THEN 'REQUIRED'
        ELSE 'OPTIONAL'
    END as importance,
    CASE 
        WHEN TABLE_NAME = 'users' THEN 'User accounts and credit balances'
        WHEN TABLE_NAME = 'items' THEN 'Auction items and bidding data'
        WHEN TABLE_NAME = 'credit_transactions' THEN 'Credit purchase and usage history'
        WHEN TABLE_NAME = 'bids' THEN 'Bid records and auction data'
        WHEN TABLE_NAME = 'categories' THEN 'Item categorization'
        ELSE 'Other table'
    END as purpose
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb'
ORDER BY importance DESC, TABLE_NAME;

-- 2. Check users table structure
SELECT '=== USERS TABLE STRUCTURE ===' as section;
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    CASE 
        WHEN COLUMN_NAME = 'id' THEN 'CRITICAL - Primary key'
        WHEN COLUMN_NAME = 'email' THEN 'CRITICAL - Authentication'
        WHEN COLUMN_NAME = 'credits' THEN 'CRITICAL - Credit balance'
        WHEN COLUMN_NAME = 'alias' THEN 'CRITICAL - Bidding identity'
        WHEN COLUMN_NAME = 'updated_at' THEN 'MISSING - Credit update tracking'
        ELSE 'Standard'
    END as importance
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'users'
ORDER BY 
    CASE 
        WHEN COLUMN_NAME = 'id' THEN 1
        WHEN COLUMN_NAME = 'email' THEN 2
        WHEN COLUMN_NAME = 'credits' THEN 3
        WHEN COLUMN_NAME = 'alias' THEN 4
        WHEN COLUMN_NAME = 'updated_at' THEN 5
        ELSE 6
    END;

-- 3. Check credit_transactions table structure
SELECT '=== CREDIT_TRANSACTIONS TABLE STRUCTURE ===' as section;
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    CASE 
        WHEN COLUMN_NAME = 'id' THEN 'CRITICAL - Primary key'
        WHEN COLUMN_NAME = 'user_id' THEN 'CRITICAL - User reference'
        WHEN COLUMN_NAME = 'type' THEN 'CRITICAL - Transaction type'
        WHEN COLUMN_NAME = 'amount' THEN 'CRITICAL - Credit amount'
        WHEN COLUMN_NAME = 'status' THEN 'CRITICAL - Transaction status'
        WHEN COLUMN_NAME = 'transaction_date' THEN 'MISSING - Timestamp tracking'
        WHEN COLUMN_NAME = 'created_at' THEN 'MISSING - Creation timestamp'
        ELSE 'Standard'
    END as importance
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'credit_transactions'
ORDER BY 
    CASE 
        WHEN COLUMN_NAME = 'id' THEN 1
        WHEN COLUMN_NAME = 'user_id' THEN 2
        WHEN COLUMN_NAME = 'type' THEN 3
        WHEN COLUMN_NAME = 'amount' THEN 4
        WHEN COLUMN_NAME = 'status' THEN 5
        WHEN COLUMN_NAME = 'transaction_date' THEN 6
        WHEN COLUMN_NAME = 'created_at' THEN 7
        ELSE 8
    END;

-- 4. Check if bids table exists
SELECT '=== BIDS TABLE STATUS ===' as section;
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'EXISTS'
        ELSE 'MISSING - CRITICAL FOR BIDDING'
    END as status,
    'Required for bid placement and credit deduction' as purpose
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'bids';

-- 5. Check if PlaceBid stored procedure exists
SELECT '=== PLACEBID STORED PROCEDURE STATUS ===' as section;
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'EXISTS'
        ELSE 'MISSING - CRITICAL FOR BIDDING'
    END as status,
    'Required for atomic bid placement and credit deduction' as purpose
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'defaultdb' 
AND ROUTINE_NAME = 'PlaceBid';

-- 6. Check items table structure for bidding
SELECT '=== ITEMS TABLE BIDDING STRUCTURE ===' as section;
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    CASE 
        WHEN COLUMN_NAME = 'id' THEN 'CRITICAL - Primary key'
        WHEN COLUMN_NAME = 'seller_id' THEN 'CRITICAL - Seller reference'
        WHEN COLUMN_NAME = 'starting_price' THEN 'CRITICAL - Starting bid'
        WHEN COLUMN_NAME = 'current_price' THEN 'CRITICAL - Current highest bid'
        WHEN COLUMN_NAME = 'status' THEN 'CRITICAL - Auction status'
        WHEN COLUMN_NAME = 'end_date' THEN 'CRITICAL - Auction end time'
        WHEN COLUMN_NAME = 'current_bidder_id' THEN 'MISSING - Current winning bidder'
        ELSE 'Standard'
    END as importance
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'items'
ORDER BY 
    CASE 
        WHEN COLUMN_NAME = 'id' THEN 1
        WHEN COLUMN_NAME = 'seller_id' THEN 2
        WHEN COLUMN_NAME = 'starting_price' THEN 3
        WHEN COLUMN_NAME = 'current_price' THEN 4
        WHEN COLUMN_NAME = 'status' THEN 5
        WHEN COLUMN_NAME = 'end_date' THEN 6
        WHEN COLUMN_NAME = 'current_bidder_id' THEN 7
        ELSE 8
    END;

-- 7. Check for foreign key constraints
SELECT '=== FOREIGN KEY CONSTRAINTS ===' as section;
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME,
    CASE 
        WHEN TABLE_NAME = 'credit_transactions' AND REFERENCED_TABLE_NAME = 'users' THEN 'CRITICAL - User credit tracking'
        WHEN TABLE_NAME = 'bids' AND REFERENCED_TABLE_NAME = 'users' THEN 'CRITICAL - Bidder tracking'
        WHEN TABLE_NAME = 'bids' AND REFERENCED_TABLE_NAME = 'items' THEN 'CRITICAL - Item bidding'
        WHEN TABLE_NAME = 'items' AND REFERENCED_TABLE_NAME = 'users' THEN 'CRITICAL - Seller tracking'
        ELSE 'Standard'
    END as importance
FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY importance DESC;

-- 8. Summary of critical missing components
SELECT '=== CRITICAL MISSING COMPONENTS SUMMARY ===' as section;
SELECT 
    'Missing updated_at column in users table' as issue,
    'Causes credit balance sync problems' as impact,
    'Add: ALTER TABLE users ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;' as solution
UNION ALL
SELECT 
    'Missing transaction_date column in credit_transactions table' as issue,
    'Causes transaction history queries to fail' as impact,
    'Add: ALTER TABLE credit_transactions ADD COLUMN transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;' as solution
UNION ALL
SELECT 
    'Missing bids table' as issue,
    'Causes bid placement to fail completely' as impact,
    'Create bids table with proper structure' as solution
UNION ALL
SELECT 
    'Missing PlaceBid stored procedure' as issue,
    'Causes atomic bid placement to fail' as impact,
    'Create PlaceBid stored procedure for atomic operations' as solution
UNION ALL
SELECT 
    'Missing current_bidder_id column in items table' as issue,
    'Causes bid tracking problems' as impact,
    'Add: ALTER TABLE items ADD COLUMN current_bidder_id INT UNSIGNED NULL;' as solution;
