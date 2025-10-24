// Debug version of bidding route with comprehensive logging
// This helps identify user account and credit mismatching issues

const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { pool } = require('../config/database');

const router = express.Router();

// Place a bid - DEBUG VERSION
router.post('/place', authenticateToken, async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    await connection.beginTransaction();

    const { item_id, amount } = req.body;
    const bidder_id = req.user.id;
    const bidder_alias = req.user.alias;
    const bidder_email = req.user.email;

    console.log('=== COMPREHENSIVE BID DEBUG ===');
    console.log('Request body:', req.body);
    console.log('JWT User info:', { 
      id: bidder_id, 
      alias: bidder_alias, 
      email: bidder_email 
    });
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
      console.log('Item not found or not active');
      return res.status(404).json({ error: 'Item not found or not active' });
    }

    const item = items[0];
    console.log('Item details:', {
      id: item.id,
      title: item.title,
      starting_price: item.starting_price,
      current_price: item.current_price,
      seller_id: item.seller_id,
      status: item.status
    });

    // Check if auction has ended
    if (new Date() > new Date(item.end_date)) {
      console.log('Auction has ended');
      return res.status(400).json({ error: 'Auction has ended' });
    }

    // Check if bidder is not the seller
    if (bidder_id === item.seller_id) {
      console.log('Cannot bid on your own item');
      return res.status(400).json({ error: 'Cannot bid on your own item' });
    }

    // Check current highest bid
    const [currentBids] = await connection.query(
      'SELECT MAX(amount) as max_bid FROM bids WHERE item_id = ?',
      [item_id]
    );

    const currentMaxBid = currentBids[0].max_bid || item.starting_price;
    const minimumBid = Math.max(currentMaxBid, item.starting_price);
    
    console.log('Bid validation:', {
      currentMaxBid,
      startingPrice: item.starting_price,
      minimumBid,
      userBid: amount,
      valid: amount > minimumBid
    });
    
    if (amount <= minimumBid) {
      console.log(`Bid validation failed: ${amount} <= ${minimumBid}`);
      return res.status(400).json({ 
        error: `Bid must be higher than current highest bid (₱${minimumBid}). Your bid: ₱${amount}`,
        current_bid: minimumBid,
        your_bid: amount,
        required_bid: minimumBid + 1
      });
    }

    // Get user info with comprehensive debugging
    console.log('=== USER LOOKUP DEBUG ===');
    
    let users = [];
    let userLookupMethod = '';
    
    try {
      // Try integer lookup first
      [users] = await connection.query(
        'SELECT id, email, alias, credits, created_at, updated_at FROM users WHERE id = ?',
        [parseInt(bidder_id)]
      );
      userLookupMethod = 'integer_lookup';
      console.log('Integer lookup result:', users);
    } catch (intError) {
      console.log('Integer lookup failed:', intError.message);
      
      try {
        // Try string lookup
        [users] = await connection.query(
          'SELECT id, email, alias, credits, created_at, updated_at FROM users WHERE id = ?',
          [String(bidder_id)]
        );
        userLookupMethod = 'string_lookup';
        console.log('String lookup result:', users);
      } catch (stringError) {
        console.log('String lookup failed:', stringError.message);
        
        try {
          // Try CAST lookup
          [users] = await connection.query(
            'SELECT id, email, alias, credits, created_at, updated_at FROM users WHERE CAST(id AS CHAR) = ?',
            [String(bidder_id)]
          );
          userLookupMethod = 'cast_lookup';
          console.log('CAST lookup result:', users);
        } catch (castError) {
          console.log('CAST lookup failed:', castError.message);
        }
      }
    }

    if (users.length === 0) {
      console.log('User not found by ID, trying email lookup...');
      
      // Try to find user by email as fallback
      const [emailUsers] = await connection.query(
        'SELECT id, email, alias, credits, created_at, updated_at FROM users WHERE email = ?',
        [bidder_email]
      );
      
      console.log('Email lookup result:', emailUsers);
      
      if (emailUsers.length > 0) {
        users = emailUsers;
        userLookupMethod = 'email_lookup';
        console.log('Found user by email, using that ID instead');
      } else {
        console.log('User not found by email either');
        return res.status(404).json({ 
          error: 'User not found',
          bidder_id: bidder_id,
          user_email: bidder_email,
          lookup_method: userLookupMethod
        });
      }
    }

    const user = users[0];
    const userCredits = user.credits;
    const actualUserId = user.id;
    
    console.log('=== CREDIT VALIDATION DEBUG ===');
    console.log('User found:', {
      id: actualUserId,
      email: user.email,
      alias: user.alias,
      credits: userCredits,
      created_at: user.created_at,
      updated_at: user.updated_at
    });
    console.log('Credit check:', { 
      actualUserId, 
      userCredits, 
      amount, 
      sufficient: userCredits >= amount,
      bidder_id_from_jwt: bidder_id,
      user_email_from_db: user.email,
      lookup_method: userLookupMethod
    });
    
    if (userCredits < amount) {
      console.log('Insufficient credits error:', { 
        required: amount, 
        available: userCredits,
        user_id: actualUserId,
        user_email: user.email
      });
      return res.status(400).json({ 
        error: `Insufficient credits. Required: ₱${amount}, Available: ₱${userCredits}`,
        required: amount,
        available: userCredits,
        user_id: actualUserId,
        user_email: user.email
      });
    }

    // Call the PlaceBid stored procedure
    console.log('=== PLACEBID PROCEDURE CALL ===');
    console.log('Calling PlaceBid with:', { 
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
      item_id: item_id,
      user_id: actualUserId,
      user_email: user.email
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
