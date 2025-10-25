-- Create view for active items with seller information
-- This view provides a comprehensive view of active items with seller details

USE defaultdb;

-- Drop view if it exists
DROP VIEW IF EXISTS v_active_items;

-- Create the view
CREATE VIEW v_active_items AS
SELECT
    i.id,
    i.uuid_id,
    i.title,
    i.description,
    i.category_id,
    i.seller_id,
    i.starting_price,
    i.reserve_price,
    i.current_price,
    i.buy_now_price,
    i.item_condition,
    i.status,
    i.end_date,
    i.current_bidder_id,
    i.created_at,
    i.updated_at,
    u.email as seller_email,
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

