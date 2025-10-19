-- Clean database status check
-- Run this in MySQL Workbench to verify your database

USE defaultdb;

-- Check users count and recent users
SELECT COUNT(*) as user_count FROM users;
SELECT id, username, email, created_at FROM users ORDER BY created_at DESC LIMIT 5;

-- Check items count and recent items  
SELECT COUNT(*) as item_count FROM items;
SELECT id, title, seller_id, status, created_at FROM items ORDER BY created_at DESC LIMIT 5;

-- Check categories count
SELECT COUNT(*) as category_count FROM categories;
SELECT id, name, parent_id FROM categories LIMIT 10;
