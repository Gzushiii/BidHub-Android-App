-- Clean up test users only (keeps any production users)
-- This removes users with test emails and their data

USE defaultdb;

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Delete test users and their data
-- This targets common test email patterns
DELETE FROM credit_transactions 
WHERE user_id IN (
    SELECT id FROM users 
    WHERE email LIKE '%test%' 
    OR email LIKE '%example%' 
    OR email LIKE '%demo%'
    OR email LIKE '%sample%'
);

DELETE FROM bids 
WHERE bidder_id IN (
    SELECT id FROM users 
    WHERE email LIKE '%test%' 
    OR email LIKE '%example%' 
    OR email LIKE '%demo%'
    OR email LIKE '%sample%'
);

DELETE FROM item_images 
WHERE item_id IN (
    SELECT id FROM items 
    WHERE seller_id IN (
        SELECT id FROM users 
        WHERE email LIKE '%test%' 
        OR email LIKE '%example%' 
        OR email LIKE '%demo%'
        OR email LIKE '%sample%'
    )
);

DELETE FROM items 
WHERE seller_id IN (
    SELECT id FROM users 
    WHERE email LIKE '%test%' 
    OR email LIKE '%example%' 
    OR email LIKE '%demo%'
    OR email LIKE '%sample%'
);

DELETE FROM users 
WHERE email LIKE '%test%' 
OR email LIKE '%example%' 
OR email LIKE '%demo%'
OR email LIKE '%sample%';

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Verify cleanup
SELECT '=== TEST USERS CLEANUP VERIFICATION ===' as status;
SELECT 'Remaining users:' as table_name, COUNT(*) as count FROM users;
SELECT 'Remaining items:' as table_name, COUNT(*) as count FROM items;
SELECT 'Remaining bids:' as table_name, COUNT(*) as count FROM bids;
SELECT 'Remaining credit transactions:' as table_name, COUNT(*) as count FROM credit_transactions;

-- Show remaining users
SELECT '=== REMAINING USERS ===' as status;
SELECT id, email, username, alias, credits, created_at FROM users ORDER BY created_at DESC;
