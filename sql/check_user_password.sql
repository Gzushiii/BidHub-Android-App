-- Check user password information for kaliwate@gmail.com
-- This will help diagnose login issues

USE defaultdb;

-- Check if user exists and their password_hash status
SELECT 
    id,
    email,
    username,
    alias,
    first_name,
    last_name,
    is_active,
    is_verified,
    created_at,
    last_login,
    CASE 
        WHEN password_hash IS NULL THEN 'NULL'
        WHEN password_hash = '' THEN 'EMPTY'
        ELSE CONCAT('HAS_HASH (', CHAR_LENGTH(password_hash), ' chars)')
    END as password_status,
    SUBSTRING(password_hash, 1, 20) as password_hash_preview
FROM users 
WHERE email = 'kaliwate@gmail.com';

-- Also check other test users
SELECT 
    email,
    username,
    is_active,
    CASE 
        WHEN password_hash IS NULL THEN 'NULL'
        WHEN password_hash = '' THEN 'EMPTY'
        ELSE 'HAS_HASH'
    END as password_status
FROM users 
WHERE email IN ('kaliwate@gmail.com', 'miliwate@gmail.com', 'test@example.com')
ORDER BY created_at DESC;

