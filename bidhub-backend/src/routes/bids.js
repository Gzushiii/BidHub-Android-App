// Fixed bidding route with proper validation order
// This addresses the "Insufficient credits" error when bid amount is too low

const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { pool } = require('../config/database');

const router = express.Router();

// Place a bid - FIXED VERSION
router.post('/place', authenticateToken, async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    await connection.beginTransaction();

    const { item_id, amount } = req.body;
    const bidder_id = req.user.id;
    const bidder_alias = req.user.alias;

    console.log('=== BID PLACEMENT DEBUG (FIXED) ===');
    console.log('Request body:', req.body);
    console.log('User info:', { id: bidder_id, alias: bidder_alias });
    console.log('Bid details:', { item_id, amount });

    // Validate input
    if (!item_id || !amount || amount <= 0) {
      console.log('Validation failed: Invalid bid data');
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

    // CRITICAL FIX: Check bid amount FIRST, before credit check
    const [currentBids] = await connection.query(
      'SELECT MAX(amount) as max_bid FROM bids WHERE item_id = ?',
      [item_id]
    );

    const currentMaxBid = currentBids[0].max_bid || item.starting_price;
    
    // Use the higher of starting price or current highest bid
    const minimumBid = Math.max(currentMaxBid, item.starting_price);
    
    if (amount <= minimumBid) {
      console.log(`Bid validation failed: ${amount} <= ${minimumBid}`);
      return res.status(400).json({ 
        error: `Bid must be higher than current highest bid (₱${minimumBid}). Your bid: ₱${amount}`,
        current_bid: minimumBid,
        your_bid: amount,
        required_bid: minimumBid + 1
      });
    }

    // Only check credits AFTER validating bid amount
    console.log('Bid amount validation passed, checking user credits...');
    
    // Get user info
    let users = [];
    try {
      [users] = await connection.query(
        'SELECT id, email, alias, credits FROM users WHERE id = ?',
        [parseInt(bidder_id)]
      );
    } catch (intError) {
      try {
        [users] = await connection.query(
          'SELECT id, email, alias, credits FROM users WHERE id = ?',
          [String(bidder_id)]
        );
      } catch (stringError) {
        [users] = await connection.query(
          'SELECT id, email, alias, credits FROM users WHERE CAST(id AS CHAR) = ?',
          [String(bidder_id)]
        );
      }
    }

    if (users.length === 0) {
      // Try to find user by email as fallback
      const [emailUsers] = await connection.query(
        'SELECT id, email, alias, credits FROM users WHERE email = ?',
        [req.user.email]
      );
      
      if (emailUsers.length > 0) {
        users = emailUsers;
      } else {
        return res.status(404).json({ 
          error: 'User not found',
          bidder_id: bidder_id,
          user_email: req.user.email
        });
      }
    }

    const userCredits = users[0].credits;
    const actualUserId = users[0].id;
    
    console.log('Credit check:', { 
      actualUserId, 
      userCredits, 
      amount, 
      sufficient: userCredits >= amount
    });
    
    if (userCredits < amount) {
      console.log('Insufficient credits error:', { 
        required: amount, 
        available: userCredits,
        user_id: actualUserId
      });
      return res.status(400).json({ 
        error: `Insufficient credits. Required: ₱${amount}, Available: ₱${userCredits}`,
        required: amount,
        available: userCredits,
        user_id: actualUserId
      });
    }

    // Call the PlaceBid stored procedure
    console.log('Calling PlaceBid stored procedure with:', { 
      item_id, 
      actual_bidder_id: actualUserId, 
      amount, 
      alias: bidder_alias 
    });
    
    try {
      await connection.query(
        'CALL PlaceBid(?, ?, ?, ?)',
        [item_id, actualUserId, amount, bidder_alias]
      );
      console.log('PlaceBid stored procedure completed successfully');
    } catch (procError) {
      console.error('PlaceBid stored procedure error:', procError);
      throw procError;
    }

    await connection.commit();
    console.log('Transaction committed successfully');
    
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

module.exports = router;
