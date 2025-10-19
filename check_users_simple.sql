-- Simple query to check users in defaultdb
USE defaultdb;

-- Check if users table exists and has data
SELECT 'Users Table Check' as info;
SELECT COUNT(*) as user_count FROM users;

-- Show all users
SELECT 'All Users' as info;
SELECT 
    id,
    username,
    email,
    alias,
    credits,
    created_at
FROM users 
ORDER BY created_at DESC;

-- Check if there are any recent users (last 7 days)
SELECT 'Recent Users (Last 7 days)' as info;
SELECT 
    username,
    email,
    created_at
FROM users 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY created_at DESC;
