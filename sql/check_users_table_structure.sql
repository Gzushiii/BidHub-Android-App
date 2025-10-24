-- Check the current structure of the users table
USE defaultdb;

-- Check if users table exists and its structure
DESCRIBE users;

-- Check what columns actually exist
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'users'
ORDER BY ORDINAL_POSITION;
