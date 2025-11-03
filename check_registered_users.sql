-- =====================================================
-- Check Registered Users in BidHub Database
-- =====================================================
-- Quick query to see all registered users
-- =====================================================

USE defaultdb;

-- =====================================================
-- QUICK VIEW: All Registered Users (Basic Info)
-- =====================================================
SELECT 
    id,
    email,
    username,
    alias,
    first_name,
    last_name,
    credits,
    is_verified,
    is_active,
    created_at,
    last_login
FROM users 
ORDER BY created_at DESC;

-- =====================================================
-- DETAILED VIEW: All Users with Full Information
-- =====================================================
SELECT 
    id,
    email,
    username,
    alias,
    first_name,
    last_name,
    phone_number,
    credits,
    is_verified,
    is_active,
    profile_picture,
    created_at,
    updated_at,
    last_login,
    CASE 
        WHEN password_hash IS NULL OR password_hash = '' THEN 'No password'
        ELSE 'Password set'
    END as password_status
FROM users 
ORDER BY created_at DESC;

-- =====================================================
-- SUMMARY STATISTICS
-- =====================================================
SELECT 
    COUNT(*) as total_users,
    SUM(CASE WHEN is_active = TRUE THEN 1 ELSE 0 END) as active_users,
    SUM(CASE WHEN is_verified = TRUE THEN 1 ELSE 0 END) as verified_users,
    SUM(CASE WHEN is_active = FALSE THEN 1 ELSE 0 END) as inactive_users,
    SUM(credits) as total_credits,
    AVG(credits) as average_credits
FROM users;

-- =====================================================
-- RECENT REGISTRATIONS (Last 10 users)
-- =====================================================
SELECT 
    id,
    email,
    username,
    alias,
    first_name,
    last_name,
    created_at,
    is_active,
    credits
FROM users 
ORDER BY created_at DESC
LIMIT 10;

-- =====================================================
-- USERS BY STATUS
-- =====================================================
-- Active and Verified Users
SELECT 
    id,
    email,
    username,
    alias,
    credits,
    created_at
FROM users 
WHERE is_active = TRUE 
AND is_verified = TRUE
ORDER BY created_at DESC;

-- Inactive Users
SELECT 
    id,
    email,
    username,
    alias,
    credits,
    created_at,
    last_login
FROM users 
WHERE is_active = FALSE
ORDER BY created_at DESC;

-- Unverified Users
SELECT 
    id,
    email,
    username,
    alias,
    created_at
FROM users 
WHERE is_verified = FALSE
ORDER BY created_at DESC;

-- =====================================================
-- SEARCH USERS BY EMAIL OR USERNAME
-- =====================================================
-- Replace 'search_term' with the email or username you're looking for

-- Example: Search for specific email
-- SELECT * FROM users WHERE email = 'user@example.com';

-- Example: Search for specific username
-- SELECT * FROM users WHERE username = 'someusername';

-- Example: Search for partial match
-- SELECT * FROM users WHERE email LIKE '%example%' OR username LIKE '%example%';

-- =====================================================
-- USERS WITH CREDIT INFORMATION
-- =====================================================
SELECT 
    id,
    email,
    username,
    alias,
    credits,
    CASE 
        WHEN credits = 0 THEN 'No credits'
        WHEN credits < 100 THEN 'Low credits'
        WHEN credits < 500 THEN 'Moderate credits'
        ELSE 'High credits'
    END as credit_status,
    created_at
FROM users 
ORDER BY credits DESC;

