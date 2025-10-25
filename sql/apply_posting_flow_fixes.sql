-- Apply Posting Flow Fixes
-- This script applies all database changes needed for the posting flow fixes

USE defaultdb;

-- ==============================================
-- STEP 1: Update v_active_items view to exclude drafts
-- ==============================================

-- Drop existing view
DROP VIEW IF EXISTS v_active_items;

-- Recreate view excluding drafts
CREATE VIEW v_active_items AS
SELECT 
    i.id,
    i.title,
    i.description,
    i.category_id,
    i.seller_id,
    i.seller_email,
    i.starting_bid,
    i.current_bid,
    i.status,
    i.created_at,
    i.updated_at,
    u.username as seller_username,
    u.email as seller_user_email,
    c.name as category_name,
    c.description as category_description
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
LEFT JOIN categories c ON i.category_id = c.id
WHERE i.status = 'active';  -- Only active items, exclude drafts

-- ==============================================
-- STEP 2: Ensure items table supports draft status properly
-- ==============================================

-- Add missing columns if they don't exist
ALTER TABLE items 
ADD COLUMN IF NOT EXISTS starting_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS current_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS end_date TIMESTAMP NULL,
ADD COLUMN IF NOT EXISTS item_condition VARCHAR(50) DEFAULT 'good',
ADD COLUMN IF NOT EXISTS status ENUM('draft', 'active', 'ended', 'sold', 'cancelled') DEFAULT 'draft',
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- ==============================================
-- STEP 3: Fix existing data
-- ==============================================

-- Copy data from old columns if they exist
UPDATE items 
SET starting_price = starting_bid 
WHERE starting_price = 0.00 AND starting_bid > 0.00;

UPDATE items 
SET current_price = current_bid 
WHERE current_price = 0.00 AND current_bid > 0.00;

-- Set end_date for active items that don't have it
UPDATE items 
SET end_date = DATE_ADD(created_at, INTERVAL 7 DAY) 
WHERE status = 'active' AND end_date IS NULL;

-- Ensure draft items don't have end_date set
UPDATE items 
SET end_date = NULL 
WHERE status = 'draft' AND end_date IS NOT NULL;

-- ==============================================
-- STEP 4: Verify the fixes
-- ==============================================

-- Check view was created correctly
SELECT '=== v_active_items VIEW CREATED ===' as status;
SHOW TABLES LIKE 'v_%';

-- Test the view
SELECT '=== ACTIVE ITEMS COUNT ===' as status;
SELECT COUNT(*) as active_items_count FROM v_active_items;

-- Check items by status
SELECT '=== ITEMS BY STATUS ===' as status;
SELECT 
    status,
    COUNT(*) as count,
    COUNT(CASE WHEN end_date IS NULL THEN 1 END) as without_end_date,
    COUNT(CASE WHEN end_date IS NOT NULL THEN 1 END) as with_end_date
FROM items 
GROUP BY status;

-- Sample data verification
SELECT '=== SAMPLE ITEMS ===' as status;
SELECT 
    id,
    title,
    status,
    end_date,
    created_at
FROM items 
ORDER BY created_at DESC 
LIMIT 10;

-- ==============================================
-- STEP 5: Create indexes for performance
-- ==============================================

-- Add indexes if they don't exist
CREATE INDEX IF NOT EXISTS idx_items_status ON items(status);
CREATE INDEX IF NOT EXISTS idx_items_seller ON items(seller_id);
CREATE INDEX IF NOT EXISTS idx_items_category ON items(category_id);
CREATE INDEX IF NOT EXISTS idx_items_end_date ON items(end_date);
CREATE INDEX IF NOT EXISTS idx_items_created ON items(created_at);

-- ==============================================
-- STEP 6: Final verification
-- ==============================================

SELECT '=== FINAL VERIFICATION ===' as status;

-- Verify draft items exist and have no end_date
SELECT 
    'Draft items without end_date' as check_type,
    COUNT(*) as count
FROM items 
WHERE status = 'draft' AND end_date IS NULL

UNION ALL

-- Verify active items have end_date
SELECT 
    'Active items with end_date' as check_type,
    COUNT(*) as count
FROM items 
WHERE status = 'active' AND end_date IS NOT NULL

UNION ALL

-- Verify view excludes drafts
SELECT 
    'Items in v_active_items view' as check_type,
    COUNT(*) as count
FROM v_active_items

UNION ALL

-- Verify total active items
SELECT 
    'Total active items in table' as check_type,
    COUNT(*) as count
FROM items 
WHERE status = 'active';

SELECT '=== POSTING FLOW FIXES APPLIED SUCCESSFULLY ===' as status;
