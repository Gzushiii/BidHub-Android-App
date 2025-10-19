-- Create view for active items with seller information
-- This view provides a comprehensive view of active items with seller details

USE defaultdb;

-- Drop view if it exists
DROP VIEW IF EXISTS v_active_items;

-- Create the view
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
WHERE i.status IN ('active', 'draft');

-- Verify the view was created
SHOW TABLES LIKE 'v_%';

-- Test the view
SELECT COUNT(*) as total_active_items FROM v_active_items;

