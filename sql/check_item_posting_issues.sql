-- Comprehensive analysis of item posting issues
-- This checks if items are being properly saved to the database

USE defaultdb;

-- ==============================================
-- STEP 1: Check if items table exists and has correct structure
-- ==============================================

SELECT '=== ITEMS TABLE STRUCTURE ===' as section;

DESCRIBE items;

-- ==============================================
-- STEP 2: Check all items in the database
-- ==============================================

SELECT '=== ALL ITEMS IN DATABASE ===' as section;

SELECT 
    id,
    title,
    description,
    category_id,
    seller_id,
    starting_price,
    current_price,
    status,
    created_at,
    updated_at,
    end_date
FROM items 
ORDER BY created_at DESC;

-- ==============================================
-- STEP 3: Check items by specific user (testuser444@example.com)
-- ==============================================

SELECT '=== ITEMS BY TESTUSER444 ===' as section;

SELECT 
    i.id,
    i.title,
    i.description,
    i.category_id,
    i.seller_id,
    i.starting_price,
    i.current_price,
    i.status,
    i.created_at,
    i.updated_at,
    u.email as seller_email
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
WHERE u.email = 'testuser444@example.com'
ORDER BY i.created_at DESC;

-- ==============================================
-- STEP 4: Check if v_active_items view exists
-- ==============================================

SELECT '=== V_ACTIVE_ITEMS VIEW CHECK ===' as section;

SELECT 
    TABLE_NAME,
    TABLE_TYPE
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'v_active_items';

-- ==============================================
-- STEP 5: Check item_images table
-- ==============================================

SELECT '=== ITEM_IMAGES TABLE CHECK ===' as section;

SELECT 
    TABLE_NAME,
    TABLE_TYPE
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'item_images';

-- If table exists, show its structure
SELECT '=== ITEM_IMAGES TABLE STRUCTURE ===' as subsection;
DESCRIBE item_images;

-- ==============================================
-- STEP 6: Check for any missing tables
-- ==============================================

SELECT '=== MISSING TABLES CHECK ===' as section;

SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'EXISTS'
        ELSE 'MISSING'
    END as items_table_status
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'items';

SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'EXISTS'
        ELSE 'MISSING'
    END as item_images_table_status
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'item_images';

SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'EXISTS'
        ELSE 'MISSING'
    END as v_active_items_view_status
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME = 'v_active_items';

-- ==============================================
-- STEP 7: Check for any foreign key constraints
-- ==============================================

SELECT '=== FOREIGN KEY CONSTRAINTS ===' as section;

SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = 'defaultdb' 
AND REFERENCED_TABLE_NAME IS NOT NULL
AND TABLE_NAME IN ('items', 'item_images');

-- ==============================================
-- STEP 8: Check for any recent item creation attempts
-- ==============================================

SELECT '=== RECENT ITEMS (LAST 24 HOURS) ===' as section;

SELECT 
    id,
    title,
    description,
    seller_id,
    status,
    created_at
FROM items 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY created_at DESC;

-- ==============================================
-- STEP 9: Check if there are any items with missing seller_id
-- ==============================================

SELECT '=== ITEMS WITH MISSING SELLER_ID ===' as section;

SELECT 
    id,
    title,
    seller_id,
    created_at
FROM items 
WHERE seller_id IS NULL OR seller_id = 0;

-- ==============================================
-- STEP 10: Check if there are any items with invalid status
-- ==============================================

SELECT '=== ITEMS WITH INVALID STATUS ===' as section;

SELECT 
    id,
    title,
    status,
    created_at
FROM items 
WHERE status NOT IN ('draft', 'active', 'ended', 'sold', 'cancelled');

-- ==============================================
-- STEP 11: Check if there are any items with missing required fields
-- ==============================================

SELECT '=== ITEMS WITH MISSING REQUIRED FIELDS ===' as section;

SELECT 
    id,
    title,
    description,
    category_id,
    seller_id,
    starting_price,
    status,
    created_at
FROM items 
WHERE title IS NULL OR title = '' 
   OR description IS NULL OR description = ''
   OR category_id IS NULL 
   OR seller_id IS NULL 
   OR starting_price IS NULL;

-- ==============================================
-- STEP 12: Check if there are any items with invalid prices
-- ==============================================

SELECT '=== ITEMS WITH INVALID PRICES ===' as section;

SELECT 
    id,
    title,
    starting_price,
    current_price,
    status,
    created_at
FROM items 
WHERE starting_price IS NULL 
   OR starting_price < 0 
   OR current_price IS NULL 
   OR current_price < 0;

-- ==============================================
-- STEP 13: Check if there are any items with invalid dates
-- ==============================================

SELECT '=== ITEMS WITH INVALID DATES ===' as section;

SELECT 
    id,
    title,
    created_at,
    updated_at,
    end_date,
    status
FROM items 
WHERE created_at IS NULL 
   OR updated_at IS NULL 
   OR (end_date IS NOT NULL AND end_date < created_at);

-- ==============================================
-- FINAL SUMMARY
-- ==============================================

SELECT '=== FINAL SUMMARY ===' as section;

SELECT 
    COUNT(*) as total_items,
    COUNT(CASE WHEN status = 'active' THEN 1 END) as active_items,
    COUNT(CASE WHEN status = 'draft' THEN 1 END) as draft_items,
    COUNT(CASE WHEN status = 'ended' THEN 1 END) as ended_items,
    COUNT(CASE WHEN status = 'sold' THEN 1 END) as sold_items,
    COUNT(CASE WHEN status = 'cancelled' THEN 1 END) as cancelled_items
FROM items;
