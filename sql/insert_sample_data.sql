-- =====================================================
-- SAMPLE DATA FOR BIDHUB APP
-- Generated: 2025-11-03T00:05:27.613Z
-- =====================================================
-- This script inserts sample data with properly hashed bcrypt passwords
-- Default password for all users: password123
-- Test user password: test1234
-- =====================================================

USE defaultdb;

-- =====================================================
-- INSERT CATEGORIES (if not exists)
-- =====================================================

-- Main categories
INSERT IGNORE INTO categories (name, description, parent_id, sort_order) VALUES
('Electronics', 'Electronic devices and accessories', NULL, 1),
('Fashion', 'Clothing, shoes, and accessories', NULL, 2),
('Home & Garden', 'Home improvement and garden items', NULL, 3),
('Sports & Outdoors', 'Sports equipment and outdoor gear', NULL, 4),
('Books & Media', 'Books, movies, music, and games', NULL, 5),
('Automotive', 'Car parts and automotive accessories', NULL, 6),
('Health & Beauty', 'Health and beauty products', NULL, 7),
('Toys & Games', 'Toys and gaming items', NULL, 8),
('Collectibles', 'Collectible items and memorabilia', NULL, 9),
('Others', 'Miscellaneous items that don\'t fit specific categories', NULL, 10);

-- Electronics subcategories
INSERT IGNORE INTO categories (name, description, parent_id, sort_order) VALUES
('Smartphones', 'Mobile phones and accessories', 1, 1),
('Laptops', 'Laptop computers and accessories', 1, 2),
('Tablets', 'Tablet computers and accessories', 1, 3),
('Audio', 'Headphones, speakers, and audio equipment', 1, 4),
('Cameras', 'Cameras and photography equipment', 1, 5),
('Gaming', 'Gaming consoles and accessories', 1, 6);

-- Fashion subcategories
INSERT IGNORE INTO categories (name, description, parent_id, sort_order) VALUES
('Men\'s Clothing', 'Men\'s apparel and accessories', 2, 1),
('Women\'s Clothing', 'Women\'s apparel and accessories', 2, 2),
('Shoes', 'Footwear for men and women', 2, 3),
('Accessories', 'Bags, jewelry, and other accessories', 2, 4);

-- Home & Garden subcategories
INSERT IGNORE INTO categories (name, description, parent_id, sort_order) VALUES
('Furniture', 'Home furniture and decor', 3, 1),
('Kitchen', 'Kitchen appliances and tools', 3, 2),
('Garden', 'Garden tools and outdoor equipment', 3, 3),
('Tools', 'Hand tools and power tools', 3, 4);

-- =====================================================
-- INSERT SAMPLE USERS (with proper bcrypt hashes)
-- =====================================================

-- Delete existing test users first
DELETE FROM users WHERE email IN (
    'alex.smith@example.com',
    'jane.doe@example.com',
    'bob.wilson@example.com',
    'test@example.com'
);

-- Insert users with proper password hashes
INSERT INTO users (username, email, phone_number, password_hash, salt, first_name, last_name, alias, credits, is_verified, is_active) VALUES
('alex_smith', 'alex.smith@example.com', '+1234567890', '$2a$08$iiCwNY8INEZpN0.nSp67euNuu5BnsthffZhsI/w5fG8v0du/7An22', 'f6a2e22164e97772691b4a2c34c79bb8', 'Alex', 'Smith', 'AlexS', 150, TRUE, TRUE),
('jane_doe', 'jane.doe@example.com', '+1234567891', '$2a$08$RnUTOhdhb1hzxejcKbEfBOYIussKC0UhP6fT.NazRIal3nmdfyE.q', '0f6b6c3394be6dc55a8d82010270897d', 'Jane', 'Doe', 'JaneD', 200, TRUE, TRUE),
('bob_wilson', 'bob.wilson@example.com', '+1234567892', '$2a$08$XA8TNFUdiEq88.7UI8Fneuv9O2QHVKN5mWDQnrTQ6yOW5x3FEd6Yq', 'af857f0260b5586f34c7280e15bd561b', 'Bob', 'Wilson', 'BobW', 100, TRUE, TRUE),
('test_user', 'test@example.com', '+1234567893', '$2a$08$BIcXkEGMbQVce7qhhp0Uw.RQMdvwpQ9sLPCk2yJ1gleW0Ki3oQOJa', '29a530d221b5088b2e00e51e59f2ed41', 'Test', 'User', 'TestUser', 500, TRUE, TRUE);

-- =====================================================
-- INSERT SAMPLE ITEMS
-- =====================================================

