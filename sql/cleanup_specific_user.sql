-- Clean up a specific user and their linked data
-- Replace 'user_email@example.com' with the actual email you want to remove

USE defaultdb;

-- Set the email of the user to remove
SET @user_email = 'user_email@example.com';

-- Get user ID first
SET @user_id = (SELECT id FROM users WHERE email = @user_email);

SELECT CONCAT('Removing user: ', @user_email, ' (ID: ', @user_id, ')') as action;

-- If user exists, proceed with cleanup
IF @user_id IS NOT NULL THEN
    -- Disable foreign key checks temporarily
    SET FOREIGN_KEY_CHECKS = 0;
    
    -- Delete user's credit transactions
    DELETE FROM credit_transactions WHERE user_id = @user_id;
    SELECT 'Deleted credit transactions' as status;
    
    -- Delete user's bids
    DELETE FROM bids WHERE bidder_id = @user_id;
    SELECT 'Deleted bids' as status;
    
    -- Delete user's items and their images
    DELETE FROM item_images WHERE item_id IN (SELECT id FROM items WHERE seller_id = @user_id);
    DELETE FROM items WHERE seller_id = @user_id;
    SELECT 'Deleted items and images' as status;
    
    -- Finally delete the user
    DELETE FROM users WHERE id = @user_id;
    SELECT 'Deleted user' as status;
    
    -- Re-enable foreign key checks
    SET FOREIGN_KEY_CHECKS = 1;
    
    SELECT 'User cleanup completed successfully' as result;
ELSE
    SELECT 'User not found' as result;
END IF;

-- Verify cleanup
SELECT '=== CLEANUP VERIFICATION ===' as status;
SELECT 'Users count:' as table_name, COUNT(*) as count FROM users;
SELECT 'Items count:' as table_name, COUNT(*) as count FROM items;
SELECT 'Bids count:' as table_name, COUNT(*) as count FROM bids;
SELECT 'Credit transactions count:' as table_name, COUNT(*) as count FROM credit_transactions;
