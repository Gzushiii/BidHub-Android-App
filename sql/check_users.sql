-- Check Users in Database
-- This script will show us what users exist and their credentials

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'CHECKING USERS IN DATABASE' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Step 1: Count total users
SELECT 'Step 1: Total users count...' AS '';
SELECT COUNT(*) as total_users FROM users;

SELECT '' AS '';

-- Step 2: List all users (without passwords for security)
SELECT 'Step 2: All users in database...' AS '';
SELECT 
    id,
    email,
    username,
    alias,
    first_name,
    last_name,
    created_at
FROM users 
ORDER BY created_at DESC;

SELECT '' AS '';

-- Step 3: Check if users have passwords
SELECT 'Step 3: Users with password hashes...' AS '';
SELECT 
    id,
    email,
    username,
    CASE 
        WHEN password IS NULL THEN 'No password'
        WHEN password = '' THEN 'Empty password'
        ELSE 'Has password hash'
    END as password_status
FROM users 
ORDER BY id;

SELECT '' AS '';

-- Step 4: Check user roles/permissions
SELECT 'Step 4: User roles and permissions...' AS '';
SELECT 
    id,
    email,
    username,
    role,
    is_active,
    email_verified
FROM users 
ORDER BY id;

SELECT '' AS '';

SELECT '=======================================================' AS '';
SELECT 'USERS CHECK COMPLETE' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';
SELECT 'Use one of these email addresses for testing:' AS '';
SELECT 'Make sure the user has a password set.' AS '';
