const express = require('express');
const { authenticateToken } = require('../../src/middleware/auth');
const { pool } = require('../../src/config/database');

const app = express();
app.use(authenticateToken);

app.get('/api/credits/balance', async (req, res) => {
  try {
    const user_id = req.user.id;

    // Get user credits
    const [users] = await pool.query(
      'SELECT credits FROM users WHERE id = ?',
      [user_id]
    );

    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const credits = users[0].credits;

    // Get recent transactions
    const [transactions] = await db.query(
      `SELECT * FROM credit_transactions 
       WHERE user_id = ? 
       ORDER BY created_at DESC 
       LIMIT 10`,
      [user_id]
    );

    res.json({
      credits,
      recent_transactions: transactions
    });
  } catch (err) {
    console.error('Credits balance error:', err);
    res.status(500).json({ error: 'Failed to fetch credits balance' });
  }
});

module.exports = app;
