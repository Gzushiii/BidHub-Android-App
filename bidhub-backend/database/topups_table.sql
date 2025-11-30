-- Top-ups table schema
-- This table stores manual top-up requests for credit purchases

CREATE TABLE IF NOT EXISTS topups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'PHP',
    generated_ref VARCHAR(50) UNIQUE NOT NULL,
    user_receipt_ref VARCHAR(255) DEFAULT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_number VARCHAR(50) NOT NULL,
    status ENUM('PENDING', 'UNDER_REVIEW', 'CONFIRMED', 'REJECTED') DEFAULT 'PENDING',
    instructions TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    submitted_at DATETIME DEFAULT NULL,
    confirmed_at DATETIME DEFAULT NULL,
    rejected_at DATETIME DEFAULT NULL,
    confirmed_by INT DEFAULT NULL,
    rejected_by INT DEFAULT NULL,
    rejection_reason TEXT DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_generated_ref (generated_ref),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

