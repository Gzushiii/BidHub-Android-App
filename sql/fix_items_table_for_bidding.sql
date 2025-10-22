-- Fix items table for bidding functionality
-- This script ensures the items table has all required columns for bidding

USE defaultdb;

-- First, check current structure
SELECT '=== CURRENT ITEMS TABLE STRUCTURE ===' as section;
DESCRIBE items;

-- Add missing columns if they don't exist
ALTER TABLE items 
ADD COLUMN IF NOT EXISTS starting_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS current_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS end_date TIMESTAMP NULL,
ADD COLUMN IF NOT EXISTS item_condition VARCHAR(50) DEFAULT 'good',
ADD COLUMN IF NOT EXISTS status ENUM('draft', 'active', 'ended', 'sold', 'cancelled') DEFAULT 'draft',
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- If starting_bid exists but starting_price doesn't, copy the data
UPDATE items 
SET starting_price = starting_bid 
WHERE starting_price = 0.00 AND starting_bid > 0.00;

-- If current_bid exists but current_price doesn't, copy the data  
UPDATE items 
SET current_price = current_bid 
WHERE current_price = 0.00 AND current_bid > 0.00;

-- Set default end_date for active items if not set
UPDATE items 
SET end_date = DATE_ADD(created_at, INTERVAL 7 DAY) 
WHERE status = 'active' AND end_date IS NULL;

-- Update status to 'active' for items that should be active
UPDATE items 
SET status = 'active' 
WHERE status = 'draft' AND end_date > NOW();

-- Verify the updated structure
SELECT '=== UPDATED ITEMS TABLE STRUCTURE ===' as section;
DESCRIBE items;

-- Show sample data
SELECT '=== SAMPLE ITEMS DATA ===' as section;
SELECT 
    id,
    title,
    starting_price,
    current_price,
    status,
    end_date,
    item_condition,
    created_at
FROM items 
LIMIT 5;

