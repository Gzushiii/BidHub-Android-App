const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { purchaseCreditsSchema, transactionFilterSchema } = require('../validators/items');
const { validateCreditPurchase } = require('../utils/validators');
const { pool } = require('../config/database');

const router = express.Router();

// Get credits balance with real-time updates
router.get('/balance', authenticateToken, async (req, res) => {
  try {
    const user_id = req.user.id;

    // Get user credits with balance version for consistency checking
    // Handle case where balance_version column might not exist
    const [users] = await pool.query(
      'SELECT credits, COALESCE(balance_version, 0) as balance_version FROM users WHERE id = ?',
      [user_id]
    );

    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const credits = Number(users[0].credits || 0);
    const balance_version = Number(users[0].balance_version || 0);

    // Get recent transactions
    const [transactions] = await pool.query(
      `SELECT 
        id, type, amount, status, description, item_id, 
        reference, transaction_date, created_at
       FROM credit_transactions 
       WHERE user_id = ? 
       ORDER BY created_at DESC 
       LIMIT 20`,
      [user_id]
    );

    // Calculate pending transactions (if any)
    const [pendingTransactions] = await pool.query(
      `SELECT SUM(amount) as pending_total
       FROM credit_transactions 
       WHERE user_id = ? AND status = 'pending'`,
      [user_id]
    );
    const pendingTotal = Number(pendingTransactions[0]?.pending_total || 0);

    res.json({
      credits,
      balance_version,
      available_balance: credits - pendingTotal,
      pending_balance: pendingTotal,
      recent_transactions: transactions,
      last_updated: new Date().toISOString()
    });
  } catch (err) {
    console.error('Credits balance error:', err);
    res.status(500).json({ error: 'Failed to fetch credits balance' });
  }
});

// Get credit transaction history
router.get('/transactions', authenticateToken, async (req, res) => {
  try {
    const { error, value } = transactionFilterSchema.validate(req.query);
    if (error) {
      return res.status(400).json({ 
        error: 'Validation failed', 
        details: error.details.map(d => d.message) 
      });
    }

    const { type, status, limit, offset } = value;
    const user_id = req.user.id;

    // Build query with filters
    let query = `
      SELECT 
        ct.*,
        i.title as item_title,
        i.id as item_id
      FROM credit_transactions ct
      LEFT JOIN items i ON ct.reference = i.id AND ct.type = 'bid'
      WHERE ct.user_id = ?
    `;
    
    const params = [user_id];

    if (type) {
      query += ' AND ct.type = ?';
      params.push(type);
    }

    if (status) {
      query += ' AND ct.status = ?';
      params.push(status);
    }

    query += ' ORDER BY ct.created_at DESC LIMIT ? OFFSET ?';
    params.push(limit, offset);

    const [transactions] = await pool.query(query, params);

    // Get total count for pagination
    let countQuery = 'SELECT COUNT(*) as total FROM credit_transactions WHERE user_id = ?';
    const countParams = [user_id];

    if (type) {
      countQuery += ' AND type = ?';
      countParams.push(type);
    }

    if (status) {
      countQuery += ' AND status = ?';
      countParams.push(status);
    }

    const [countResult] = await pool.query(countQuery, countParams);
    const total = countResult[0].total;

    res.json({
      transactions,
      count: transactions.length,
      total,
      limit,
      offset
    });

  } catch (err) {
    console.error('Transaction history error:', err);
    res.status(500).json({ error: 'Failed to fetch transaction history' });
  }
});

// Purchase credits
router.post('/purchase', authenticateToken, async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    await connection.beginTransaction();

    // Validate input
    const { error, value } = purchaseCreditsSchema.validate(req.body);
    if (error) {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ 
        error: 'Validation failed', 
        details: error.details.map(d => d.message) 
      });
    }

    const { amount, payment_method, transaction_id } = value;
    const user_id = req.user.id;

    // Additional business logic validation
    const creditValidation = validateCreditPurchase(amount);
    if (!creditValidation.isValid) {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ error: creditValidation.message });
    }

    // Check if user exists
    const [users] = await connection.query(
      'SELECT id, credits FROM users WHERE id = ?',
      [user_id]
    );

    if (users.length === 0) {
      await connection.rollback();
      connection.release();
      return res.status(404).json({ error: 'User not found' });
    }

    const currentCredits = users[0].credits;

    // Check for duplicate transaction
    const [existingTransactions] = await connection.query(
      "SELECT id FROM credit_transactions WHERE reference = ? AND type = 'purchase'",
      [transaction_id]
    );

    if (existingTransactions.length > 0) {
      await connection.rollback();
      connection.release();
      return res.status(400).json({ 
        error: 'Transaction already processed',
        transaction_id: transaction_id
      });
    }

    // Simulate payment processing based on payment method
    let paymentStatus = 'completed';
    let paymentError = null;

    if (payment_method === 'test') {
      // Test payment always succeeds
      paymentStatus = 'completed';
    } else if (payment_method === 'stripe' || payment_method === 'card') {
      // Simulate payment processing
      // In real implementation, integrate with Stripe API
      if (amount > 5000) {
        paymentStatus = 'failed';
        paymentError = 'Payment declined: Amount too high';
      } else {
        paymentStatus = 'completed';
      }
    } else if (payment_method === 'redemption_code') {
      // Check if redemption code is valid
      // In real implementation, validate against redemption codes table
      if (transaction_id.length < 8) {
        paymentStatus = 'failed';
        paymentError = 'Invalid redemption code';
      } else {
        paymentStatus = 'completed';
      }
    } else {
      paymentStatus = 'failed';
      paymentError = 'Unsupported payment method';
    }

    if (paymentStatus === 'failed') {
      await connection.rollback();
      return res.status(400).json({ 
        error: 'Payment failed',
        details: paymentError
      });
    }

    // Create credit transaction
    const [result] = await connection.query(
      `INSERT INTO credit_transactions 
       (user_id, type, amount, payment_method, transaction_id, status, reference) 
       VALUES (?, 'purchase', ?, ?, ?, 'completed', ?)`,
      [user_id, amount, payment_method, transaction_id, transaction_id]
    );

    // Update user credits
    await connection.query(
      'UPDATE users SET credits = credits + ? WHERE id = ?',
      [amount, user_id]
    );

    // Get updated credits balance
    const [updatedUsers] = await connection.query(
      'SELECT credits FROM users WHERE id = ?',
      [user_id]
    );

    await connection.commit();
    
    res.json({ 
      message: 'Credits purchased successfully',
      amount_purchased: amount,
      previous_balance: currentCredits,
      new_balance: updatedUsers[0].credits,
      transaction_id: transaction_id,
      payment_method: payment_method
    });
  } catch (err) {
    await connection.rollback();
    console.error('Credit purchase error:', err);
    res.status(500).json({ error: 'Failed to process payment' });
  } finally {
    connection.release();
  }
});

module.exports = router;
