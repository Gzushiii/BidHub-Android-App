-- =====================================================
-- Remove PlaceBid and BuyNow Procedures
-- =====================================================
-- This script removes the PlaceBid and BuyNow stored procedures
-- from the database
-- =====================================================

USE defaultdb;

-- Drop PlaceBid procedure
DROP PROCEDURE IF EXISTS PlaceBid;

-- Drop BuyNow procedure
DROP PROCEDURE IF EXISTS BuyNow;

-- =====================================================
-- Verification
-- =====================================================

SELECT '✅ PlaceBid and BuyNow procedures removed successfully!' AS status;

-- Verify procedures are removed
SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE,
    CREATED
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb'
  AND ROUTINE_NAME IN ('PlaceBid', 'BuyNow');

-- If the query above returns no rows, the procedures have been successfully removed

