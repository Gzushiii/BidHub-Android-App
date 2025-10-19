-- CHECK ACTUAL DATABASE SCHEMA
-- This will show us what columns actually exist in each table

USE defaultdb;

-- 1. Check what tables exist
SELECT '=== AVAILABLE TABLES ===' as section;
SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    CREATE_TIME
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb'
ORDER BY TABLE_NAME;

-- 2. Check users table structure
SELECT '=== USERS TABLE STRUCTURE ===' as section;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'users'
ORDER BY ORDINAL_POSITION;

-- 3. Check items table structure
SELECT '=== ITEMS TABLE STRUCTURE ===' as section;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'items'
ORDER BY ORDINAL_POSITION;

-- 4. Check bids table structure
SELECT '=== BIDS TABLE STRUCTURE ===' as section;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'bids'
ORDER BY ORDINAL_POSITION;

-- 5. Check categories table structure (if it exists)
SELECT '=== CATEGORIES TABLE STRUCTURE ===' as section;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'defaultdb' 
    AND TABLE_NAME = 'categories'
ORDER BY ORDINAL_POSITION;
