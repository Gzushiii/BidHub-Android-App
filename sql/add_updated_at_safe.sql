-- Safe version to add updated_at column (checks if it exists first)
USE defaultdb;

-- Check if the column already exists
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'Column updated_at already exists'
        ELSE 'Column updated_at does not exist - will add it'
    END as column_status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'users' 
AND COLUMN_NAME = 'updated_at';

-- Add the column (this will fail if it already exists, but that's okay)
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Verify the final structure
DESCRIBE users;
