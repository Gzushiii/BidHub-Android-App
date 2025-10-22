-- Clean up all users and their linked data
-- WARNING: This will delete ALL users and ALL related data!
-- Use with extreme caution - this is irreversible!

USE defaultdb;

-- Disable foreign key checks temporarily to avoid constraint issues
SET FOREIGN_KEY_CHECKS = 0;

-- Delete all data from tables that reference users (in dependency order)
DELETE FROM credit_transactions;
DELETE FROM bids;
DELETE FROM item_images;
DELETE FROM items;
DELETE FROM categories;
DELETE FROM users;

-- Reset auto-increment counters
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE items AUTO_INCREMENT = 1;
ALTER TABLE bids AUTO_INCREMENT = 1;
ALTER TABLE credit_transactions AUTO_INCREMENT = 1;
ALTER TABLE categories AUTO_INCREMENT = 1;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Verify cleanup
SELECT '=== CLEANUP VERIFICATION ===' as status;
SELECT 'Users count:' as table_name, COUNT(*) as count FROM users;
SELECT 'Items count:' as table_name, COUNT(*) as count FROM items;
SELECT 'Bids count:' as table_name, COUNT(*) as count FROM bids;
SELECT 'Credit transactions count:' as table_name, COUNT(*) as count FROM credit_transactions;
SELECT 'Categories count:' as table_name, COUNT(*) as count FROM categories;

-- Recreate default categories
INSERT INTO categories (name, description) VALUES 
('Electronics', 'Electronic devices and gadgets'),
('Fashion', 'Clothing and accessories'),
('Home & Garden', 'Home improvement and garden items'),
('Sports', 'Sports equipment and gear'),
('Books', 'Books and educational materials'),
('Toys', 'Toys and games'),
('Automotive', 'Car parts and accessories'),
('Health & Beauty', 'Health and beauty products'),
('Collectibles', 'Collectible items and antiques'),
('Other', 'Miscellaneous items');

SELECT '=== DEFAULT CATEGORIES RESTORED ===' as status;
SELECT * FROM categories;
