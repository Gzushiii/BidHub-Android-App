const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { pool } = require('../config/database');
const crypto = require('crypto');

const router = express.Router();

// =====================================================
// HELPER FUNCTIONS
// =====================================================

/**
 * Generate unique top-up reference code
 * Format: TOPUP + YYYYMMDD + 4-digit sequence
 */
function generateTopupRef() {
  const date = new Date().toISOString().split('T')[0].replace(/-/g, '');
  const sequence = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
  return `TOPUP${date}${sequence}`;
}

/**
 * Get payment number for payment method
 */
function getPaymentNumber(paymentMethod) {
  const paymentNumbers = {
    gcash: process.env.PAYMENT_GCASH_NUMBER || '+63 916 123 4567',
    maya: process.env.PAYMENT_MAYA_NUMBER || '+63 917 789 0123',
    bank_transfer: process.env.PAYMENT_BANK_ACCOUNT || '1234567890'
  };
  return paymentNumbers[paymentMethod] || paymentNumbers.gcash;
}

// =====================================================
// USER ENDPOINTS
// =====================================================

/**
 * POST /api/topups
 * Initiate a new manual top-up request
 */
router.post('/', authenticateToken, async (req, res) => {
  let connection;
  
  try {
    connection = await pool.getConnection();
    
    const { amount, payment_method } = req.body;
    const user_id = req.user.id;
    const user_email = req.user.email;

    // Validate input
    if (!amount || amount <= 0 || !Number.isFinite(amount)) {
      connection.release();
      return res.status(400).json({ 
        error: 'Invalid amount',
        details: 'Amount must be a valid number greater than 0'
      });
    }

    if (!payment_method || typeof payment_method !== 'string') {
      connection.release();
      return res.status(400).json({ 
        error: 'Missing payment method',
        details: 'Payment method is required (gcash, maya, bank_transfer)'
      });
    }

    // Validate payment method
    const validMethods = ['gcash', 'maya', 'bank_transfer', 'other'];
    if (!validMethods.includes(payment_method.toLowerCase())) {
      connection.release();
      return res.status(400).json({ 
        error: 'Invalid payment method',
        details: `Payment method must be one of: ${validMethods.join(', ')}`
      });
    }

    // Check amount limits
    const minAmount = 100.00;
    const maxAmount = 50000.00;

    if (amount < minAmount) {
      connection.release();
      return res.status(400).json({ 
        error: 'Amount too low',
        details: `Minimum top-up amount is ₱${minAmount}`
      });
    }

    if (amount > maxAmount) {
      connection.release();
      return res.status(400).json({ 
        error: 'Amount too high',
        details: `Maximum top-up amount is ₱${maxAmount}`
      });
    }

    // Generate unique reference code
    let generated_ref = generateTopupRef();
    let retries = 0;
    const maxRetries = 10;

    // Ensure uniqueness with retry logic
    while (retries < maxRetries) {
      const [existing] = await connection.query(
        'SELECT id FROM topups WHERE generated_ref = ?',
        [generated_ref]
      );

      if (existing.length === 0) {
        break; // Unique code found
      }

      generated_ref = generateTopupRef();
      retries++;
    }

    if (retries >= maxRetries) {
      console.error('Failed to generate unique reference code after retries');
      if (connection) connection.release();
      return res.status(500).json({ 
        error: 'Failed to generate reference code',
        details: 'Please try again'
      });
    }

    // Get payment number
    const payment_number = getPaymentNumber(payment_method);

    // Create payment instructions
    const instructions = `Please pay ₱${amount.toFixed(2)} to ${payment_method.toUpperCase()} number ${payment_number} with reference code ${generated_ref}`;

    // Insert topup record
    const [result] = await connection.query(
      `INSERT INTO topups 
       (user_id, user_email, amount, currency, generated_ref, payment_method, 
        payment_number, status, instructions, ip_address, user_agent) 
       VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?, ?)`,
      [
        user_id,
        user_email,
        amount,
        'PHP',
        generated_ref,
        payment_method,
        payment_number,
        instructions,
        req.ip || req.socket.remoteAddress,
        req.get('user-agent')
      ]
    );

    const topup_id = result.insertId;

    // Build response
    const response = {
      success: true,
      topup_id: topup_id,
      generated_ref: generated_ref,
      instructions: instructions,
      payment_number: payment_number,
      amount: amount,
      payment_method: payment_method,
      status: 'PENDING'
    };

    // TODO: Generate QR code in production
    // For now, return placeholder
    response.qr_code_data = `data:image/svg+xml;base64,${Buffer.from(
      `<svg xmlns="http://www.w3.org/2000/svg" width="250" height="250">
        <rect width="250" height="250" fill="white"/>
        <text x="125" y="125" font-family="Arial" font-size="14" text-anchor="middle">
          QR Code: ${generated_ref}
        </text>
      </svg>`
    ).toString('base64')}`;

    console.log(`Top-up initiated: ${generated_ref} by user ${user_id} for ₱${amount}`);

    res.status(201).json(response);

  } catch (err) {
    console.error('Top-up initiation error:', err);
    console.error('Error stack:', err.stack);
    
    // Always include error details for better debugging
    const errorDetails = {
      error: 'Failed to initiate top-up',
      details: err.message || 'An unexpected error occurred'
    };
    
    // Include stack trace in development
    if (process.env.NODE_ENV === 'development') {
      errorDetails.stack = err.stack;
    }
    
    // Check for specific error types
    if (err.code === 'ER_NO_SUCH_TABLE') {
      errorDetails.details = 'Database table not found. Please contact support.';
      errorDetails.error = 'Database configuration error';
    } else if (err.code === 'ECONNREFUSED') {
      errorDetails.details = 'Cannot connect to database. Please try again later.';
      errorDetails.error = 'Database connection error';
    } else if (err.code === 'ER_BAD_FIELD_ERROR') {
      errorDetails.details = 'Database schema mismatch. Please contact support.';
      errorDetails.error = 'Database schema error';
    }
    
    res.status(500).json(errorDetails);
  } finally {
    if (connection) {
      connection.release();
    }
  }
});

