-- Check data types and potential mismatches
USE defaultdb;

-- Step 1: Check user table structure and data types
SELECT '=== USER TABLE STRUCTURE ===' as section;
DESCRIBE users;

-- Step 2: Check actual user data with different ID formats
SELECT '=== USER ID FORMAT CHECK ===' as section;
SELECT 
    id, 
    email, 
    alias, 
    credits,
    -- Removed TYPEOF(id) as it's not available in MySQL
    CASE 
        WHEN id = '1' THEN 'String 1 matches'
        WHEN id = 1 THEN 'Integer 1 matches'
        ELSE 'No match'
    END as id_comparison
FROM users 
WHERE email LIKE '%test%' OR email LIKE '%example%'
ORDER BY id;

-- Step 3: Test queries with different ID formats
SELECT '=== ID FORMAT QUERY TESTS ===' as section;

-- Test with string ID
SELECT 'String ID Query:' as test_type;
SELECT id, email, credits FROM users WHERE id = '1';

-- Test with integer ID
SELECT 'Integer ID Query:' as test_type;
SELECT id, email, credits FROM users WHERE id = 1;

-- Test with CAST to string
SELECT 'CAST to String Query:' as test_type;
SELECT id, email, credits FROM users WHERE CAST(id AS CHAR) = '1';

-- Test with CAST to integer
SELECT 'CAST to Integer Query:' as test_type;
SELECT id, email, credits FROM users WHERE CAST(id AS UNSIGNED) = 1;

-- Step 4: Check items table structure
SELECT '=== ITEMS TABLE STRUCTURE ===' as section;
DESCRIBE items;

-- Step 5: Check bids table structure
SELECT '=== BIDS TABLE STRUCTURE ===' as section;
DESCRIBE bids;

-- Step 6: Check foreign key constraints
SELECT '=== FOREIGN KEY CONSTRAINTS ===' as section;
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE 
WHERE REFERENCED_TABLE_SCHEMA = 'defaultdb' 
AND TABLE_NAME IN ('bids', 'items', 'users');

-- Step 7: Test specific user lookup (replace with actual user email from your system)
SELECT '=== SPECIFIC USER LOOKUP ===' as section;
SELECT 
    id,
    email,
    alias,
    credits,
    'Direct ID lookup' as lookup_method
FROM users 
WHERE email = 'testuser444@example.com'
UNION ALL
SELECT 
    id,
    email,
    alias,
    credits,
    'String ID lookup' as lookup_method
FROM users 
WHERE id = (SELECT id FROM users WHERE email = 'testuser444@example.com' LIMIT 1);