-- Get user IDs (assuming they were inserted in order)
SET @alex_id = (SELECT id FROM users WHERE email = 'alex.smith@example.com' LIMIT 1);
SET @jane_id = (SELECT id FROM users WHERE email = 'jane.doe@example.com' LIMIT 1);
SET @bob_id = (SELECT id FROM users WHERE email = 'bob.wilson@example.com' LIMIT 1);
SET @test_id = (SELECT id FROM users WHERE email = 'test@example.com' LIMIT 1);

-- Delete existing test items
DELETE FROM items WHERE seller_id IN (@alex_id, @jane_id, @bob_id, @test_id);

-- Insert sample items
INSERT INTO items (title, description, category_id, seller_id, starting_price, current_bid, buy_now_price, bid_deadline, billing_deadline, item_condition, status, location, created_at, updated_at) VALUES
('Vintage Nikon Camera', 'Beautiful vintage Nikon camera from the 1980s. Fully functional with original lens and case. Excellent condition.', 5, @alex_id, 150.00, 150.00, 250.00, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), 'good', 'active', 'New York, NY', NOW(), NOW()),
('Designer Leather Handbag', 'Luxury designer leather handbag from premium brand. Barely used, perfect condition. Includes dust bag and authenticity card.', 4, @jane_id, 800.00, 800.00, 1200.00, DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY), 'like_new', 'active', 'Los Angeles, CA', NOW(), NOW()),
('Modern Sectional Sofa', 'Comfortable modern sectional sofa in light gray fabric. Perfect for large living rooms. Includes throw pillows.', 11, @alex_id, 1200.00, 1200.00, 1500.00, DATE_ADD(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 11 DAY), 'good', 'active', 'Chicago, IL', NOW(), NOW()),
('Rare Coin Collection', 'Collection of rare silver coins from different eras. Includes Morgan dollars, Peace dollars, and other collectibles.', 9, @bob_id, 50.00, 50.00, 100.00, DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY), 'good', 'active', 'Miami, FL', NOW(), NOW()),
('Wireless Headphones', 'High-quality wireless noise-cancelling headphones. Brand new in box, never opened. Latest model with 30-hour battery life.', 4, @test_id, 200.00, 200.00, 300.00, DATE_ADD(NOW(), INTERVAL 14 DAY), DATE_ADD(NOW(), INTERVAL 15 DAY), 'new', 'active', 'San Francisco, CA', NOW(), NOW()),
('Gaming Laptop', 'Powerful gaming laptop with RTX graphics card. 16GB RAM, 1TB SSD, 15-inch display. Excellent for gaming and work.', 2, @alex_id, 1000.00, 1050.00, 1500.00, DATE_ADD(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 'like_new', 'active', 'Seattle, WA', NOW(), NOW()),
('Vintage Watch Collection', 'Collection of three vintage watches from the 1960s-1970s. All in working condition with leather straps.', 9, @jane_id, 300.00, 300.00, 500.00, DATE_ADD(NOW(), INTERVAL 8 DAY), DATE_ADD(NOW(), INTERVAL 9 DAY), 'good', 'active', 'Boston, MA', NOW(), NOW()),
('Designer Sunglasses', 'Luxury designer sunglasses with UV protection. Brand new with original case and cleaning cloth.', 4, @bob_id, 100.00, 100.00, 200.00, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), 'new', 'active', 'Austin, TX', NOW(), NOW());

-- =====================================================
-- VERIFY DATA INSERTED
-- =====================================================

SELECT '=================================================' as message
UNION ALL
SELECT 'Sample Data Inserted Successfully!'
UNION ALL
SELECT '=================================================';

SELECT 'Categories' as table_name, COUNT(*) as record_count FROM categories
UNION ALL
SELECT 'Users', COUNT(*) FROM users
UNION ALL
SELECT 'Items', COUNT(*) FROM items;

-- Show users with their credentials
SELECT 
    'User Credentials' as info
UNION ALL
SELECT '=================================================='
UNION ALL
SELECT CONCAT('Email: ', email, ' | Password: ', 
    CASE 
        WHEN email = 'test@example.com' THEN 'test1234'
        ELSE 'password123'
    END) as credentials
FROM users 
ORDER BY id;

-- Show sample items
SELECT 
    id,
    title,
    CONCAT('$', FORMAT(starting_price, 2)) as starting_price,
    item_condition,
    status,
    location
FROM items 
ORDER BY created_at DESC
LIMIT 10;

SELECT '=================================================' as message
UNION ALL
SELECT 'Sample data insertion complete!'
UNION ALL
SELECT 'You can now test the app with these accounts.'
UNION ALL
SELECT '=================================================';