/**
 * POST /api/topups/:id/submit
 * User submits receipt reference number
 */
router.post('/:id/submit', authenticateToken, async (req, res) => {
  let connection;
  
  try {
    connection = await pool.getConnection();
    
    const topup_id = parseInt(req.params.id);
    const { user_receipt_ref } = req.body;
    const user_id = req.user.id;

    // Validate input
    if (!user_receipt_ref || user_receipt_ref.trim().length < 4) {
      if (connection) connection.release();
      return res.status(400).json({ 
        error: 'Invalid receipt reference',
        details: 'Receipt reference must be at least 4 characters'
      });
    }

    // Find topup and verify ownership
    const [topups] = await connection.query(
      'SELECT * FROM topups WHERE id = ? AND user_id = ?',
      [topup_id, user_id]
    );

    if (topups.length === 0) {
      if (connection) connection.release();
      return res.status(404).json({ 
        error: 'Top-up not found',
        details: 'Invalid top-up ID or you do not own this top-up'
      });
    }

    const topup = topups[0];

    // Check status
    if (topup.status !== 'PENDING') {
      if (connection) connection.release();
      return res.status(400).json({ 
        error: 'Invalid status transition',
        details: `Cannot submit receipt for top-up in ${topup.status} status`
      });
    }

    // Update topup
    await connection.query(
      `UPDATE topups 
       SET user_receipt_ref = ?, status = 'UNDER_REVIEW', submitted_at = NOW() 
       WHERE id = ?`,
      [user_receipt_ref.trim(), topup_id]
    );

    console.log(`Top-up ${topup_id} submitted for review with receipt: ${user_receipt_ref}`);

    res.json({
      success: true,
      status: 'UNDER_REVIEW',
      message: 'Top-up submitted for review'
    });

  } catch (err) {
    console.error('Top-up submission error:', err);
    console.error('Error stack:', err.stack);
    
    const errorDetails = {
      error: 'Failed to submit receipt',
      details: err.message || 'An unexpected error occurred'
    };
    
    if (process.env.NODE_ENV === 'development') {
      errorDetails.stack = err.stack;
    }
    
    res.status(500).json(errorDetails);
  } finally {
    if (connection) {
      connection.release();
    }
  }
});

/**
 * GET /api/topups/:id
 * Get top-up details and status
 */
