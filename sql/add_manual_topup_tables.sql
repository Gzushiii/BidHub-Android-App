-- =====================================================
-- MANUAL TOP-UP SUPPORT SCHEMA
-- =====================================================
-- This script installs the database objects required by
-- the manual QR/reference-based top-up system.
-- It is safe to run multiple times (idempotent).
-- =====================================================

USE defaultdb;

SELECT '=======================================================' AS '';
SELECT 'INSTALLING MANUAL TOP-UP SUPPORT OBJECTS' AS '';
SELECT '=======================================================' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 1: CREATE TOPUPS TABLE
-- =====================================================

SELECT 'STEP 1: Ensuring topups table exists...' AS '';

CREATE TABLE IF NOT EXISTS topups (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    -- User association
    user_id INT UNSIGNED NOT NULL,
    user_email VARCHAR(255) NOT NULL,

    -- Amount information
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) DEFAULT 'PHP',

    -- Reference codes
    generated_ref VARCHAR(50) NOT NULL UNIQUE,
    user_receipt_ref VARCHAR(64) NULL,

    -- Payment information
    payment_method ENUM('gcash', 'maya', 'bank_transfer', 'other') NOT NULL,
    payment_number VARCHAR(50) NULL,

    -- Status workflow
    status ENUM('PENDING', 'UNDER_REVIEW', 'CONFIRMED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING',

    -- Admin tracking
    confirmed_by INT UNSIGNED NULL,
    rejected_by INT UNSIGNED NULL,
    rejection_reason TEXT NULL,

    -- Metadata
    instructions TEXT NULL,
    notes TEXT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NULL,
    confirmed_at TIMESTAMP NULL,
    rejected_at TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_topups_user_id       FOREIGN KEY (user_id)      REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_topups_confirmed_by  FOREIGN KEY (confirmed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_topups_rejected_by   FOREIGN KEY (rejected_by)  REFERENCES users(id) ON DELETE SET NULL,

    -- Indexes
    INDEX idx_topups_user_id (user_id),
    INDEX idx_topups_status (status),
    INDEX idx_topups_generated_ref (generated_ref),
    INDEX idx_topups_user_receipt_ref (user_receipt_ref),
    INDEX idx_topups_created_at (created_at),
    INDEX idx_topups_payment_method (payment_method),
    INDEX idx_topups_user_status (user_id, status),
    INDEX idx_topups_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ topups table ready' AS '';
SELECT '' AS '';

-- =====================================================
-- NOTE: If you have an existing topups table with generated_ref VARCHAR(16),
-- you need to run the migration script: sql/migrate_fix_generated_ref_size.sql
-- to update the column size to VARCHAR(50).
-- 
-- The generated reference format is: TOPUP + YYYYMMDD + 4-digit sequence (17 chars)
-- which requires at least VARCHAR(20), but we use VARCHAR(50) for future flexibility.
-- =====================================================

-- =====================================================
-- STEP 2: CREATE CREDIT_LEDGER TABLE
-- =====================================================

SELECT 'STEP 2: Ensuring credit_ledger table exists...' AS '';

CREATE TABLE IF NOT EXISTS credit_ledger (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    -- User association
    user_id INT UNSIGNED NOT NULL,
    user_email VARCHAR(255) NOT NULL,

    -- Credit delta
    delta DECIMAL(10,2) NOT NULL,
    balance_before DECIMAL(10,2) NOT NULL,
    balance_after DECIMAL(10,2) NOT NULL,

    -- Context
    reason ENUM('TOPUP', 'BID', 'REFUND', 'REFUND_OUTBID', 'BUY_NOW', 'ADJUSTMENT', 'TRANSFER', 'BONUS') NOT NULL,
    description TEXT NULL,

    -- References
    ref_id INT UNSIGNED NULL,
    ref_type ENUM('topup', 'bid', 'item', 'refund', 'manual') NULL,
    credit_transaction_id INT UNSIGNED NULL,

    -- Metadata
    metadata JSON NULL,
    performed_by INT UNSIGNED NULL,
    system_note TEXT NULL,

    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_credit_ledger_user_id   FOREIGN KEY (user_id)              REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_credit_ledger_ref_id    FOREIGN KEY (ref_id)               REFERENCES topups(id) ON DELETE SET NULL,
    CONSTRAINT fk_credit_ledger_txn_id    FOREIGN KEY (credit_transaction_id) REFERENCES credit_transactions(id) ON DELETE SET NULL,
    CONSTRAINT fk_credit_ledger_actor_id  FOREIGN KEY (performed_by)         REFERENCES users(id) ON DELETE SET NULL,

    -- Indexes
    INDEX idx_credit_ledger_user_id (user_id),
    INDEX idx_credit_ledger_reason (reason),
    INDEX idx_credit_ledger_created_at (created_at),
    INDEX idx_credit_ledger_ref (ref_type, ref_id),
    INDEX idx_credit_ledger_credit_transaction (credit_transaction_id),
    INDEX idx_credit_ledger_user_date (user_id, created_at),
    INDEX idx_credit_ledger_user_reason (user_id, reason)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ credit_ledger table ready' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 3: SUPPORTING VIEWS
-- =====================================================

SELECT 'STEP 3: Creating reporting views...' AS '';

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
    TIMESTAMPDIFF(HOUR, t.submitted_at, NOW()) AS hours_waiting
FROM topups t
JOIN users u ON t.user_id = u.id
WHERE t.status IN ('PENDING', 'UNDER_REVIEW')
ORDER BY t.created_at DESC;

CREATE OR REPLACE VIEW v_user_topup_stats AS
SELECT 
    u.id              AS user_id,
    u.username,
    u.email,
    u.alias,
    COUNT(DISTINCT t.id) AS total_topups,
    SUM(CASE WHEN t.status = 'CONFIRMED' THEN 1 ELSE 0 END) AS confirmed_topups,
    SUM(CASE WHEN t.status = 'CONFIRMED' THEN t.amount ELSE 0 END) AS total_confirmed_amount,
    MAX(t.created_at) AS last_topup_at
FROM users u
LEFT JOIN topups t ON u.id = t.user_id
GROUP BY u.id, u.username, u.email, u.alias
ORDER BY total_confirmed_amount DESC;

CREATE OR REPLACE VIEW v_credit_ledger_summary AS
SELECT 
    cl.user_id,
    u.username,
    cl.reason,
    COUNT(*) AS transaction_count,
    SUM(cl.delta) AS total_delta,
    MIN(cl.created_at) AS first_transaction,
    MAX(cl.created_at) AS last_transaction
FROM credit_ledger cl
JOIN users u ON cl.user_id = u.id
GROUP BY cl.user_id, u.username, cl.reason
ORDER BY cl.user_id, cl.reason;

SELECT '✓ views created/updated' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 4: STORED PROCEDURES
-- =====================================================

SELECT 'STEP 4: Creating stored procedures...' AS '';

DELIMITER $$

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

    SELECT COUNT(*) INTO v_exists
    FROM topups
    WHERE id = p_topup_id 
      AND status = 'UNDER_REVIEW'
    FOR UPDATE;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Topup not found or not in UNDER_REVIEW status';
    END IF;

    SELECT user_id, amount, user_email INTO v_user_id, v_amount, v_user_id
    FROM topups
    WHERE id = p_topup_id
    FOR UPDATE;

    SELECT credits INTO v_current_balance
    FROM users
    WHERE id = v_user_id
    FOR UPDATE;

    SET v_new_balance = v_current_balance + v_amount;

    UPDATE users
    SET credits = v_new_balance
    WHERE id = v_user_id;

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

    UPDATE topups
    SET status = 'CONFIRMED',
        confirmed_by = p_admin_user_id,
        confirmed_at = NOW()
    WHERE id = p_topup_id;

    COMMIT;

    SELECT 'success' AS status, v_new_balance AS new_balance;
END$$

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

    SELECT 'success' AS status, 'Topup rejected' AS message;
END$$

DELIMITER ;

SELECT '✓ stored procedures ready' AS '';
SELECT '' AS '';

-- =====================================================
-- STEP 5: SUMMARY
-- =====================================================

SELECT 'Manual top-up schema installation complete.' AS '';
SELECT 'You can now use the /api/topups endpoints safely.' AS '';

