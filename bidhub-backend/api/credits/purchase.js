const express = require('express');
const { authenticateToken } = require('../../src/middleware/auth');
const db = require('../../src/config/database');

const app = express();
app.use(express.json());
app.use(authenticateToken);

app.post('/api/credits/purchase', async (req, res) => {
  const connection = await db.getConnection();
  
  try {
    await connection.beginTransaction();

    const { amount, payment_method, transaction_id } = req.body;
    const user_id = req.user.id;

    // Validate input
    if (!amount || amount <= 0) {
      return res.status(400).json({ error: 'Invalid amount' });
    }

    if (!payment_method || !transaction_id) {
      return res.status(400).json({ error: 'Payment method and transaction ID required' });
    }

    // Check if user exists
    const [users] = await connection.query(
      'SELECT id FROM users WHERE id = ?',
      [user_id]
    );

    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    // Create credit transaction
    const [result] = await connection.query(
      `INSERT INTO credit_transactions 
       (user_id, type, amount, payment_method, transaction_id, status) 
       VALUES (?, 'purchase', ?, ?, ?, 'completed')`,
      [user_id, amount, payment_method, transaction_id]
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
      new_balance: updatedUsers[0].credits,
      transaction_id: result.insertId
    });
  } catch (err) {
    await connection.rollback();
    console.error('Credit purchase error:', err);
    res.status(500).json({ error: 'Failed to process payment' });
  } finally {
    connection.release();
  }
});

module.exports = app;