router.get('/:id', authenticateToken, async (req, res) => {
  try {
    const topup_id = parseInt(req.params.id);
    const user_id = req.user.id;

    // Find topup and verify ownership
    const [topups] = await pool.query(
      'SELECT * FROM topups WHERE id = ? AND user_id = ?',
      [topup_id, user_id]
    );

    if (topups.length === 0) {
      return res.status(404).json({ 
        error: 'Top-up not found',
        details: 'Invalid top-up ID or you do not own this top-up'
      });
    }

    const topup = topups[0];

    // Format response (exclude sensitive data)
    const response = {
      id: topup.id,
      amount: parseFloat(topup.amount),
      currency: topup.currency,
      generated_ref: topup.generated_ref,
      user_receipt_ref: topup.user_receipt_ref,
      payment_method: topup.payment_method,
      payment_number: topup.payment_number,
      status: topup.status,
      created_at: topup.created_at,
      submitted_at: topup.submitted_at,
      confirmed_at: topup.confirmed_at,
      rejected_at: topup.rejected_at,
      rejection_reason: topup.rejection_reason
    };

    res.json(response);

  } catch (err) {
    console.error('Top-up fetch error:', err);
    res.status(500).json({ 
      error: 'Failed to fetch top-up',
      details: process.env.NODE_ENV === 'development' ? err.message : undefined
    });
  }
});

/**
 * GET /api/topups
 * Get user's top-up history
 */
router.get('/', authenticateToken, async (req, res) => {
  try {
    const user_id = req.user.id;
    const { status, limit = 20, offset = 0 } = req.query;

    // Build query
    let query = `
      SELECT 
        id, amount, currency, generated_ref, payment_method, 
        status, created_at, submitted_at, confirmed_at, rejected_at
      FROM topups 
      WHERE user_id = ?
    `;
    const params = [user_id];

    // Add status filter
    if (status) {
      query += ' AND status = ?';
      params.push(status.toUpperCase());
    }

    // Add pagination
    query += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
    params.push(parseInt(limit), parseInt(offset));

    const [topups] = await pool.query(query, params);

    // Get total count
    let countQuery = 'SELECT COUNT(*) as total FROM topups WHERE user_id = ?';
    const countParams = [user_id];

    if (status) {
      countQuery += ' AND status = ?';
      countParams.push(status.toUpperCase());
    }

    const [countResult] = await pool.query(countQuery, countParams);
    const total = countResult[0].total;

    res.json({
      topups: topups,
      total: total,
      limit: parseInt(limit),
      offset: parseInt(offset)
    });

  } catch (err) {
    console.error('Top-up history error:', err);
    res.status(500).json({ 
      error: 'Failed to fetch top-up history',
      details: process.env.NODE_ENV === 'development' ? err.message : undefined
    });
  }
});

// =====================================================
// ADMIN ENDPOINTS
// =====================================================

/**
 * Middleware to check admin role
 * TODO: Implement proper admin role checking
 */
function requireAdmin(req, res, next) {
  // For now, allow all authenticated users to be admins
  // In production, check req.user.is_admin flag
  next();
}

/**
 * POST /api/topups/admin/:id/confirm
 * Admin confirms top-up and adds credits
 */
router.post('/admin/:id/confirm', authenticateToken, requireAdmin, async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    await connection.beginTransaction();

    const topup_id = parseInt(req.params.id);
    const admin_user_id = req.user.id;

    // Find topup
    const [topups] = await connection.query(
      'SELECT * FROM topups WHERE id = ?',
      [topup_id]
    );

    if (topups.length === 0) {
      await connection.rollback();
      return res.status(404).json({ 
        error: 'Top-up not found',
        details: 'Invalid top-up ID'
      });
    }

    const topup = topups[0];

    // Check status
    if (topup.status !== 'UNDER_REVIEW') {
      await connection.rollback();
      return res.status(400).json({ 
        error: 'Invalid status',
        details: `Cannot confirm top-up in ${topup.status} status`
      });
    }

    const user_id = topup.user_id;
    const amount = parseFloat(topup.amount);

    // Get current balance
    const [users] = await connection.query(
      'SELECT id, credits FROM users WHERE id = ?',
      [user_id]
    );

    if (users.length === 0) {
      await connection.rollback();
      return res.status(404).json({ 
        error: 'User not found',
        details: 'User associated with top-up not found'
      });
    }

    const currentBalance = parseFloat(users[0].credits);
    const newBalance = currentBalance + amount;

    // Update user credits
    await connection.query(
      'UPDATE users SET credits = ? WHERE id = ?',
      [newBalance, user_id]
    );

    // Create credit transaction
    const [ctResult] = await connection.query(
      `INSERT INTO credit_transactions 
       (user_id, type, amount, status, payment_method, reference, transaction_id, created_at) 
       VALUES (?, 'purchase', ?, 'completed', ?, ?, ?, NOW())`,
      [
        user_id,
        amount,
        topup.payment_method,
        topup.generated_ref,
        topup.user_receipt_ref
      ]
    );

    const credit_transaction_id = ctResult.insertId;

    // Create ledger entry
    await connection.query(
      `INSERT INTO credit_ledger 
       (user_id, user_email, delta, balance_before, balance_after, 
        reason, description, ref_id, ref_type, credit_transaction_id, performed_by) 
       VALUES (?, ?, ?, ?, ?, 'TOPUP', ?, ?, 'topup', ?, ?)`,
      [
        user_id,
        topup.user_email,
        amount,
        currentBalance,
        newBalance,
        `Top-up confirmed: ₱${amount.toFixed(2)} via ${topup.payment_method}`,
        topup_id,
        credit_transaction_id,
        admin_user_id
      ]
    );

    // Update topup status
    await connection.query(
      `UPDATE topups 
       SET status = 'CONFIRMED', confirmed_by = ?, confirmed_at = NOW() 
       WHERE id = ?`,
      [admin_user_id, topup_id]
    );

    await connection.commit();

    console.log(`Top-up ${topup_id} confirmed by admin ${admin_user_id}. User ${user_id} balance: ₱${currentBalance} -> ₱${newBalance}`);

    res.json({
      success: true,
      new_balance: newBalance,
      message: 'Top-up confirmed and credits added'
    });

  } catch (err) {
    await connection.rollback();
    console.error('Top-up confirmation error:', err);
    res.status(500).json({ 
      error: 'Failed to confirm top-up',
      details: process.env.NODE_ENV === 'development' ? err.message : undefined
    });
  } finally {
    connection.release();
  }
});

