-- ADD MISSING COLUMNS TO EXISTING TABLES
-- This will add the missing columns that the queries expect

USE defaultdb;

-- 1. Add missing columns to items table
ALTER TABLE items 
ADD COLUMN IF NOT EXISTS end_date TIMESTAMP NULL,
ADD COLUMN IF NOT EXISTS item_condition VARCHAR(50) DEFAULT 'good',
ADD COLUMN IF NOT EXISTS images TEXT NULL,
ADD COLUMN IF NOT EXISTS metadata TEXT NULL,
ADD COLUMN IF NOT EXISTS location VARCHAR(255) NULL,
ADD COLUMN IF NOT EXISTS buy_now_price DECIMAL(10,2) NULL,
ADD COLUMN IF NOT EXISTS reserve_price DECIMAL(10,2) NULL,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 2. Add missing columns to users table
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS last_login TIMESTAMP NULL;

-- 3. Create bids table if it doesn't exist
CREATE TABLE IF NOT EXISTS bids (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id INT UNSIGNED NOT NULL,
    bidder_id INT UNSIGNED NOT NULL,
    bidder_alias VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('active', 'outbid', 'winning', 'won', 'lost', 'cancelled') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_bids_item_id (item_id),
    INDEX idx_bids_bidder_id (bidder_id),
    INDEX idx_bids_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Verify the updated structure
SELECT '=== UPDATED ITEMS TABLE STRUCTURE ===' as section;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'items'
ORDER BY ORDINAL_POSITION;

-- 5. Verify the updated users table structure
SELECT '=== UPDATED USERS TABLE STRUCTURE ===' as section;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'users'
ORDER BY ORDINAL_POSITION;
