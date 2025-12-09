-- =====================================================
-- Add Seller Rating Columns to Users Table
-- =====================================================
-- This migration adds seller_rating and seller_review_count columns
-- to the users table to support dynamic seller ratings
-- =====================================================

USE defaultdb;

SELECT 'Adding seller rating columns to users table...' AS '';

-- Add seller_rating column if it doesn't exist
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'seller_rating';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN seller_rating DECIMAL(3,2) DEFAULT 0.00 NOT NULL AFTER is_active',
    'SELECT "Column seller_rating already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add seller_review_count column if it doesn't exist
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'seller_review_count';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN seller_review_count INT UNSIGNED DEFAULT 0 NOT NULL AFTER seller_rating',
    'SELECT "Column seller_review_count already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add check constraint for rating (only if it doesn't exist)
SET @constraint_exists = 0;
SELECT COUNT(*) INTO @constraint_exists 
FROM information_schema.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'users' 
  AND CONSTRAINT_NAME = 'chk_users_seller_rating';

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE users ADD CONSTRAINT chk_users_seller_rating CHECK (seller_rating >= 0 AND seller_rating <= 5)',
    'SELECT "Constraint chk_users_seller_rating already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add indexes for seller rating queries (only if they don't exist)
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'users' 
  AND INDEX_NAME = 'idx_users_seller_rating';

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_users_seller_rating ON users(seller_rating)',
    'SELECT "Index idx_users_seller_rating already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND TABLE_NAME = 'users' 
  AND INDEX_NAME = 'idx_users_seller_review_count';

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_users_seller_review_count ON users(seller_review_count)',
    'SELECT "Index idx_users_seller_review_count already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '✓ Seller rating columns added successfully' AS '';
SELECT '' AS '';

-- Note: In a production system, you would calculate these values from actual reviews/ratings
-- For now, we'll set default values. You can update them when implementing the review system.

