-- Check for triggers and constraints that might be using 'active' column
USE defaultdb;

-- Check all triggers
SELECT '=== ALL TRIGGERS ===' as section;
SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE, ACTION_STATEMENT
FROM information_schema.TRIGGERS 
WHERE TRIGGER_SCHEMA = 'defaultdb';

-- Check all foreign keys
SELECT '=== ALL FOREIGN KEYS ===' as section;
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = 'defaultdb' 
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Check all check constraints (MySQL 8.0+ compatible)
SELECT '=== ALL CHECK CONSTRAINTS ===' as section;
SELECT CONSTRAINT_NAME, CONSTRAINT_SCHEMA, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS 
WHERE CONSTRAINT_SCHEMA = 'defaultdb';

-- Check for any views that might use 'active' column
SELECT '=== ALL VIEWS ===' as section;
SELECT TABLE_NAME, VIEW_DEFINITION
FROM information_schema.VIEWS 
WHERE TABLE_SCHEMA = 'defaultdb';

-- Check items table for any special columns
SELECT '=== ITEMS TABLE FULL STRUCTURE ===' as section;
SHOW CREATE TABLE items;
