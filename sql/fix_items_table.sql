-- FIX ITEMS TABLE - Add missing columns
-- This will add the missing columns that the queries expect

USE defaultdb;

-- 1. First, let's see what columns currently exist
SELECT '=== CURRENT ITEMS TABLE STRUCTURE ===' as section;
SHOW COLUMNS FROM items;

-- 2. Add missing columns one by one (with error handling)
-- Add end_date column
ALTER TABLE items 
ADD COLUMN end_date TIMESTAMP NULL;

-- Add item_condition column  
ALTER TABLE items 
ADD COLUMN item_condition VARCHAR(50) DEFAULT 'good';

-- Add images column
ALTER TABLE items 
ADD COLUMN images TEXT NULL;

-- Add metadata column
ALTER TABLE items 
ADD COLUMN metadata TEXT NULL;

-- Add location column
ALTER TABLE items 
ADD COLUMN location VARCHAR(255) NULL;

-- Add buy_now_price column
ALTER TABLE items 
ADD COLUMN buy_now_price DECIMAL(10,2) NULL;

-- Add reserve_price column
ALTER TABLE items 
ADD COLUMN reserve_price DECIMAL(10,2) NULL;

-- Add updated_at column
ALTER TABLE items 
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 3. Verify the updated structure
SELECT '=== UPDATED ITEMS TABLE STRUCTURE ===' as section;
SHOW COLUMNS FROM items;

-- 4. Test the query that was failing
SELECT '=== TEST QUERY WITH ALL COLUMNS ===' as section;
SELECT 
    id,
    title,
    description,
    seller_id,
    starting_bid,
    current_bid,
    status,
    created_at,
    end_date,
    item_condition
FROM items 
ORDER BY created_at DESC;
