-- =====================================================
-- BidHub MySQL Database Schema
-- =====================================================
-- This DDL file creates the complete database schema for BidHub
-- Based on frontend input fields and Android SQLite structure
-- Optimized for MySQL with best practices
-- =====================================================

-- Create database
CREATE DATABASE IF NOT EXISTS bidhub_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE bidhub_db;

-- =====================================================
-- 1. USERS TABLE
-- =====================================================
-- Stores user account information and authentication data
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
    last_login TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Indexes for performance
    INDEX idx_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_alias (alias),
    INDEX idx_users_created_at (created_at),
    INDEX idx_users_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 2. CATEGORIES TABLE
-- =====================================================
-- Hierarchical category system for item organization
CREATE TABLE categories (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    parent_id INT UNSIGNED NULL,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL,
    
    -- Indexes
    INDEX idx_categories_parent_id (parent_id),
    INDEX idx_categories_is_active (is_active),
    INDEX idx_categories_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 3. ITEMS TABLE
-- =====================================================
-- Auction items and listings
CREATE TABLE items (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category_id INT UNSIGNED NULL,
    seller_id INT UNSIGNED NOT NULL,
    seller_email VARCHAR(255) NULL, -- For compatibility
    starting_bid DECIMAL(10,2) NOT NULL,
    current_bid DECIMAL(10,2) DEFAULT 0.00,
    current_bidder_id INT UNSIGNED NULL,
    bid_deadline DATETIME NOT NULL,
    billing_deadline DATETIME NOT NULL,
    condition ENUM('new', 'like_new', 'good', 'fair', 'poor') NOT NULL,
    images JSON NULL, -- Store image paths as JSON array
    status ENUM('draft', 'active', 'ended', 'sold', 'cancelled') DEFAULT 'draft',
    view_count INT UNSIGNED DEFAULT 0,
    bid_count INT UNSIGNED DEFAULT 0,
    buy_now_price DECIMAL(10,2) NULL,
    location VARCHAR(255) NULL,
    shipping_info TEXT NULL,
    metadata JSON NULL, -- Additional item attributes (size, features, origin, etc.)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (current_bidder_id) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Indexes for performance
    INDEX idx_items_seller_id (seller_id),
    INDEX idx_items_category_id (category_id),
    INDEX idx_items_status (status),
    INDEX idx_items_bid_deadline (bid_deadline),
    INDEX idx_items_created_at (created_at),
    INDEX idx_items_current_bid (current_bid),
    INDEX idx_items_view_count (view_count),
    INDEX idx_items_bid_count (bid_count),
    
    -- Check constraints
    CHECK (starting_bid >= 0),
    CHECK (current_bid >= 0),
    CHECK (buy_now_price IS NULL OR buy_now_price > starting_bid),
    CHECK (billing_deadline > bid_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 4. BIDS TABLE
-- =====================================================
-- All bids placed on items
CREATE TABLE bids (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id INT UNSIGNED NOT NULL,
    bidder_id INT UNSIGNED NOT NULL,
    bidder_email VARCHAR(255) NULL, -- For compatibility
    amount DECIMAL(10,2) NOT NULL,
    bidder_alias VARCHAR(50) NOT NULL,
    is_winning BOOLEAN DEFAULT FALSE,
    status ENUM('ACTIVE', 'OUTBID', 'WON', 'CANCELLED') DEFAULT 'ACTIVE',
    placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_bids_item_id (item_id),
    INDEX idx_bids_bidder_id (bidder_id),
    INDEX idx_bids_amount (amount),
    INDEX idx_bids_placed_at (placed_at),
    INDEX idx_bids_status (status),
    INDEX idx_bids_is_winning (is_winning),
    
    -- Check constraints
    CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 5. CREDIT_TRANSACTIONS TABLE
-- =====================================================
-- Financial transactions and credit management
CREATE TABLE credit_transactions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    type ENUM('purchase', 'redemption', 'bid', 'refund', 'transfer', 'bonus') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description TEXT NULL,
    payment_method ENUM('stripe', 'card', 'test', 'redemption_code') NULL,
    status ENUM('pending', 'completed', 'failed', 'cancelled') DEFAULT 'pending',
    reference VARCHAR(255) NULL, -- Payment reference or item_id for bids
    transaction_id VARCHAR(255) NULL, -- External payment transaction ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    
    -- Foreign key constraints
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_credit_transactions_user_id (user_id),
    INDEX idx_credit_transactions_type (type),
    INDEX idx_credit_transactions_status (status),
    INDEX idx_credit_transactions_created_at (created_at),
    INDEX idx_credit_transactions_reference (reference),
    
    -- Check constraints
    CHECK (amount != 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 6. REDEMPTION_CODES TABLE
-- =====================================================
-- Credit redemption code system
CREATE TABLE redemption_codes (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    user_id INT UNSIGNED NOT NULL,
    credits DECIMAL(10,2) NOT NULL,
    status ENUM('unused', 'used', 'expired', 'cancelled') DEFAULT 'unused',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    used_by_user_id INT UNSIGNED NULL,
    
    -- Foreign key constraints
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (used_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Indexes for performance
    INDEX idx_redemption_codes_code (code),
    INDEX idx_redemption_codes_user_id (user_id),
    INDEX idx_redemption_codes_status (status),
    INDEX idx_redemption_codes_expires_at (expires_at),
    
    -- Check constraints
    CHECK (credits > 0),
    CHECK (expires_at > created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 7. PASSWORD_RECOVERY TABLE
-- =====================================================
-- Password reset verification codes
CREATE TABLE password_recovery (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NULL,
    phone VARCHAR(20) NULL,
    verification_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_email BOOLEAN NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,
    
    -- Indexes for performance
    INDEX idx_password_recovery_email (email),
    INDEX idx_password_recovery_phone (phone),
    INDEX idx_password_recovery_code (verification_code),
    INDEX idx_password_recovery_expires_at (expires_at),
    INDEX idx_password_recovery_is_used (is_used),
    
    -- Check constraints
    CHECK (email IS NOT NULL OR phone IS NOT NULL),
    CHECK (expires_at > created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 8. NOTIFICATIONS TABLE
-- =====================================================
-- User notifications and alerts
CREATE TABLE notifications (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    type ENUM('bid_update', 'auction_ending', 'auction_ended', 'payment', 'system') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    data JSON NULL, -- Additional notification data
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    -- Foreign key constraints
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_type (type),
    INDEX idx_notifications_is_read (is_read),
    INDEX idx_notifications_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 9. WATCHLIST TABLE
-- =====================================================
-- User watchlist for items
CREATE TABLE watchlist (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    item_id INT UNSIGNED NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicates
    UNIQUE KEY unique_user_item (user_id, item_id),
    
    -- Indexes for performance
    INDEX idx_watchlist_user_id (user_id),
    INDEX idx_watchlist_item_id (item_id),
    INDEX idx_watchlist_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 10. ITEM_IMAGES TABLE
-- =====================================================
-- Separate table for item images (normalized approach)
CREATE TABLE item_images (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id INT UNSIGNED NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    image_order INT DEFAULT 0,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_item_images_item_id (item_id),
    INDEX idx_item_images_order (image_order),
    INDEX idx_item_images_is_primary (is_primary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 11. USER_SESSIONS TABLE
-- =====================================================
-- User session management
CREATE TABLE user_sessions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    device_info JSON NULL,
    ip_address VARCHAR(45) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_user_sessions_user_id (user_id),
    INDEX idx_user_sessions_token (session_token),
    INDEX idx_user_sessions_is_active (is_active),
    INDEX idx_user_sessions_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 12. AUDIT_LOGS TABLE
-- =====================================================
-- System audit trail
CREATE TABLE audit_logs (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NULL,
    action VARCHAR(100) NOT NULL,
    table_name VARCHAR(50) NULL,
    record_id INT UNSIGNED NULL,
    old_values JSON NULL,
    new_values JSON NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Indexes for performance
    INDEX idx_audit_logs_user_id (user_id),
    INDEX idx_audit_logs_action (action),
    INDEX idx_audit_logs_table_name (table_name),
    INDEX idx_audit_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- INSERT DEFAULT DATA
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
('Other', 'Miscellaneous items', NULL, 10);

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

-- =====================================================
-- CREATE VIEWS FOR COMMON QUERIES
-- =====================================================

-- Active items with seller and category information
CREATE VIEW v_active_items AS
SELECT 
    i.id,
    i.title,
    i.description,
    i.starting_bid,
    i.current_bid,
    i.bid_deadline,
    i.condition,
    i.status,
    i.view_count,
    i.bid_count,
    i.created_at,
    c.name as category_name,
    u.alias as seller_alias,
    u.email as seller_email,
    TIMESTAMPDIFF(SECOND, NOW(), i.bid_deadline) as seconds_remaining
FROM items i
LEFT JOIN categories c ON i.category_id = c.id
LEFT JOIN users u ON i.seller_id = u.id
WHERE i.status = 'active' AND i.bid_deadline > NOW();

-- User bid history with item details
CREATE VIEW v_user_bids AS
SELECT 
    b.id as bid_id,
    b.amount,
    b.bidder_alias,
    b.placed_at,
    b.is_winning,
    b.status,
    i.title as item_title,
    i.current_bid as item_current_bid,
    i.bid_deadline,
    c.name as category_name
FROM bids b
JOIN items i ON b.item_id = i.id
LEFT JOIN categories c ON i.category_id = c.id
ORDER BY b.placed_at DESC;

-- User credit summary
CREATE VIEW v_user_credits AS
SELECT 
    u.id as user_id,
    u.alias,
    u.credits as current_balance,
    COALESCE(SUM(CASE WHEN ct.type = 'purchase' THEN ct.amount ELSE 0 END), 0) as total_purchased,
    COALESCE(SUM(CASE WHEN ct.type = 'bid' THEN ABS(ct.amount) ELSE 0 END), 0) as total_spent,
    COALESCE(SUM(CASE WHEN ct.type = 'refund' THEN ct.amount ELSE 0 END), 0) as total_refunded,
    COUNT(ct.id) as transaction_count
FROM users u
LEFT JOIN credit_transactions ct ON u.id = ct.user_id AND ct.status = 'completed'
GROUP BY u.id, u.alias, u.credits;

-- =====================================================
-- CREATE STORED PROCEDURES
-- =====================================================

DELIMITER //

-- Procedure to place a bid
CREATE PROCEDURE PlaceBid(
    IN p_item_id INT UNSIGNED,
    IN p_bidder_id INT UNSIGNED,
    IN p_amount DECIMAL(10,2),
    IN p_bidder_alias VARCHAR(50)
)
BEGIN
    DECLARE v_current_bid DECIMAL(10,2) DEFAULT 0;
    DECLARE v_starting_bid DECIMAL(10,2);
    DECLARE v_seller_id INT UNSIGNED;
    DECLARE v_bid_deadline DATETIME;
    DECLARE v_status VARCHAR(20);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Get item details
    SELECT current_bid, starting_bid, seller_id, bid_deadline, status
    INTO v_current_bid, v_starting_bid, v_seller_id, v_bid_deadline, v_status
    FROM items WHERE id = p_item_id;
    
    -- Validate bid
    IF v_status != 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Item is not active for bidding';
    END IF;
    
    IF v_bid_deadline <= NOW() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bidding deadline has passed';
    END IF;
    
    IF p_amount <= v_current_bid THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid amount must be higher than current bid';
    END IF;
    
    IF p_amount < v_starting_bid THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid amount must be at least the starting bid';
    END IF;
    
    IF p_bidder_id = v_seller_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot bid on your own item';
    END IF;
    
    -- Update previous winning bids to outbid
    UPDATE bids 
    SET status = 'OUTBID', is_winning = FALSE 
    WHERE item_id = p_item_id AND is_winning = TRUE;
    
    -- Insert new bid
    INSERT INTO bids (item_id, bidder_id, amount, bidder_alias, is_winning, status)
    VALUES (p_item_id, p_bidder_id, p_amount, p_bidder_alias, TRUE, 'ACTIVE');
    
    -- Update item current bid
    UPDATE items 
    SET current_bid = p_amount, current_bidder_id = p_bidder_id, bid_count = bid_count + 1
    WHERE id = p_item_id;
    
    COMMIT;
END //

-- Procedure to end an auction
CREATE PROCEDURE EndAuction(IN p_item_id INT UNSIGNED)
BEGIN
    DECLARE v_winning_bid_id INT UNSIGNED;
    DECLARE v_winning_bidder_id INT UNSIGNED;
    DECLARE v_winning_amount DECIMAL(10,2);
    DECLARE v_seller_id INT UNSIGNED;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Get winning bid details
    SELECT id, bidder_id, amount
    INTO v_winning_bid_id, v_winning_bidder_id, v_winning_amount
    FROM bids 
    WHERE item_id = p_item_id AND is_winning = TRUE 
    ORDER BY amount DESC, placed_at ASC 
    LIMIT 1;
    
    -- Get seller ID
    SELECT seller_id INTO v_seller_id FROM items WHERE id = p_item_id;
    
    -- Update item status
    UPDATE items SET status = 'ended' WHERE id = p_item_id;
    
    -- If there's a winning bid, update it
    IF v_winning_bid_id IS NOT NULL THEN
        UPDATE bids SET status = 'WON' WHERE id = v_winning_bid_id;
        UPDATE items SET status = 'sold' WHERE id = p_item_id;
    END IF;
    
    COMMIT;
END //

DELIMITER ;

-- =====================================================
-- CREATE TRIGGERS
-- =====================================================

-- Trigger to update item updated_at timestamp
DELIMITER //
CREATE TRIGGER tr_items_updated_at
    BEFORE UPDATE ON items
    FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END //

-- Trigger to update user credits when transaction is completed
CREATE TRIGGER tr_credit_transaction_completed
    AFTER UPDATE ON credit_transactions
    FOR EACH ROW
BEGIN
    IF OLD.status != 'completed' AND NEW.status = 'completed' THEN
        UPDATE users 
        SET credits = credits + NEW.amount 
        WHERE id = NEW.user_id;
    END IF;
END //

-- Trigger to create audit log for user updates
CREATE TRIGGER tr_users_audit
    AFTER UPDATE ON users
    FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, action, table_name, record_id, old_values, new_values)
    VALUES (
        NEW.id,
        'UPDATE',
        'users',
        NEW.id,
        JSON_OBJECT(
            'username', OLD.username,
            'email', OLD.email,
            'credits', OLD.credits,
            'is_active', OLD.is_active
        ),
        JSON_OBJECT(
            'username', NEW.username,
            'email', NEW.email,
            'credits', NEW.credits,
            'is_active', NEW.is_active
        )
    );
END //

DELIMITER ;

-- =====================================================
-- CREATE INDEXES FOR PERFORMANCE
-- =====================================================

-- Composite indexes for common queries
CREATE INDEX idx_items_status_deadline ON items(status, bid_deadline);
CREATE INDEX idx_bids_item_amount ON bids(item_id, amount DESC);
CREATE INDEX idx_credit_transactions_user_type ON credit_transactions(user_id, type);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);

-- =====================================================
-- GRANT PERMISSIONS (Adjust as needed for your environment)
-- =====================================================

-- Create application user (uncomment and modify as needed)
-- CREATE USER 'bidhub_app'@'%' IDENTIFIED BY 'secure_password_here';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON bidhub_db.* TO 'bidhub_app'@'%';
-- FLUSH PRIVILEGES;

-- =====================================================
-- END OF SCHEMA
-- =====================================================


