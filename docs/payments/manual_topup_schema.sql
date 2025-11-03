-- =====================================================
-- MANUAL TOP-UP SYSTEM - DATABASE SCHEMA
-- =====================================================
-- This schema implements the manual QR/reference code top-up system
-- for BidHub, replacing external payment SDK integration.
-- 
-- Core Tables:
--   - topups: Manages manual top-up requests
--   - credit_ledger: Audit trail for all credit adjustments
--
-- System Flow:
--   1. User initiates top-up → topups entry created (PENDING)
--   2. User submits receipt ref → status → UNDER_REVIEW
--   3. Admin confirms → credits added → status → CONFIRMED
--   4. Admin rejects → status → REJECTED
-- =====================================================

USE defaultdb;

-- =====================================================
-- TOPUPS TABLE - Manual Top-Up Requests
-- =====================================================

CREATE TABLE IF NOT EXISTS topups (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    
    -- User reference
    user_id INT UNSIGNED NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    
    -- Amount information
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) DEFAULT 'PHP',
    
    -- Payment reference codes
    generated_ref VARCHAR(16) NOT NULL UNIQUE,
    user_receipt_ref VARCHAR(64) NULL COMMENT 'User-entered receipt reference',
    
    -- Payment information
    payment_method ENUM('gcash', 'maya', 'bank_transfer', 'other') NOT NULL,
    payment_number VARCHAR(50) COMMENT 'Official payment number (GCash/Maya)',
    
    -- Status tracking
    status ENUM('PENDING', 'UNDER_REVIEW', 'CONFIRMED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING',
    
    -- Admin tracking
    confirmed_by INT UNSIGNED NULL COMMENT 'Admin user_id who confirmed',
    rejected_by INT UNSIGNED NULL COMMENT 'Admin user_id who rejected',
    rejection_reason TEXT NULL COMMENT 'Reason for rejection',
    
    -- Metadata
    instructions TEXT COMMENT 'Payment instructions with QR code data',
    notes TEXT COMMENT 'Internal notes',
    ip_address VARCHAR(45) COMMENT 'User IP for security',
    user_agent VARCHAR(500) COMMENT 'User agent for security',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NULL COMMENT 'When user submitted receipt ref',
    confirmed_at TIMESTAMP NULL COMMENT 'When admin confirmed',
    rejected_at TIMESTAMP NULL COMMENT 'When admin rejected',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign keys
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (confirmed_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (rejected_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Indexes for performance
    INDEX idx_topups_user_id (user_id),
    INDEX idx_topups_status (status),
    INDEX idx_topups_generated_ref (generated_ref),
    INDEX idx_topups_user_receipt_ref (user_receipt_ref),
    INDEX idx_topups_created_at (created_at),
    INDEX idx_topups_payment_method (payment_method),
    
    -- Constraints
    UNIQUE KEY unique_generated_ref (generated_ref)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Manual top-up requests with QR/reference code system';

-- =====================================================
-- CREDIT_LEDGER TABLE - Audit Trail for All Credits
-- =====================================================

CREATE TABLE IF NOT EXISTS credit_ledger (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    
    -- User reference
    user_id INT UNSIGNED NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    
    -- Credit change
    delta DECIMAL(10,2) NOT NULL COMMENT 'Positive for credit addition, negative for deduction',
    balance_before DECIMAL(10,2) NOT NULL COMMENT 'Balance before this transaction',
    balance_after DECIMAL(10,2) NOT NULL COMMENT 'Balance after this transaction',
    
    -- Transaction details
    reason ENUM('TOPUP', 'BID', 'REFUND', 'REFUND_OUTBID', 'BUY_NOW', 'ADJUSTMENT', 'TRANSFER', 'BONUS') NOT NULL,
    description TEXT COMMENT 'Human-readable description',
    
    -- References to other tables
    ref_id INT UNSIGNED NULL COMMENT 'Reference to topups.id, bids.id, or items.id',
    ref_type ENUM('topup', 'bid', 'item', 'refund', 'manual') NULL,
    
    -- Related transaction
    credit_transaction_id INT UNSIGNED NULL COMMENT 'Link to credit_transactions table',
    
    -- Metadata
    metadata JSON NULL COMMENT 'Additional data (item_id, bid_id, etc.)',
    performed_by INT UNSIGNED NULL COMMENT 'User or system ID that performed this',
    system_note TEXT COMMENT 'Internal system note',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (ref_id) REFERENCES topups(id) ON DELETE SET NULL,
    FOREIGN KEY (credit_transaction_id) REFERENCES credit_transactions(id) ON DELETE SET NULL,
    FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Indexes for performance and auditing
    INDEX idx_credit_ledger_user_id (user_id),
    INDEX idx_credit_ledger_reason (reason),
    INDEX idx_credit_ledger_created_at (created_at),
    INDEX idx_credit_ledger_ref (ref_type, ref_id),
    INDEX idx_credit_ledger_credit_transaction (credit_transaction_id),
    
    -- Composite indexes for common queries
    INDEX idx_credit_ledger_user_date (user_id, created_at),
    INDEX idx_credit_ledger_user_reason (user_id, reason)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit trail for all credit adjustments - immutable ledger';

-- =====================================================
-- VIEWS FOR REPORTING
-- =====================================================

-- Pending top-ups requiring admin review
CREATE OR REPLACE VIEW v_pending_topups AS
SELECT 
    t.id,
    t.user_id,
    u.username,
    u.alias,
    u.email,
    t.amount,
    t.generated_ref,
    t.user_receipt_ref,
    t.payment_method,
    t.status,
    t.created_at,
    t.submitted_at,
    TIMESTAMPDIFF(HOUR, t.submitted_at, NOW()) as hours_waiting
FROM topups t
JOIN users u ON t.user_id = u.id
WHERE t.status IN ('PENDING', 'UNDER_REVIEW')
ORDER BY t.created_at DESC;

-- Top-up statistics per user
CREATE OR REPLACE VIEW v_user_topup_stats AS
SELECT 
    u.id as user_id,
    u.username,
    u.email,
    u.alias,
    COUNT(DISTINCT t.id) as total_topups,
    SUM(CASE WHEN t.status = 'CONFIRMED' THEN 1 ELSE 0 END) as confirmed_topups,
    SUM(CASE WHEN t.status = 'CONFIRMED' THEN t.amount ELSE 0 END) as total_confirmed_amount,
    MAX(t.created_at) as last_topup_at
FROM users u
LEFT JOIN topups t ON u.id = t.user_id
GROUP BY u.id, u.username, u.email, u.alias
ORDER BY total_confirmed_amount DESC;

-- Credit ledger summary
CREATE OR REPLACE VIEW v_credit_ledger_summary AS
SELECT 
    cl.user_id,
    u.username,
    cl.reason,
    COUNT(*) as transaction_count,
    SUM(cl.delta) as total_delta,
    MIN(cl.created_at) as first_transaction,
    MAX(cl.created_at) as last_transaction
FROM credit_ledger cl
JOIN users u ON cl.user_id = u.id
GROUP BY cl.user_id, u.username, cl.reason
ORDER BY cl.user_id, cl.reason;

-- =====================================================
-- STORED PROCEDURES
-- =====================================================

DELIMITER $$

-- Procedure to process top-up confirmation
CREATE PROCEDURE IF NOT EXISTS sp_confirm_topup(
    IN p_topup_id INT UNSIGNED,
    IN p_admin_user_id INT UNSIGNED
)
BEGIN
    DECLARE v_user_id INT UNSIGNED;
    DECLARE v_amount DECIMAL(10,2);
    DECLARE v_current_balance DECIMAL(10,2);
    DECLARE v_new_balance DECIMAL(10,2);
    DECLARE v_transaction_id INT UNSIGNED;
    DECLARE v_exists INT;
    
    START TRANSACTION;
    
    -- Check if topup exists and is in correct status
    SELECT COUNT(*) INTO v_exists
    FROM topups
    WHERE id = p_topup_id 
    AND status = 'UNDER_REVIEW';
    
    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Topup not found or not in UNDER_REVIEW status';
    END IF;
    
    -- Get topup details
    SELECT user_id, amount, user_email INTO v_user_id, v_amount, v_user_id
    FROM topups
    WHERE id = p_topup_id;
    
    -- Get current balance
    SELECT credits INTO v_current_balance
    FROM users
    WHERE id = v_user_id;
    
    -- Calculate new balance
    SET v_new_balance = v_current_balance + v_amount;
    
    -- Update user credits
    UPDATE users
    SET credits = v_new_balance
    WHERE id = v_user_id;
    
    -- Create credit transaction
    INSERT INTO credit_transactions (
        user_id, type, amount, status, payment_method, 
        reference, transaction_id, created_at
    ) VALUES (
        v_user_id, 'purchase', v_amount, 'completed', 
        (SELECT payment_method FROM topups WHERE id = p_topup_id),
        (SELECT generated_ref FROM topups WHERE id = p_topup_id),
        (SELECT user_receipt_ref FROM topups WHERE id = p_topup_id),
        NOW()
    );
    
    SET v_transaction_id = LAST_INSERT_ID();
    
    -- Create ledger entry
    INSERT INTO credit_ledger (
        user_id, user_email, delta, balance_before, balance_after,
        reason, description, ref_id, ref_type, credit_transaction_id,
        performed_by
    ) VALUES (
        v_user_id,
        (SELECT user_email FROM topups WHERE id = p_topup_id),
        v_amount,
        v_current_balance,
        v_new_balance,
        'TOPUP',
        CONCAT('Top-up confirmed: ₱', v_amount, ' via ', (SELECT payment_method FROM topups WHERE id = p_topup_id)),
        p_topup_id,
        'topup',
        v_transaction_id,
        p_admin_user_id
    );
    
    -- Update topup status
    UPDATE topups
    SET status = 'CONFIRMED',
        confirmed_by = p_admin_user_id,
        confirmed_at = NOW()
    WHERE id = p_topup_id;
    
    COMMIT;
    
    -- Return success
    SELECT 'success' as status, v_new_balance as new_balance;
END$$

-- Procedure to reject top-up
CREATE PROCEDURE IF NOT EXISTS sp_reject_topup(
    IN p_topup_id INT UNSIGNED,
    IN p_admin_user_id INT UNSIGNED,
    IN p_rejection_reason TEXT
)
BEGIN
    UPDATE topups
    SET status = 'REJECTED',
        rejected_by = p_admin_user_id,
        rejected_at = NOW(),
        rejection_reason = p_rejection_reason
    WHERE id = p_topup_id
    AND status IN ('PENDING', 'UNDER_REVIEW');
    
    SELECT 'success' as status, 'Topup rejected' as message;
END$$

DELIMITER ;

-- =====================================================
-- INDICES AND OPTIMIZATION
-- =====================================================

-- Additional composite indexes for common queries
CREATE INDEX idx_topups_user_status ON topups(user_id, status);
CREATE INDEX idx_topups_status_created ON topups(status, created_at);

CREATE INDEX idx_credit_ledger_user_created ON credit_ledger(user_id, created_at DESC);

-- =====================================================
-- SAMPLE DATA FOR TESTING
-- =====================================================

-- Uncomment to insert sample data for testing
/*
-- Sample topup in PENDING status
INSERT INTO topups (
    user_id, user_email, amount, currency, generated_ref, 
    payment_method, payment_number, status, instructions
) VALUES (
    1, 
    'test@example.com', 
    500.00, 
    'PHP', 
    'TOPUP20240001', 
    'gcash', 
    '+63 916 123 4567',
    'PENDING',
    'Please pay ₱500.00 to GCash number +63 916 123 4567 with reference code TOPUP20240001'
);

-- Sample topup in UNDER_REVIEW status
INSERT INTO topups (
    user_id, user_email, amount, currency, generated_ref, 
    payment_method, payment_number, status, user_receipt_ref, 
    submitted_at
) VALUES (
    2, 
    'user@example.com', 
    1000.00, 
    'PHP', 
    'TOPUP20240002', 
    'maya', 
    '+63 917 789 0123',
    'UNDER_REVIEW',
    'RECEIPT1234567890',
    NOW()
);
*/

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Check tables were created
SELECT '=== TOPUPS TABLE ===' as status;
DESCRIBE topups;

SELECT '=== CREDIT_LEDGER TABLE ===' as status;
DESCRIBE credit_ledger;

-- Check indexes
SELECT '=== TOPUPS INDEXES ===' as status;
SHOW INDEX FROM topups WHERE Key_name != 'PRIMARY';

SELECT '=== CREDIT_LEDGER INDEXES ===' as status;
SHOW INDEX FROM credit_ledger WHERE Key_name != 'PRIMARY';

-- Check views
SELECT '=== VIEWS CREATED ===' as status;
SHOW FULL TABLES WHERE Table_type = 'VIEW';

-- Check procedures
SELECT '=== STORED PROCEDURES ===' as status;
SHOW PROCEDURE STATUS WHERE Db = DATABASE();

-- =====================================================
-- MIGRATION NOTES
-- =====================================================

-- If upgrading from existing credit_transactions system:
-- 1. credit_transactions table already exists and is compatible
-- 2. Existing transactions will remain in credit_transactions
-- 3. New topups will use both topups and credit_ledger tables
-- 4. credit_ledger provides immutable audit trail
-- 5. Users table credits column is still the source of truth for current balance

SELECT '✓ Manual top-up system schema installed successfully' as status;

