-- Step 3: Minimal items table (for testing)
-- Run this after Step 2

USE defaultdb;

-- Create minimal items table
CREATE TABLE items (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category_id INT UNSIGNED NULL,
    seller_id INT UNSIGNED NOT NULL,
    seller_email VARCHAR(255) NULL,
    starting_bid DECIMAL(10,2) NOT NULL,
    current_bid DECIMAL(10,2) DEFAULT 0.00,
    status ENUM('draft', 'active', 'ended', 'sold', 'cancelled') DEFAULT 'draft',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verify table created
SHOW TABLES;
