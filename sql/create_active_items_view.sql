-- Create view for active items with seller information
-- This view provides a comprehensive view of active items with seller details

USE defaultdb;

-- Drop view if it exists
DROP VIEW IF EXISTS v_active_items;

-- Create the view
-- FIX: Include both starting_price/starting_bid and current_price/current_bid for compatibility
CREATE VIEW v_active_items AS
SELECT
    i.id,
    i.uuid_id,
    i.title,
    i.description,
    i.category_id,
    i.seller_id,
    i.starting_price,
    COALESCE(i.starting_bid, i.starting_price) as starting_bid,
    i.reserve_price,
    i.current_price,
    COALESCE(i.current_bid, i.current_price) as current_bid,
    i.buy_now_price,
    i.item_condition,
    i.status,
    i.end_date,
    COALESCE(i.bid_deadline, i.end_date) as bid_deadline,
    i.current_bidder_id,
    i.created_at,
    i.updated_at,
    u.email as seller_email,
    u.username as seller_username,
    u.alias as seller_alias,
    c.name as category_name
FROM items i
LEFT JOIN users u ON i.seller_id = u.id
LEFT JOIN categories c ON i.category_id = c.id
WHERE i.status = 'active';

-- Verify the view was created
SHOW TABLES LIKE 'v_%';

-- Test the view
SELECT COUNT(*) as total_active_items FROM v_active_items;