/**
 * POST /api/topups/admin/:id/reject
 * Admin rejects top-up
 */
router.post('/admin/:id/reject', authenticateToken, requireAdmin, async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    const topup_id = parseInt(req.params.id);
    const admin_user_id = req.user.id;
    const { reason } = req.body;

    // Validate input
    if (!reason || reason.trim().length < 10) {
      return res.status(400).json({ 
        error: 'Invalid rejection reason',
        details: 'Reason must be at least 10 characters'
      });
    }

    // Update topup
    const [result] = await pool.query(
      `UPDATE topups 
       SET status = 'REJECTED', rejected_by = ?, rejected_at = NOW(), 
           rejection_reason = ? 
       WHERE id = ? AND status IN ('PENDING', 'UNDER_REVIEW')`,
      [admin_user_id, reason.trim(), topup_id]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ 
        error: 'Top-up not found or not in valid status',
        details: 'Cannot reject this top-up'
      });
    }

    console.log(`Top-up ${topup_id} rejected by admin ${admin_user_id}. Reason: ${reason}`);

    res.json({
      success: true,
      message: 'Top-up rejected'
    });

  } catch (err) {
    console.error('Top-up rejection error:', err);
    res.status(500).json({ 
      error: 'Failed to reject top-up',
      details: process.env.NODE_ENV === 'development' ? err.message : undefined
    });
  } finally {
    connection.release();
  }
});

/**
 * GET /api/topups/admin/pending
 * Admin gets pending top-ups
 */
router.get('/admin/pending', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const { limit = 50, offset = 0 } = req.query;

    const [topups] = await pool.query(
      `SELECT 
        t.id, t.user_id, u.username, u.alias, u.email,
        t.amount, t.currency, t.generated_ref, t.user_receipt_ref,
        t.payment_method, t.status, t.created_at, t.submitted_at,
        TIMESTAMPDIFF(HOUR, t.submitted_at, NOW()) as hours_waiting
       FROM topups t
       JOIN users u ON t.user_id = u.id
       WHERE t.status IN ('PENDING', 'UNDER_REVIEW')
       ORDER BY t.created_at DESC
       LIMIT ? OFFSET ?`,
      [parseInt(limit), parseInt(offset)]
    );

    const [countResult] = await pool.query(
      'SELECT COUNT(*) as total FROM topups WHERE status IN ("PENDING", "UNDER_REVIEW")'
    );

    res.json({
      topups: topups,
      total: countResult[0].total,
      limit: parseInt(limit),
      offset: parseInt(offset)
    });

  } catch (err) {
    console.error('Pending top-ups fetch error:', err);
    res.status(500).json({ 
      error: 'Failed to fetch pending top-ups',
      details: process.env.NODE_ENV === 'development' ? err.message : undefined
    });
  }
});

module.exports = router;

