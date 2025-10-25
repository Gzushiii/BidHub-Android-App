-- Create Test Items for Testing
-- This script creates test items with proper UUIDs and active status

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'CREATING TEST ITEMS FOR TESTING' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Step 1: Check if we have any users to assign as sellers
SELECT 'Step 1: Checking available users...' AS '';
SELECT 
    id,
    email,
    username,
    alias
FROM users 
LIMIT 5;

SELECT '' AS '';

-- Step 2: Check if we have categories
SELECT 'Step 2: Checking available categories...' AS '';
SELECT 
    id,
    name,
    description
FROM categories 
LIMIT 5;

SELECT '' AS '';

-- Step 3: Create test items if none exist
SELECT 'Step 3: Creating test items...' AS '';

-- Get the first user as seller
SET @seller_id = (SELECT id FROM users LIMIT 1);
SET @category_id = (SELECT id FROM categories LIMIT 1);

SELECT CONCAT('Using seller_id: ', @seller_id, ', category_id: ', @category_id) AS info;

-- Create test items
INSERT INTO items (
    uuid_id,
    title,
    description,
    category_id,
    seller_id,
    seller_email,
    starting_bid,
    current_bid,
    buy_now_price,
    status,
    created_at,
    updated_at
) VALUES 
(
    UUID(),
    'Test Item 1 - Electronics',
    'A test electronic item for testing the bidding system',
    @category_id,
    @seller_id,
    (SELECT email FROM users WHERE id = @seller_id),
    100.00,
    100.00,
    500.00,
    'active',
    NOW(),
    NOW()
),
(
    UUID(),
    'Test Item 2 - Clothing',
    'A test clothing item for testing the buy now functionality',
    @category_id,
    @seller_id,
    (SELECT email FROM users WHERE id = @seller_id),
    50.00,
    50.00,
    200.00,
    'active',
    NOW(),
    NOW()
),
(
    UUID(),
    'Test Item 3 - Home & Garden',
    'A test home and garden item for comprehensive testing',
    @category_id,
    @seller_id,
    (SELECT email FROM users WHERE id = @seller_id),
    75.00,
    75.00,
    300.00,
    'active',
    NOW(),
    NOW()
);

SELECT 'Test items created successfully' AS '';

SELECT '' AS '';

-- Step 4: Verify the created items
SELECT 'Step 4: Verifying created items...' AS '';
SELECT 
    id,
    uuid_id,
    title,
    status,
    starting_bid,
    current_bid,
    buy_now_price,
    created_at
FROM items 
WHERE title LIKE 'Test Item%'
ORDER BY created_at DESC;

SELECT '' AS '';

-- Step 5: Check v_active_items view
SELECT 'Step 5: Checking v_active_items view...' AS '';
SELECT COUNT(*) as total_active_items FROM v_active_items;

SELECT 'Active items in view:' AS '';
SELECT 
    id,
    title,
    status,
    starting_bid,
    current_bid
FROM v_active_items 
ORDER BY created_at DESC 
LIMIT 5;

SELECT '' AS '';

SELECT '=======================================================' AS '';
SELECT 'TEST ITEMS CREATION COMPLETE' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';
SELECT 'Test items created with:' AS '';
SELECT '- Active status for visibility' AS '';
SELECT '- Proper UUIDs for API compatibility' AS '';
SELECT '- Realistic pricing for testing' AS '';
SELECT '- Proper seller and category assignments' AS '';
SELECT '' AS '';
SELECT 'You can now test the Android app!' AS '';
