-- SIMPLE DATABASE CONNECTION TEST
-- Run this first to verify everything is working

-- 1. Select the database explicitly
USE defaultdb;

-- 2. Test basic connection
SELECT 'Database Connection Test' as test_name;
SELECT DATABASE() as current_database;
SELECT NOW() as current_time;

-- 3. Check if tables exist
SELECT 'Table Existence Check' as test_name;
SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    CREATE_TIME
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME IN ('users', 'items', 'bids', 'categories')
ORDER BY TABLE_NAME;

-- 4. Simple user count (if users table exists)
SELECT 'User Count Test' as test_name;
SELECT COUNT(*) as user_count FROM users;

-- 5. Simple item count (if items table exists)
SELECT 'Item Count Test' as test_name;
SELECT COUNT(*) as item_count FROM items;
