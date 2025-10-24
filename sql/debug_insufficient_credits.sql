-- Debug queries for "Insufficient credit balance" issues
-- Use these to investigate credit balance problems

USE defaultdb;

-- Check user's current credit balance and recent activity
SELECT '=== USER CREDIT BALANCE DEBUG ===' as section;
SELECT 
    u.id as user_id,
    u.email,
    u.alias,
    u.credits as current_balance,
    u.updated_at as last_balance_update
FROM users u
WHERE u.email = 'testuser444@example.com'; -- Change this to the user having issues

-- Check recent credit transactions for this user
SELECT '=== RECENT TRANSACTIONS FOR USER ===' as section;
SELECT 
    ct.id as transaction_id,
    ct.type,
    ct.amount,
    ct.status,
    ct.reference,
    ct.transaction_date,
    CASE 
        WHEN ct.type = 'purchase' THEN '+' || ct.amount
        WHEN ct.type = 'bid' THEN '-' || ct.amount
        ELSE ct.amount
    END as credit_change
FROM credit_transactions ct
JOIN users u ON ct.user_id = u.id
WHERE u.email = 'testuser444@example.com' -- Change this to the user having issues
ORDER BY ct.transaction_date DESC
LIMIT 15;

-- Check if there are any pending or failed transactions
SELECT '=== PENDING/FAILED TRANSACTIONS ===' as section;
SELECT 
    u.email,
    ct.type,
    ct.amount,
    ct.status,
    ct.reference,
    ct.transaction_date
FROM credit_transactions ct
JOIN users u ON ct.user_id = u.id
WHERE u.email = 'testuser444@example.com' -- Change this to the user having issues
AND ct.status IN ('pending', 'failed', 'cancelled')
ORDER BY ct.transaction_date DESC;

-- Calculate running balance to verify consistency
SELECT '=== CREDIT BALANCE VERIFICATION ===' as section;
WITH credit_changes AS (
    SELECT 
        ct.transaction_date,
        ct.type,
        ct.amount,
        CASE 
            WHEN ct.type = 'purchase' THEN ct.amount
            WHEN ct.type = 'bid' THEN -ct.amount
            ELSE 0
        END as balance_change
    FROM credit_transactions ct
    JOIN users u ON ct.user_id = u.id
    WHERE u.email = 'testuser444@example.com' -- Change this to the user having issues
    AND ct.status = 'completed'
    ORDER BY ct.transaction_date ASC
)
SELECT 
    transaction_date,
    type,
    amount,
    balance_change,
    SUM(balance_change) OVER (ORDER BY transaction_date) as running_balance
FROM credit_changes
ORDER BY transaction_date DESC
LIMIT 10;

-- Check for duplicate transactions that might cause issues
SELECT '=== DUPLICATE TRANSACTION CHECK ===' as section;
SELECT 
    reference,
    type,
    amount,
    status,
    COUNT(*) as duplicate_count,
    GROUP_CONCAT(transaction_date) as transaction_dates
FROM credit_transactions ct
JOIN users u ON ct.user_id = u.id
WHERE u.email = 'testuser444@example.com' -- Change this to the user having issues
GROUP BY reference, type, amount, status
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC;
