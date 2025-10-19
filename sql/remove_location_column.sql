-- REMOVE LOCATION COLUMN FROM ITEMS TABLE
-- This will remove the location column from the items table

USE defaultdb;

-- 1. First, let's check if the location column exists
SELECT '=== CHECKING IF LOCATION COLUMN EXISTS ===' as section;
SELECT COLUMN_NAME 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'items' 
    AND COLUMN_NAME = 'location';

-- 2. Remove the location column if it exists
ALTER TABLE items DROP COLUMN IF EXISTS location;

-- 3. Verify the column has been removed
SELECT '=== VERIFYING LOCATION COLUMN REMOVAL ===' as section;
SELECT COLUMN_NAME 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'items' 
    AND COLUMN_NAME = 'location';

-- 4. Show updated table structure
SELECT '=== UPDATED ITEMS TABLE STRUCTURE ===' as section;
SHOW COLUMNS FROM items;
