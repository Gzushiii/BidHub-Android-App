-- Fix v_active_items view to match API expectations
-- This script ensures the view exists with the correct column structure

USE defaultdb;

-- Drop view if it exists
DROP VIEW IF EXISTS v_active_items;

-- Create the view with correct column structure
-- API expects: id (integer), uuid_id (UUID), and all other fields
CREATE VIEW v_active_items AS
SELECT
    i.id,                    -- Integer ID (for FK relationships)
    i.uuid_id,               -- UUID ID (for API responses)
    i.title,
    i.description,
    i.category_id,
    i.seller_id,
    -- Price fields - handle both starting_price/starting_bid and current_price/current_bid
    COALESCE(i.starting_price, i.starting_bid, 0) as starting_price,
    COALESCE(i.starting_bid, i.starting_price, 0) as starting_bid,
    i.reserve_price,
    COALESCE(i.current_price, i.current_bid, COALESCE(i.starting_price, i.starting_bid, 0)) as current_price,
    COALESCE(i.current_bid, i.current_price, COALESCE(i.starting_bid, i.starting_price, 0)) as current_bid,
    i.buy_now_price,
    COALESCE(i.item_condition, 'good') as item_condition,
    COALESCE(i.item_condition, 'good') as `condition`,
    i.status,
    i.end_date,
    COALESCE(i.bid_deadline, i.end_date) as bid_deadline,
    i.created_at,
    i.updated_at,
    -- Seller information
    COALESCE(i.seller_email, u.email) as seller_email,
    u.username as seller_username,
    u.alias as seller_alias,
    -- Category information
    c.name as category_name,
    c.description as category_description
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
LEFT JOIN categories c ON i.category_id = c.id
WHERE i.status = 'active';

-- Verify the view was created
SELECT '✓ v_active_items view created successfully' AS '';

-- Test the view
SELECT 'Testing v_active_items view...' AS '';
SELECT COUNT(*) as total_active_items FROM v_active_items;

-- Show view structure
SELECT 'View structure:' AS '';
SHOW COLUMNS FROM v_active_items;

SELECT '' AS '';
SELECT '=======================================================' AS '';
SELECT 'v_active_items VIEW FIX COMPLETED!' AS '';
SELECT '=======================================================' AS '';

