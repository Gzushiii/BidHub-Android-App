-- Fix item posting database issues
-- This ensures all required tables and views exist for item posting

USE defaultdb;

-- ==============================================
-- STEP 1: Create items table if it doesn't exist
-- ==============================================

CREATE TABLE IF NOT EXISTS items (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category_id INT UNSIGNED NOT NULL,
    seller_id INT UNSIGNED NOT NULL,
    starting_price DECIMAL(10,2) NOT NULL,
    reserve_price DECIMAL(10,2) DEFAULT NULL,
    current_price DECIMAL(10,2) NOT NULL,
    buy_now_price DECIMAL(10,2) DEFAULT NULL,
    item_condition ENUM('new', 'used', 'good', 'fair', 'poor') DEFAULT 'good',
    status ENUM('draft', 'active', 'ended', 'sold', 'cancelled') DEFAULT 'draft',
    end_date TIMESTAMP NULL,
    current_bidder_id INT UNSIGNED NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (current_bidder_id) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_items_status (status),
    INDEX idx_items_category (category_id),
    INDEX idx_items_seller (seller_id),
    INDEX idx_items_created (created_at),
    INDEX idx_items_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- STEP 2: Create item_images table if it doesn't exist
-- ==============================================

CREATE TABLE IF NOT EXISTS item_images (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id INT UNSIGNED NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    display_order INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    INDEX idx_item_images_item (item_id),
    INDEX idx_item_images_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- STEP 3: Create v_active_items view if it doesn't exist
-- ==============================================

DROP VIEW IF EXISTS v_active_items;

CREATE VIEW v_active_items AS
SELECT 
    i.id,
    i.title,
    i.description,
    i.category_id,
    i.seller_id,
    i.starting_price,
    i.reserve_price,
    i.current_price,
    i.buy_now_price,
    i.item_condition,
    i.status,
    i.end_date,
    i.current_bidder_id,
    i.created_at,
    i.updated_at,
    u.email as seller_email,
    u.alias as seller_alias,
    c.name as category_name
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
LEFT JOIN categories c ON i.category_id = c.id
WHERE i.status = 'active';

-- ==============================================
-- STEP 4: Create categories table if it doesn't exist
-- ==============================================

CREATE TABLE IF NOT EXISTS categories (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- STEP 5: Insert default categories if they don't exist
-- ==============================================

INSERT IGNORE INTO categories (id, name, description) VALUES
(1, 'Electronics', 'Electronic devices and gadgets'),
(2, 'Clothing', 'Fashion and apparel'),
(3, 'Home & Garden', 'Home improvement and gardening items'),
(4, 'Sports', 'Sports equipment and accessories'),
(5, 'Books', 'Books and educational materials'),
(6, 'Toys', 'Toys and games'),
(7, 'Automotive', 'Car parts and accessories'),
(8, 'Art & Collectibles', 'Artwork and collectible items'),
(9, 'Jewelry', 'Jewelry and accessories'),
(10, 'Other', 'Miscellaneous items');

-- ==============================================
-- STEP 6: Add missing columns to items table if needed
-- ==============================================

-- Check if columns exist before adding them
SET @buy_now_price_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'buy_now_price'
);

SET @item_condition_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'item_condition'
);

SET @current_bidder_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'current_bidder_id'
);

SET @updated_at_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND COLUMN_NAME = 'updated_at'
);

-- Add columns only if they don't exist
SET @sql_buy_now = IF(@buy_now_price_exists = 0,
    'ALTER TABLE items ADD COLUMN buy_now_price DECIMAL(10,2) DEFAULT NULL;',
    'SELECT ''Column buy_now_price already exists'' AS message;'
);
PREPARE stmt_buy_now FROM @sql_buy_now;
EXECUTE stmt_buy_now;
DEALLOCATE PREPARE stmt_buy_now;

