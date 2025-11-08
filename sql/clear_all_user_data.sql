-- =====================================================
-- CLEAR ALL USER DATA FROM DATABASE
-- =====================================================
-- WARNING: This will DELETE ALL user-generated data!
-- This includes users, items, bids, and transactions
-- Categories will be preserved
-- =====================================================

USE defaultdb;

-- Disable foreign key checks temporarily to allow cascading deletes
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- DELETE ALL DATA (in order due to foreign key constraints)
-- =====================================================

-- Delete all bids first (references users and items)
TRUNCATE TABLE bids;

-- Delete all credit transactions
TRUNCATE TABLE credit_transactions;

-- Delete all items (references users)
TRUNCATE TABLE items;

-- Delete all users
TRUNCATE TABLE users;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Reset AUTO_INCREMENT counters
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE items AUTO_INCREMENT = 1;
ALTER TABLE bids AUTO_INCREMENT = 1;
ALTER TABLE credit_transactions AUTO_INCREMENT = 1;

-- =====================================================
-- VERIFY DELETION
-- =====================================================

-- Show counts (should all be 0)
SELECT 
    'Users' as table_name, COUNT(*) as record_count FROM users
UNION ALL
SELECT 'Items', COUNT(*) FROM items
UNION ALL
SELECT 'Bids', COUNT(*) FROM bids
UNION ALL
SELECT 'Credit Transactions', COUNT(*) FROM credit_transactions
UNION ALL
SELECT 'Categories', COUNT(*) FROM categories;

-- Show message
SELECT '=================================================' as message
UNION ALL
SELECT 'All user data has been cleared successfully!'
UNION ALL
SELECT 'Categories have been preserved.'
UNION ALL
SELECT 'Ready to insert sample data.'
UNION ALL
SELECT '=================================================';

