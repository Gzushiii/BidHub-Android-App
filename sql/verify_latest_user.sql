-- Verify the latest user registration
USE defaultdb;

-- Check the most recent users
SELECT id, username, email, created_at FROM users ORDER BY created_at DESC LIMIT 5;

-- Check total user count
SELECT COUNT(*) as total_users FROM users;
