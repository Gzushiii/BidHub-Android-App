-- =====================================================
-- MIGRATION: Fix generated_ref column size
-- =====================================================
-- Issue: The generated_ref column is VARCHAR(16) but the generated
-- reference codes are 17 characters long (TOPUP + YYYYMMDD + 4-digit sequence)
-- 
-- Solution: Increase column size to VARCHAR(50) to accommodate
-- the current format and allow for future expansion
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'MIGRATING: Fixing generated_ref column size' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Check current column size
SELECT 
    COLUMN_NAME,
    CHARACTER_MAXIMUM_LENGTH as current_size,
    CASE 
        WHEN CHARACTER_MAXIMUM_LENGTH < 20 THEN 'NEEDS UPDATE'
        ELSE 'OK'
    END as status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'topups' 
AND COLUMN_NAME = 'generated_ref';

SELECT '' AS '';

-- Alter the column to increase size
ALTER TABLE topups 
MODIFY COLUMN generated_ref VARCHAR(50) NOT NULL UNIQUE;

SELECT '✓ generated_ref column updated to VARCHAR(50)' AS '';
SELECT '' AS '';

-- Verify the change
SELECT 
    COLUMN_NAME,
    CHARACTER_MAXIMUM_LENGTH as new_size,
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'topups' 
AND COLUMN_NAME = 'generated_ref';

SELECT '' AS '';
SELECT 'Migration completed successfully!' AS '';
SELECT '=======================================================' AS '';

