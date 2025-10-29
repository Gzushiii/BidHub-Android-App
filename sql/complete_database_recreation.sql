-- =====================================================
-- COMPLETE DATABASE RECREATION SCRIPT
-- =====================================================
-- This script recreates the entire BidHub database schema
-- including all tables, procedures, triggers, and sample data
-- =====================================================

-- Drop database if exists (be careful!)
-- DROP DATABASE IF EXISTS defaultdb;

-- Create database
CREATE DATABASE IF NOT EXISTS defaultdb 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Use the database
USE defaultdb;

-- =====================================================
-- STEP 1: CREATE CORE TABLES
-- =====================================================

-- Create users table
CREATE TABLE users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    alias VARCHAR(50) NOT NULL UNIQUE,
    credits DECIMAL(10,2) DEFAULT 0.00,
    is_verified BOOLEAN DEFAULT FALSE,
    profile_picture VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    
    INDEX idx_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_alias (alias)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create categories table
CREATE TABLE categories (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    parent_id INT UNSIGNED NULL,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_categories_parent_id (parent_id),
    INDEX idx_categories_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create items table
CREATE TABLE items (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category_id INT UNSIGNED NULL,
    seller_id INT UNSIGNED NOT NULL,
    seller_email VARCHAR(255) NULL,
    starting_price DECIMAL(10,2) NOT NULL,
    current_bid DECIMAL(10,2) DEFAULT 0.00,
    buy_now_price DECIMAL(10,2) NULL,
    bid_deadline DATETIME NULL,
    billing_deadline DATETIME NULL,
    item_condition ENUM('new', 'like_new', 'good', 'fair', 'poor') DEFAULT 'good',
    status ENUM('draft', 'active', 'ended', 'sold', 'cancelled') DEFAULT 'draft',
    location VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_items_seller_id (seller_id),
    INDEX idx_items_category_id (category_id),
    INDEX idx_items_status (status),
    INDEX idx_items_bid_deadline (bid_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create bids table
CREATE TABLE bids (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id INT UNSIGNED NOT NULL,
    bidder_id INT UNSIGNED NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('active', 'outbid', 'winning', 'cancelled') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_bids_item_id (item_id),
    INDEX idx_bids_bidder_id (bidder_id),
    INDEX idx_bids_status (status),
    INDEX idx_bids_amount (amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create transactions table for credit tracking
CREATE TABLE transactions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    transaction_type ENUM('bid', 'buy_now', 'refund', 'outbid_refund', 'credit_purchase') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description TEXT NULL,
    item_id INT UNSIGNED NULL,
    bid_id INT UNSIGNED NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE SET NULL,
    FOREIGN KEY (bid_id) REFERENCES bids(id) ON DELETE SET NULL,
    INDEX idx_transactions_user_id (user_id),
    INDEX idx_transactions_type (transaction_type),
    INDEX idx_transactions_date (transaction_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- STEP 2: INSERT SAMPLE DATA
-- =====================================================

-- Insert default categories
INSERT INTO categories (name, description, parent_id, sort_order) VALUES
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

-- Insert subcategories for Electronics
INSERT INTO categories (name, description, parent_id, sort_order) VALUES
('Smartphones', 'Mobile phones and accessories', 1, 1),
('Laptops', 'Laptop computers and accessories', 1, 2),
('Tablets', 'Tablet computers and accessories', 1, 3),
('Audio', 'Headphones, speakers, and audio equipment', 1, 4),
('Cameras', 'Cameras and photography equipment', 1, 5),
('Gaming', 'Gaming consoles and accessories', 1, 6);

-- Insert subcategories for Fashion
INSERT INTO categories (name, description, parent_id, sort_order) VALUES
('Men\'s Clothing', 'Men\'s apparel and accessories', 2, 1),
('Women\'s Clothing', 'Women\'s apparel and accessories', 2, 2),
('Shoes', 'Footwear for men and women', 2, 3),
('Accessories', 'Bags, jewelry, and other accessories', 2, 4);

-- Insert subcategories for Home & Garden
INSERT INTO categories (name, description, parent_id, sort_order) VALUES
('Furniture', 'Home furniture and decor', 3, 1),
('Kitchen', 'Kitchen appliances and tools', 3, 2),
('Garden', 'Garden tools and outdoor equipment', 3, 3),
('Tools', 'Hand tools and power tools', 3, 4);

-- Insert sample users
INSERT INTO users (username, email, phone_number, password_hash, salt, first_name, last_name, alias, credits) VALUES
('alex_smith', 'alex.smith@example.com', '+1234567890', 'hashed_password_1', 'salt_1', 'Alex', 'Smith', 'AlexS', 150.00),
('jane_doe', 'jane.doe@example.com', '+1234567891', 'hashed_password_2', 'salt_2', 'Jane', 'Doe', 'JaneD', 200.00),
('bob_wilson', 'bob.wilson@example.com', '+1234567892', 'hashed_password_3', 'salt_3', 'Bob', 'Wilson', 'BobW', 100.00);

-- Insert sample items
INSERT INTO items (title, description, category_id, seller_id, starting_price, current_bid, bid_deadline, billing_deadline, item_condition, status, location, buy_now_price) VALUES
('Vintage Camera', 'Beautiful vintage camera in excellent condition', 5, 1, 150.00, 175.00, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), 'good', 'active', 'New York, NY', 250.00),
('Designer Handbag', 'Luxury designer handbag, barely used', 4, 2, 800.00, 850.00, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 'like_new', 'active', 'Los Angeles, CA', 1200.00),
('Modern Sofa', 'Comfortable modern sofa, perfect for living room', 1, 1, 1200.00, 1200.00, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), 'good', 'active', 'Chicago, IL', 1500.00),
('Rare Coin Collection', 'Collection of rare coins from different eras', 9, 3, 50.00, 75.00, DATE_ADD(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY), 'good', 'active', 'Miami, FL', 100.00);

-- =====================================================
-- STEP 3: CREATE STORED PROCEDURES
-- =====================================================

-- Set delimiter for procedure creation
DELIMITER $$

-- Drop existing procedures
DROP PROCEDURE IF EXISTS PlaceBid$$
DROP PROCEDURE IF EXISTS BuyNow$$

-- Create PlaceBid procedure
CREATE PROCEDURE PlaceBid(
    IN p_item_id INT UNSIGNED,
    IN p_bidder_id INT UNSIGNED,
    IN p_amount DECIMAL(10,2),
    IN p_bidder_alias VARCHAR(50)
)
BEGIN
    DECLARE v_current_bid DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_user_credits DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_item_exists INT DEFAULT 0;
    DECLARE v_user_exists INT DEFAULT 0;
    DECLARE v_starting_price DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_previous_bidder_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_previous_bid_amount DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_seller_id INT UNSIGNED DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Lock the bidder's row to prevent race conditions
    SELECT credits INTO v_user_credits
    FROM users
    WHERE id = p_bidder_id
    FOR UPDATE;

    -- Check if item exists and is active
    SELECT COUNT(*), seller_id, starting_price
    INTO v_item_exists, v_seller_id, v_starting_price
    FROM items
    WHERE id = p_item_id AND status = 'active'
    FOR UPDATE;

    IF v_item_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found or not active';
    END IF;

    -- Check bidder is not the seller
    IF p_bidder_id = v_seller_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot bid on your own item';
    END IF;

    -- Check if user exists
    SELECT COUNT(*) INTO v_user_exists
    FROM users
    WHERE id = p_bidder_id;

    IF v_user_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User not found';
    END IF;

    -- Get current highest bid for the item
    SELECT COALESCE(MAX(amount), 0) INTO v_current_bid
    FROM bids
    WHERE item_id = p_item_id AND status IN ('active', 'winning');

    -- Get previous winning bidder info for refund
    SELECT bidder_id, amount INTO v_previous_bidder_id, v_previous_bid_amount
    FROM bids
    WHERE item_id = p_item_id AND status = 'winning'
    ORDER BY created_at DESC
    LIMIT 1;

    -- Validate bid amount
    IF p_amount <= v_current_bid THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid amount must be higher than current bid';
    END IF;

    IF p_amount <= v_starting_price THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid amount must be higher than starting price';
    END IF;

    -- Check if user has enough credits
    IF v_user_credits < p_amount THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits';
    END IF;

    -- Refund previous winning bidder if exists
    IF v_previous_bidder_id IS NOT NULL THEN
        UPDATE users 
        SET credits = credits + v_previous_bid_amount
        WHERE id = v_previous_bidder_id;
        
        -- Update previous bid status
        UPDATE bids 
        SET status = 'outbid'
        WHERE item_id = p_item_id AND bidder_id = v_previous_bidder_id AND status = 'winning';
        
        -- Record refund transaction
        INSERT INTO transactions (user_id, transaction_type, amount, description, item_id)
        VALUES (v_previous_bidder_id, 'outbid_refund', v_previous_bid_amount, 
                CONCAT('Refund for outbid on item ', p_item_id), p_item_id);
    END IF;

    -- Deduct credits from bidder
    UPDATE users 
    SET credits = credits - p_amount
    WHERE id = p_bidder_id;

    -- Record bid transaction
    INSERT INTO transactions (user_id, transaction_type, amount, description, item_id)
    VALUES (p_bidder_id, 'bid', p_amount, 
            CONCAT('Bid placed on item ', p_item_id), p_item_id);

    -- Insert new bid
    INSERT INTO bids (item_id, bidder_id, amount, status)
    VALUES (p_item_id, p_bidder_id, p_amount, 'winning');

    -- Update item current bid
    UPDATE items 
    SET current_bid = p_amount
    WHERE id = p_item_id;

    COMMIT;
    
    SELECT 'Bid placed successfully' AS message, p_amount AS bid_amount;
END$$

-- Create BuyNow procedure
CREATE PROCEDURE BuyNow(
    IN p_item_id INT UNSIGNED,
    IN p_buyer_id INT UNSIGNED,
    IN p_buyer_alias VARCHAR(50)
)
BEGIN
    DECLARE v_buy_now_price DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_user_credits DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_item_exists INT DEFAULT 0;
    DECLARE v_user_exists INT DEFAULT 0;
    DECLARE v_seller_id INT UNSIGNED DEFAULT NULL;
    DECLARE v_seller_credits DECIMAL(10,2) DEFAULT 0.00;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Lock buyer's row
    SELECT credits INTO v_user_credits
    FROM users
    WHERE id = p_buyer_id
    FOR UPDATE;

    -- Check if item exists and get buy now price
    SELECT COUNT(*), seller_id, buy_now_price
    INTO v_item_exists, v_seller_id, v_buy_now_price
    FROM items
    WHERE id = p_item_id AND status = 'active'
    FOR UPDATE;

    IF v_item_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item not found or not active';
    END IF;

    IF v_buy_now_price IS NULL OR v_buy_now_price <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item does not have buy now option';
    END IF;

    -- Check buyer is not the seller
    IF p_buyer_id = v_seller_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot buy your own item';
    END IF;

    -- Check if user exists
    SELECT COUNT(*) INTO v_user_exists
    FROM users
    WHERE id = p_buyer_id;

    IF v_user_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User not found';
    END IF;

    -- Check if user has enough credits
    IF v_user_credits < v_buy_now_price THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient credits';
    END IF;

    -- Lock seller's row
    SELECT credits INTO v_seller_credits
    FROM users
    WHERE id = v_seller_id
    FOR UPDATE;

    -- Deduct credits from buyer
    UPDATE users 
    SET credits = credits - v_buy_now_price
    WHERE id = p_buyer_id;

    -- Add credits to seller
    UPDATE users 
    SET credits = credits + v_buy_now_price
    WHERE id = v_seller_id;

    -- Record buyer transaction
    INSERT INTO transactions (user_id, transaction_type, amount, description, item_id)
    VALUES (p_buyer_id, 'buy_now', v_buy_now_price, 
            CONCAT('Buy now purchase of item ', p_item_id), p_item_id);

    -- Record seller transaction
    INSERT INTO transactions (user_id, transaction_type, amount, description, item_id)
    VALUES (v_seller_id, 'buy_now', v_buy_now_price, 
            CONCAT('Item ', p_item_id, ' sold via buy now'), p_item_id);

    -- Update item status
    UPDATE items 
    SET status = 'sold', current_bid = v_buy_now_price
    WHERE id = p_item_id;

    -- Cancel all active bids for this item
    UPDATE bids 
    SET status = 'cancelled'
    WHERE item_id = p_item_id AND status IN ('active', 'winning');

    COMMIT;
    
    SELECT 'Item purchased successfully' AS message, v_buy_now_price AS purchase_amount;
END$$

-- Reset delimiter
DELIMITER ;

-- =====================================================
-- STEP 4: CREATE VIEWS
-- =====================================================

-- Create active items view
CREATE VIEW active_items AS
SELECT 
    i.id,
    i.title,
    i.description,
    i.starting_price,
    i.current_bid,
    i.buy_now_price,
    i.bid_deadline,
    i.item_condition,
    i.location,
    i.created_at,
    c.name as category_name,
    u.alias as seller_alias
FROM items i
LEFT JOIN categories c ON i.category_id = c.id
LEFT JOIN users u ON i.seller_id = u.id
WHERE i.status = 'active';

-- =====================================================
-- STEP 5: VERIFY SETUP
-- =====================================================

-- Show all tables
SHOW TABLES;

-- Show table counts
SELECT 'Categories' as table_name, COUNT(*) as count FROM categories
UNION ALL
SELECT 'Users', COUNT(*) FROM users
UNION ALL
SELECT 'Items', COUNT(*) FROM items
UNION ALL
SELECT 'Bids', COUNT(*) FROM bids
UNION ALL
SELECT 'Transactions', COUNT(*) FROM transactions;

-- Show procedures
SHOW PROCEDURE STATUS WHERE Db = 'defaultdb';

-- Show views
SHOW FULL TABLES WHERE Table_type = 'VIEW';

SELECT '=======================================================' AS '';
SELECT 'DATABASE RECREATION COMPLETED SUCCESSFULLY!' AS '';
SELECT 'Completed at:', NOW() AS '';
SELECT '=======================================================' AS '';



