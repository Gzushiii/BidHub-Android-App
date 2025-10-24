-- Add updated_at column to users table (MySQL compatible version)
USE defaultdb;

-- Check if updated_at column already exists
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'updated_at'
);

-- Only add the column if it doesn't exist
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE users ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;',
    'SELECT "Column updated_at already exists" AS message;'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify the column was added
DESCRIBE users;
