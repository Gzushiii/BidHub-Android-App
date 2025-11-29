-- =====================================================
-- BidHub MySQL Database Schema
-- =====================================================
-- Comprehensive DDL script for BidHub mobile bidding platform
-- Based on frontend requirements, Java models, and system documentation
-- 
-- Features:
-- - Proper normalization (3NF)
-- - Foreign key constraints with appropriate actions
-- - Comprehensive indexing for performance
-- - Consistent naming conventions (snake_case)
-- - Appropriate data types and constraints
-- - Support for all app features including bidding, credits, and redemption
-- =====================================================

-- Create database
CREATE DATABASE IF NOT EXISTS bidhub_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Use the database
USE bidhub_db;

-- =====================================================
-- TABLE: users
-- =====================================================
-- Stores user account information including authentication
-- and credit balance. Supports alias-based privacy system.
-- =====================================================
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
    credits DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE NOT NULL,
    profile_picture VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    last_login TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    
    -- Indexes for common queries
    INDEX idx_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_alias (alias),
    INDEX idx_users_phone_number (phone_number),
    INDEX idx_users_is_active (is_active),
    INDEX idx_users_created_at (created_at),
    INDEX idx_users_is_verified (is_verified)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: categories
-- =====================================================
-- Hierarchical category system for item organization
-- Supports parent-child relationships for subcategories
-- =====================================================
CREATE TABLE categories (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    parent_id INT UNSIGNED NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    sort_order INT DEFAULT 0 NOT NULL,
    icon_path VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraint for parent category
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL,
    
    -- Indexes
    INDEX idx_categories_parent_id (parent_id),
    INDEX idx_categories_is_active (is_active),
    INDEX idx_categories_sort_order (sort_order),
    INDEX idx_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: items
-- =====================================================
-- Stores auction item listings with all details
-- Includes bidding information, metadata, and analytics
-- Supports draft, active, ended, sold, and cancelled statuses
-- =====================================================
CREATE TABLE items (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category_id INT UNSIGNED NULL,
    seller_id INT UNSIGNED NOT NULL,
    starting_bid DECIMAL(10,2) NOT NULL,
    current_bid DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
    buy_now_price DECIMAL(10,2) NULL,
    current_bidder_id INT UNSIGNED NULL,
    bid_deadline DATETIME NOT NULL,
    billing_deadline DATETIME NOT NULL,
    condition VARCHAR(50) NOT NULL,
    images JSON NULL,
    status ENUM('draft', 'active', 'paused', 'ended', 'sold', 'cancelled') DEFAULT 'draft' NOT NULL,
    currency VARCHAR(3) DEFAULT 'PHP' NOT NULL,
    view_count INT UNSIGNED DEFAULT 0 NOT NULL,
    bid_count INT UNSIGNED DEFAULT 0 NOT NULL,
    rating DECIMAL(3,2) DEFAULT 0.00 NOT NULL,
    review_count INT UNSIGNED DEFAULT 0 NOT NULL,
    shipping_info TEXT NULL,
    tags JSON NULL,
    is_featured BOOLEAN DEFAULT FALSE NOT NULL,
    is_trending BOOLEAN DEFAULT FALSE NOT NULL,
    notes TEXT NULL,
    metadata JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (current_bidder_id) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Check constraints for data integrity
    CONSTRAINT chk_items_starting_bid CHECK (starting_bid >= 0),
    CONSTRAINT chk_items_current_bid CHECK (current_bid >= 0),
    CONSTRAINT chk_items_buy_now_price CHECK (buy_now_price IS NULL OR buy_now_price > 0),
    CONSTRAINT chk_items_bid_deadline CHECK (bid_deadline > created_at),
    CONSTRAINT chk_items_billing_deadline CHECK (billing_deadline >= bid_deadline),
    CONSTRAINT chk_items_rating CHECK (rating >= 0 AND rating <= 5),
    
    -- Indexes for common queries
    INDEX idx_items_seller_id (seller_id),
    INDEX idx_items_category_id (category_id),
    INDEX idx_items_status (status),
    INDEX idx_items_current_bidder_id (current_bidder_id),
    INDEX idx_items_bid_deadline (bid_deadline),
    INDEX idx_items_created_at (created_at),
    INDEX idx_items_is_featured (is_featured),
    INDEX idx_items_is_trending (is_trending),
    INDEX idx_items_status_deadline (status, bid_deadline),
    INDEX idx_items_seller_status (seller_id, status),
    FULLTEXT INDEX idx_items_title_description (title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: bids
-- =====================================================
-- Tracks all bids placed on items
-- Includes bidder information, bid status, and metadata
-- Supports multiple bid statuses for auction lifecycle
-- =====================================================
CREATE TABLE bids (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id INT UNSIGNED NOT NULL,
    bidder_id INT UNSIGNED NOT NULL,
    bidder_alias VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_winning BOOLEAN DEFAULT FALSE NOT NULL,
    status ENUM('pending', 'active', 'winning', 'outbid', 'cancelled', 'expired', 'invalid') DEFAULT 'pending' NOT NULL,
    notes TEXT NULL,
    metadata JSON NULL,
    
    -- Foreign key constraints
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE RESTRICT,
    
    -- Check constraints
    CONSTRAINT chk_bids_amount CHECK (amount > 0),
    
    -- Indexes for common queries
    INDEX idx_bids_item_id (item_id),
    INDEX idx_bids_bidder_id (bidder_id),
    INDEX idx_bids_status (status),
    INDEX idx_bids_is_winning (is_winning),
    INDEX idx_bids_placed_at (placed_at),
    INDEX idx_bids_item_status (item_id, status),
    INDEX idx_bids_bidder_item (bidder_id, item_id),
    INDEX idx_bids_amount (amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: credit_transactions
-- =====================================================
-- Records all credit-related transactions
-- Tracks purchases, redemptions, bids, refunds, and transfers
-- Provides complete audit trail for financial operations
-- =====================================================
CREATE TABLE credit_transactions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    type ENUM('purchase', 'redemption', 'bid', 'refund', 'transfer', 'adjustment') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description TEXT NULL,
    payment_method VARCHAR(50) NULL,
    status ENUM('pending', 'completed', 'failed', 'cancelled') DEFAULT 'pending' NOT NULL,
    reference VARCHAR(255) NULL,
    from_user_id INT UNSIGNED NULL,
    to_user_id INT UNSIGNED NULL,
    audit_trail JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_credit_transactions_amount CHECK (amount != 0),
    
    -- Indexes for common queries
    INDEX idx_credit_transactions_user_id (user_id),
    INDEX idx_credit_transactions_type (type),
    INDEX idx_credit_transactions_status (status),
    INDEX idx_credit_transactions_created_at (created_at),
    INDEX idx_credit_transactions_reference (reference),
    INDEX idx_credit_transactions_user_type (user_id, type),
    INDEX idx_credit_transactions_user_status (user_id, status),
    INDEX idx_credit_transactions_payment_method (payment_method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: redemption_codes
-- =====================================================
-- Manages credit redemption codes
-- Tracks generation, delivery, and redemption lifecycle
-- Supports secure code distribution via email/SMS
-- =====================================================
CREATE TABLE redemption_codes (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    user_id INT UNSIGNED NOT NULL,
    credits DECIMAL(10,2) NOT NULL,
    status ENUM('generated', 'sent', 'delivered', 'redeemed', 'expired', 'invalid') DEFAULT 'generated' NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at DATETIME NOT NULL,
    redeemed_at TIMESTAMP NULL,
    redeemed_by VARCHAR(255) NULL,
    delivery_method VARCHAR(50) NULL,
    delivery_address VARCHAR(500) NULL,
    generated_by VARCHAR(255) NULL,
    notes TEXT NULL,
    usage_count INT UNSIGNED DEFAULT 0 NOT NULL,
    max_usage INT UNSIGNED DEFAULT 1 NOT NULL,
    transaction_id VARCHAR(255) NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    
    -- Check constraints
    CONSTRAINT chk_redemption_codes_credits CHECK (credits > 0),
    CONSTRAINT chk_redemption_codes_expires_at CHECK (expires_at > generated_at),
    CONSTRAINT chk_redemption_codes_usage_count CHECK (usage_count <= max_usage),
    
    -- Indexes for common queries
    INDEX idx_redemption_codes_code (code),
    INDEX idx_redemption_codes_user_id (user_id),
    INDEX idx_redemption_codes_status (status),
    INDEX idx_redemption_codes_expires_at (expires_at),
    INDEX idx_redemption_codes_is_active (is_active),
    INDEX idx_redemption_codes_transaction_id (transaction_id),
    INDEX idx_redemption_codes_delivery_method (delivery_method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- VIEWS
-- =====================================================
-- Create useful views for common queries
-- =====================================================

-- View for active items with seller and category information
CREATE OR REPLACE VIEW v_active_items AS
SELECT 
    i.id,
    i.title,
    i.description,
    i.category_id,
    c.name AS category_name,
    i.seller_id,
    u.username AS seller_username,
    u.alias AS seller_alias,
    i.starting_bid,
    i.current_bid,
    i.buy_now_price,
    i.current_bidder_id,
    i.bid_deadline,
    i.billing_deadline,
    i.condition,
    i.images,
    i.status,
    i.currency,
    i.view_count,
    i.bid_count,
    i.rating,
    i.review_count,
    i.shipping_info,
    i.tags,
    i.is_featured,
    i.is_trending,
    i.created_at,
    i.updated_at
FROM items i
LEFT JOIN categories c ON i.category_id = c.id
LEFT JOIN users u ON i.seller_id = u.id
WHERE i.status = 'active' AND i.bid_deadline > NOW();

-- View for user bid history
CREATE OR REPLACE VIEW v_user_bids AS
SELECT 
    b.id,
    b.item_id,
    i.title AS item_title,
    b.bidder_id,
    b.bidder_alias,
    b.amount,
    b.placed_at,
    b.is_winning,
    b.status,
    i.status AS item_status,
    i.bid_deadline
FROM bids b
JOIN items i ON b.item_id = i.id
ORDER BY b.placed_at DESC;

-- View for credit transaction summary
CREATE OR REPLACE VIEW v_credit_summary AS
SELECT 
    user_id,
    type,
    COUNT(*) AS transaction_count,
    SUM(CASE WHEN status = 'completed' THEN amount ELSE 0 END) AS total_amount,
    MIN(created_at) AS first_transaction,
    MAX(created_at) AS last_transaction
FROM credit_transactions
GROUP BY user_id, type;

-- =====================================================
-- DEFAULT DATA
-- =====================================================
-- Insert default categories for the marketplace
-- =====================================================
INSERT INTO categories (name, description, sort_order) VALUES 
('Electronics', 'Electronic devices and accessories', 1),
('Clothing & Accessories', 'Apparel, shoes, and fashion accessories', 2),
('Home & Garden', 'Home decor, furniture, and garden supplies', 3),
('Sports & Recreation', 'Sports equipment and recreational items', 4),
('Books & Media', 'Books, movies, music, and games', 5),
('Collectibles', 'Rare items, antiques, and collectibles', 6),
('Automotive', 'Car parts, accessories, and automotive items', 7),
('Health & Beauty', 'Health products and beauty items', 8),
('Toys & Games', 'Toys, board games, and gaming items', 9),
('Other', 'Miscellaneous items', 10);

-- =====================================================
-- VERIFICATION
-- =====================================================
-- Verify all tables and views were created successfully
-- =====================================================
SHOW TABLES;

-- Display table structures
DESCRIBE users;
DESCRIBE categories;
DESCRIBE items;
DESCRIBE bids;
DESCRIBE credit_transactions;
DESCRIBE redemption_codes;

-- Show views
SHOW FULL TABLES WHERE Table_type = 'VIEW';
