-- Step 5: Final safe data insertion (no bids table at all)
-- Run this after Step 4

USE defaultdb;

-- Insert default categories (ignore duplicates)
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

-- Insert subcategories for Electronics (ignore duplicates)
INSERT IGNORE INTO categories (name, description, parent_id, sort_order) VALUES
('Smartphones', 'Mobile phones and accessories', 1, 1),
('Laptops', 'Laptop computers and accessories', 1, 2),
('Tablets', 'Tablet computers and accessories', 1, 3),
('Audio', 'Headphones, speakers, and audio equipment', 1, 4),
('Cameras', 'Cameras and photography equipment', 1, 5),
('Gaming', 'Gaming consoles and accessories', 1, 6);

-- Insert subcategories for Fashion (ignore duplicates)
INSERT IGNORE INTO categories (name, description, parent_id, sort_order) VALUES
('Men\'s Clothing', 'Men\'s apparel and accessories', 2, 1),
('Women\'s Clothing', 'Women\'s apparel and accessories', 2, 2),
('Shoes', 'Footwear for men and women', 2, 3),
('Accessories', 'Bags, jewelry, and other accessories', 2, 4);

-- Insert subcategories for Home & Garden (ignore duplicates)
INSERT IGNORE INTO categories (name, description, parent_id, sort_order) VALUES
('Furniture', 'Home furniture and decor', 3, 1),
('Kitchen', 'Kitchen appliances and tools', 3, 2),
('Garden', 'Garden tools and outdoor equipment', 3, 3),
('Tools', 'Hand tools and power tools', 3, 4);

-- Insert sample users (ignore duplicates)
INSERT IGNORE INTO users (username, email, phone_number, password_hash, salt, first_name, last_name, alias, credits) VALUES
('alex_smith', 'alex.smith@example.com', '+1234567890', 'hashed_password_1', 'salt_1', 'Alex', 'Smith', 'AlexS', 150.00),
('jane_doe', 'jane.doe@example.com', '+1234567891', 'hashed_password_2', 'salt_2', 'Jane', 'Doe', 'JaneD', 200.00),
('bob_wilson', 'bob.wilson@example.com', '+1234567892', 'hashed_password_3', 'salt_3', 'Bob', 'Wilson', 'BobW', 100.00);

-- Insert sample items (ignore duplicates)
INSERT IGNORE INTO items (title, description, category_id, seller_id, starting_bid, current_bid, bid_deadline, billing_deadline, item_condition, status, location, buy_now_price) VALUES
('Vintage Camera', 'Beautiful vintage camera in excellent condition', 5, 1, 150.00, 175.00, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), 'good', 'active', 'New York, NY', 250.00),
('Designer Handbag', 'Luxury designer handbag, barely used', 4, 2, 800.00, 850.00, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 'like_new', 'active', 'Los Angeles, CA', 1200.00),
('Modern Sofa', 'Comfortable modern sofa, perfect for living room', 1, 1, 1200.00, 1200.00, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), 'good', 'active', 'Chicago, IL', 1500.00),
('Rare Coin Collection', 'Collection of rare coins from different eras', 9, 3, 50.00, 75.00, DATE_ADD(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY), 'good', 'active', 'Miami, FL', 100.00);

-- Verify data inserted (only show tables that exist)
SELECT 'Categories' as table_name, COUNT(*) as count FROM categories
UNION ALL
SELECT 'Users', COUNT(*) FROM users
UNION ALL
SELECT 'Items', COUNT(*) FROM items;
