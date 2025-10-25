-- Check UUID Status and Provide Manual Update Commands
-- This script shows the current state and provides SQL commands to run manually

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'UUID STATUS CHECK AND MANUAL UPDATE COMMANDS' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- Step 1: Check current state
SELECT 'Step 1: Current UUID Status...' AS '';
SELECT 'Items with UUIDs:', (SELECT COUNT(*) FROM items WHERE uuid_id IS NOT NULL) as count;
SELECT 'Items without UUIDs:', (SELECT COUNT(*) FROM items WHERE uuid_id IS NULL) as count;
SELECT 'Total items:', (SELECT COUNT(*) FROM items) as count;

SELECT '' AS '';

-- Step 2: Show items without UUIDs
SELECT 'Step 2: Items without UUIDs...' AS '';
SELECT id, title, status FROM items WHERE uuid_id IS NULL LIMIT 10;

SELECT '' AS '';

-- Step 3: Create ID mapping for items without UUIDs
SELECT 'Step 3: Creating ID mapping...' AS '';
DROP TABLE IF EXISTS id_mapping;
CREATE TABLE id_mapping (
    old_id INT UNSIGNED,
    new_id VARCHAR(36),
    PRIMARY KEY (old_id)
);

INSERT INTO id_mapping (old_id, new_id)
SELECT id, UUID() as new_id
FROM items
WHERE uuid_id IS NULL
ORDER BY id;

SELECT 'ID Mapping created for items without UUIDs:' AS '';
SELECT old_id, new_id FROM id_mapping LIMIT 10;

SELECT '' AS '';

-- Step 4: Show manual update commands
SELECT 'Step 4: Manual Update Commands (run these one by one)...' AS '';
SELECT '' AS '';

-- Generate individual UPDATE statements
SELECT CONCAT('UPDATE items SET uuid_id = ''', new_id, ''' WHERE id = ', old_id, ';') as update_command
FROM id_mapping
ORDER BY old_id;

SELECT '' AS '';

-- Step 5: Verification commands
SELECT 'Step 5: Verification Commands (run after updates)...' AS '';
SELECT 'SELECT COUNT(*) FROM items WHERE uuid_id IS NOT NULL;' as verify_all_have_uuid;
SELECT 'SELECT id, uuid_id, title FROM items WHERE uuid_id IS NOT NULL LIMIT 5;' as verify_sample;

SELECT '' AS '';

SELECT '=======================================================' AS '';
SELECT 'INSTRUCTIONS:' AS '';
SELECT '1. Copy and run each UPDATE command above' AS '';
SELECT '2. Run the verification commands to confirm' AS '';
SELECT '3. All items should then have UUID IDs' AS '';
SELECT '=======================================================' AS '';
