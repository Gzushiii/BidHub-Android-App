// Fixed credits route with proper balance synchronization
// This addresses the credit balance sync issue between frontend and backend

const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { purchaseCreditsSchema, transactionFilterSchema } = require('../validators/items');
const { validateCreditPurchase } = require('../utils/validators');
const { pool } = require('../config/database');

const router = express.Router();

// Get credits balance - FIXED VERSION
router.get('/balance', authenticateToken, async (req, res) => {
  try {
    const user_id = req.user.id;
    
    console.log('=== CREDIT BALANCE REQUEST ===');
    console.log('User ID:', user_id);
    console.log('User Email:', req.user.email);

    // Get user credits with detailed logging
    const [users] = await pool.query(
      'SELECT id, email, alias, credits, created_at, updated_at FROM users WHERE id = ?',
      [user_id]
    );

    console.log('User query result:', users);

    if (users.length === 0) {
      console.log('User not found in database');
      return res.status(404).json({ error: 'User not found' });
    }

    const user = users[0];
    const credits = user.credits;

    console.log('User credits:', credits);
    console.log('User email:', user.email);
    console.log('User alias:', user.alias);

    // Get recent transactions for debugging
    const [transactions] = await pool.query(
      `SELECT * FROM credit_transactions 
       WHERE user_id = ? 
       ORDER BY created_at DESC 
       LIMIT 10`,
      [user_id]
    );

    console.log('Recent transactions:', transactions);

    // Return both credits and balance for compatibility
    res.json({
      credits: credits,
      balance: credits, // Some clients expect 'balance' field
      user_id: user_id,
      email: user.email,
      alias: user.alias,
      recent_transactions: transactions
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

    const { limit = 50, offset = 0, type, status } = value;
    const user_id = req.user.id;

    let query = `
      SELECT * FROM credit_transactions 
      WHERE user_id = ?
    `;
    const params = [user_id];

    if (type) {
      query += ' AND type = ?';
      params.push(type);
    }

    if (status) {
      query += ' AND status = ?';
      params.push(status);
    }

    query += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
    params.push(limit, offset);

    const [transactions] = await pool.query(query, params);

    // Get total count
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
    console.error('Credit transactions error:', err);
    res.status(500).json({ error: 'Failed to fetch credit transactions' });
  }
});

// Purchase credits - FIXED VERSION
router.post('/purchase', authenticateToken, async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    await connection.beginTransaction();

    console.log('=== CREDIT PURCHASE REQUEST ===');
    console.log('Request body:', req.body);
    console.log('User ID:', req.user.id);

    // Validate input
    const { error, value } = purchaseCreditsSchema.validate(req.body);
    if (error) {
      return res.status(400).json({ 
        error: 'Validation failed', 
        details: error.details.map(d => d.message) 
      });
    }

    const { amount, payment_method, transaction_id } = value;
    const user_id = req.user.id;

    console.log('Purchase details:', { amount, payment_method, transaction_id });

    // Additional business logic validation
    const creditValidation = validateCreditPurchase(amount);
    if (!creditValidation.isValid) {
      return res.status(400).json({ error: creditValidation.message });
    }

    // Check if user exists and get current credits
    const [users] = await connection.query(
      'SELECT id, email, credits FROM users WHERE id = ?',
      [user_id]
    );

    if (users.length === 0) {
      console.log('User not found for credit purchase');
      return res.status(404).json({ error: 'User not found' });
    }

    const currentCredits = users[0].credits;
    console.log('Current user credits:', currentCredits);

    // Check for duplicate transaction
    const [existingTransactions] = await connection.query(
      "SELECT id FROM credit_transactions WHERE reference = ? AND type = 'purchase'",
      [transaction_id]
    );

    if (existingTransactions.length > 0) {
      console.log('Duplicate transaction detected');
      return res.status(400).json({ 
        error: 'Transaction already processed',
        transaction_id: transaction_id
      });
    }

    // Process payment
    let paymentStatus = 'completed';
    let paymentError = null;

    if (payment_method === 'test') {
      paymentStatus = 'completed';
    } else if (payment_method === 'stripe' || payment_method === 'card') {
      if (amount > 5000) {
        paymentStatus = 'failed';
        paymentError = 'Payment declined: Amount too high';
      } else {
        paymentStatus = 'completed';
      }
    } else if (payment_method === 'redemption_code') {
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
      console.log('Payment failed:', paymentError);
      await connection.rollback();
      return res.status(400).json({ 
        error: 'Payment failed',
        details: paymentError
      });
    }

    // Create credit transaction
    const [result] = await connection.query(
      `INSERT INTO credit_transactions 
       (user_id, type, amount, payment_method, transaction_id, status, reference, transaction_date) 
       VALUES (?, 'purchase', ?, ?, ?, 'completed', ?, NOW())`,
      [user_id, amount, payment_method, transaction_id, transaction_id]
    );

    console.log('Credit transaction created:', result.insertId);

    // Update user credits
    await connection.query(
      'UPDATE users SET credits = credits + ? WHERE id = ?',
      [amount, user_id]
    );

    console.log('User credits updated');

    // Get updated credits balance
    const [updatedUsers] = await connection.query(
      'SELECT credits FROM users WHERE id = ?',
      [user_id]
    );

    const newBalance = updatedUsers[0].credits;
    console.log('New balance:', newBalance);

    await connection.commit();
    
    res.json({
      message: 'Credits purchased successfully',
      amount_purchased: amount,
      previous_balance: currentCredits,
      new_balance: newBalance,
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
