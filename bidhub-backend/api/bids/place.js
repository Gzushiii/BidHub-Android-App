const express = require('express');
const { authenticateToken } = require('../../src/middleware/auth');
const db = require('../../src/config/database');

const app = express();
app.use(express.json());
app.use(authenticateToken);

app.post('/api/bids/place', async (req, res) => {
  const connection = await db.getConnection();
  
  try {
    await connection.beginTransaction();

    const { item_id, amount } = req.body;
    const bidder_id = req.user.id;

    // Validate input
    if (!item_id || !amount || amount <= 0) {
      return res.status(400).json({ error: 'Invalid bid data' });
    }

    // Check if item exists and is active
    const [items] = await connection.query(
      'SELECT * FROM items WHERE id = ? AND status = "active"',
      [item_id]
    );

    if (items.length === 0) {
      return res.status(404).json({ error: 'Item not found or not active' });
    }

    const item = items[0];

    // Check if auction has ended
    if (new Date() > new Date(item.end_date)) {
      return res.status(400).json({ error: 'Auction has ended' });
    }

    // Check if bidder is not the seller
    if (bidder_id === item.seller_id) {
      return res.status(400).json({ error: 'Cannot bid on your own item' });
    }

    // Check if bid amount is higher than current highest bid
    const [currentBids] = await connection.query(
      'SELECT MAX(amount) as max_bid FROM bids WHERE item_id = ?',
      [item_id]
    );

    const currentMaxBid = currentBids[0].max_bid || item.starting_price;
    
    if (amount <= currentMaxBid) {
      return res.status(400).json({ 
        error: `Bid must be higher than current highest bid (${currentMaxBid})` 
      });
    }

    // Check if user has enough credits
    const [users] = await connection.query(
      'SELECT credits FROM users WHERE id = ?',
      [bidder_id]
    );

    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const userCredits = users[0].credits;
    if (userCredits < amount) {
      return res.status(400).json({ 
        error: 'Insufficient credits',
        required: amount,
        available: userCredits
      });
    }

    // Call the PlaceBid stored procedure
    await connection.query(
      'CALL PlaceBid(?, ?, ?, ?)',
      [item_id, bidder_id, amount, req.user.alias]
    );

    await connection.commit();
    
    res.json({ 
      message: 'Bid placed successfully',
      bid_amount: amount,
      item_id: item_id
    });
  } catch (err) {
    await connection.rollback();
    console.error('Bid placement error:', err);
    
    if (err.sqlMessage) {
      res.status(400).json({ error: err.sqlMessage });
    } else {
      res.status(500).json({ error: 'Failed to place bid' });
    }
  } finally {
    connection.release();
  }
});

module.exports = app;
