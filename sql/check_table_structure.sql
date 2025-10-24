-- Check the actual structure of the users table
USE defaultdb;

-- Check users table structure
DESCRIBE users;

-- Check if updated_at column exists
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'users'
ORDER BY ORDINAL_POSITION;