SET @sql_condition = IF(@item_condition_exists = 0,
    'ALTER TABLE items ADD COLUMN item_condition ENUM(''new'', ''used'', ''good'', ''fair'', ''poor'') DEFAULT ''good'';',
    'SELECT ''Column item_condition already exists'' AS message;'
);
PREPARE stmt_condition FROM @sql_condition;
EXECUTE stmt_condition;
DEALLOCATE PREPARE stmt_condition;

SET @sql_bidder = IF(@current_bidder_id_exists = 0,
    'ALTER TABLE items ADD COLUMN current_bidder_id INT UNSIGNED NULL;',
    'SELECT ''Column current_bidder_id already exists'' AS message;'
);
PREPARE stmt_bidder FROM @sql_bidder;
EXECUTE stmt_bidder;
DEALLOCATE PREPARE stmt_bidder;

SET @sql_updated = IF(@updated_at_exists = 0,
    'ALTER TABLE items ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;',
    'SELECT ''Column updated_at already exists'' AS message;'
);
PREPARE stmt_updated FROM @sql_updated;
EXECUTE stmt_updated;
DEALLOCATE PREPARE stmt_updated;

-- ==============================================
-- STEP 7: Add foreign key constraints if they don't exist
-- ==============================================

-- Check if foreign key constraints exist
SET @fk_seller_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND CONSTRAINT_NAME = 'fk_items_seller'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @fk_category_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND CONSTRAINT_NAME = 'fk_items_category'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @fk_bidder_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'defaultdb'
      AND TABLE_NAME = 'items'
      AND CONSTRAINT_NAME = 'fk_items_current_bidder'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

-- Add foreign key constraints if they don't exist
SET @sql_seller = IF(@fk_seller_exists = 0,
    'ALTER TABLE items ADD CONSTRAINT fk_items_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE;',
    'SELECT ''Foreign key fk_items_seller already exists'' AS message;'
);
PREPARE stmt_seller FROM @sql_seller;
EXECUTE stmt_seller;
DEALLOCATE PREPARE stmt_seller;

SET @sql_category = IF(@fk_category_exists = 0,
    'ALTER TABLE items ADD CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE;',
    'SELECT ''Foreign key fk_items_category already exists'' AS message;'
);
PREPARE stmt_category FROM @sql_category;
EXECUTE stmt_category;
DEALLOCATE PREPARE stmt_category;

SET @sql_bidder = IF(@fk_bidder_exists = 0,
    'ALTER TABLE items ADD CONSTRAINT fk_items_current_bidder FOREIGN KEY (current_bidder_id) REFERENCES users(id) ON DELETE SET NULL;',
    'SELECT ''Foreign key fk_items_current_bidder already exists'' AS message;'
);
PREPARE stmt_bidder FROM @sql_bidder;
EXECUTE stmt_bidder;
DEALLOCATE PREPARE stmt_bidder;

-- ==============================================
-- STEP 8: Verify the fix
-- ==============================================

SELECT '=== VERIFICATION: ITEM POSTING DATABASE FIXED ===' as section;

-- Check if all tables exist
SELECT 
    TABLE_NAME,
    TABLE_TYPE
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME IN ('items', 'item_images', 'categories', 'v_active_items')
ORDER BY TABLE_NAME;

-- Check items table structure
SELECT '=== ITEMS TABLE STRUCTURE ===' as subsection;
DESCRIBE items;

-- Check item_images table structure
SELECT '=== ITEM_IMAGES TABLE STRUCTURE ===' as subsection;
DESCRIBE item_images;

-- Check categories table structure
SELECT '=== CATEGORIES TABLE STRUCTURE ===' as subsection;
DESCRIBE categories;

-- Check v_active_items view
SELECT '=== V_ACTIVE_ITEMS VIEW ===' as subsection;
SELECT * FROM v_active_items LIMIT 5;

-- ==============================================
-- FINAL STATUS
-- ==============================================

SELECT '=== ITEM POSTING DATABASE FIXED ===' as final_status;
SELECT 'All required tables, views, and constraints have been created' as result;
