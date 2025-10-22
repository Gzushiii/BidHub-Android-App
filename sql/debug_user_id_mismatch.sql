-- Debug user ID mismatch issue
USE defaultdb;

-- Step 1: Check user table structure
SELECT '=== USER TABLE STRUCTURE ===' as section;
DESCRIBE users;

-- Step 2: Check actual user data with different ID formats
SELECT '=== USER DATA CHECK ===' as section;
SELECT id, email, alias, credits, 
       CASE 
           WHEN id = '1' THEN 'String 1'
           WHEN id = 1 THEN 'Integer 1'
           ELSE 'Other'
       END as id_type
FROM users 
WHERE email = 'testuser444@example.com' 
   OR email LIKE '%test%'
ORDER BY id;

-- Step 3: Test query with string ID
SELECT '=== STRING ID QUERY TEST ===' as section;
SELECT id, email, credits FROM users WHERE id = '1';

-- Step 4: Test query with integer ID  
SELECT '=== INTEGER ID QUERY TEST ===' as section;
SELECT id, email, credits FROM users WHERE id = 1;

-- Step 5: Check if bids table exists and its structure
SELECT '=== BIDS TABLE CHECK ===' as section;
SHOW TABLES LIKE 'bids';
