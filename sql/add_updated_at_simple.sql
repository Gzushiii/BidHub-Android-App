-- Simple version to add updated_at column
USE defaultdb;

-- Add the updated_at column
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Verify the column was added
DESCRIBE users;
