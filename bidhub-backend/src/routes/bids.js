const express = require('express');
const { authenticateToken, optionalAuth, checkBidOwnership } = require('../middleware/auth');
const { paginationSchema } = require('../validators/items');
const { canRetractBid } = require('../utils/validators');
const { pool } = require('../config/database');

const router = express.Router();

// Place a bid
router.post('/place', authenticateToken, async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    await connection.beginTransaction();

    const { item_id, amount } = req.body;
    const bidder_id = req.user.id;
    const bidder_alias = req.user.alias;

    // Add detailed logging
    console.log('=== BID PLACEMENT DEBUG ===');
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
    console.log('Checking user credits for bidder_id:', bidder_id, 'Type:', typeof bidder_id);
    
    // Try multiple query formats to handle potential data type issues
    let users = [];
    try {
      // First try with integer
      [users] = await connection.query(
        'SELECT id, email, alias, credits FROM users WHERE id = ?',
        [parseInt(bidder_id)]
      );
      console.log('Integer query result:', users);
    } catch (intError) {
      console.log('Integer query failed:', intError.message);
      
      try {
        // Try with string
        [users] = await connection.query(
          'SELECT id, email, alias, credits FROM users WHERE id = ?',
          [String(bidder_id)]
        );
        console.log('String query result:', users);
      } catch (stringError) {
        console.log('String query failed:', stringError.message);
        
        // Try with CAST
        [users] = await connection.query(
          'SELECT id, email, alias, credits FROM users WHERE CAST(id AS CHAR) = ?',
          [String(bidder_id)]
        );
        console.log('CAST query result:', users);
      }
    }

    console.log('Final user query result:', users);

    if (users.length === 0) {
      console.log('User not found in database with bidder_id:', bidder_id);
      
      // Try to find user by email as fallback
      const [emailUsers] = await connection.query(
        'SELECT id, email, alias, credits FROM users WHERE email = ?',
        [req.user.email]
      );
      console.log('Email fallback query result:', emailUsers);
      
      if (emailUsers.length > 0) {
        console.log('Found user by email, using that ID instead');
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
    console.log('User credits check:', { 
      actualUserId, 
      userCredits, 
      amount, 
      sufficient: userCredits >= amount,
      bidder_id_from_token: bidder_id,
      user_email: users[0].email
    });
    
    if (userCredits < amount) {
      console.log('Insufficient credits error:', { 
        required: amount, 
        available: userCredits,
        user_id: actualUserId,
        user_email: users[0].email
      });
      return res.status(400).json({ 
        error: 'Insufficient credits',
        required: amount,
        available: userCredits,
        user_id: actualUserId
      });
    }

    // Call the PlaceBid stored procedure with the actual user ID we found
    console.log('Calling PlaceBid stored procedure with:', { 
      item_id, 
      actual_bidder_id: actualUserId, 
      original_bidder_id: bidder_id,
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

// Get all user's bids
router.get('/', authenticateToken, async (req, res) => {
  try {
    const { error, value } = paginationSchema.validate(req.query);
    if (error) {
      return res.status(400).json({ 
        error: 'Validation failed', 
        details: error.details.map(d => d.message) 
      });
    }

    const { limit, offset } = value;
    const userId = req.user.id;

    // Get user's bids with item details
    const [bids] = await pool.query(
      `SELECT 
        b.*,
        i.title as item_title,
        i.description as item_description,
        i.current_price as item_current_price,
        i.end_date as item_end_date,
        i.status as item_status,
        c.name as category_name,
        u.alias as seller_alias
      FROM bids b
      JOIN items i ON b.item_id = i.id
      JOIN categories c ON i.category_id = c.id
      JOIN users u ON i.seller_id = u.id
      WHERE b.bidder_id = ?
      ORDER BY b.placed_at DESC
      LIMIT ? OFFSET ?`,
      [userId, limit, offset]
    );

    // Get total count for pagination
    const [countResult] = await pool.query(
      'SELECT COUNT(*) as total FROM bids WHERE bidder_id = ?',
      [userId]
    );

    const total = countResult[0].total;

    res.json({
      bids,
      count: bids.length,
      total,
      limit,
      offset
    });

  } catch (err) {
    console.error('Bids fetch error:', err);
    res.status(500).json({ error: 'Failed to fetch bids' });
  }
});

// Get bids for specific item
router.get('/item/:itemId', optionalAuth, async (req, res) => {
  try {
    const { error, value } = paginationSchema.validate(req.query);
    if (error) {
      return res.status(400).json({ 
        error: 'Validation failed', 
        details: error.details.map(d => d.message) 
      });
    }

    const { limit, offset } = value;
    const { itemId } = req.params;

    // Check if item exists
    const [items] = await pool.query(
      'SELECT id, title, status FROM items WHERE id = ?',
      [itemId]
    );

    if (items.length === 0) {
      return res.status(404).json({ error: 'Item not found' });
    }

    // Get bids for the item
    const [bids] = await pool.query(
      `SELECT 
        b.id,
        b.amount,
        b.placed_at,
        b.is_winning,
        u.alias as bidder_alias
      FROM bids b
      JOIN users u ON b.bidder_id = u.id
      WHERE b.item_id = ?
      ORDER BY b.amount DESC, b.placed_at ASC
      LIMIT ? OFFSET ?`,
      [itemId, limit, offset]
    );

    // Get total count for pagination
    const [countResult] = await pool.query(
      'SELECT COUNT(*) as total FROM bids WHERE item_id = ?',
      [itemId]
    );

    const total = countResult[0].total;

    res.json({
      item: items[0],
      bids,
      count: bids.length,
      total,
      limit,
      offset
    });

  } catch (err) {
    console.error('Item bids fetch error:', err);
    res.status(500).json({ error: 'Failed to fetch item bids' });
  }
});

// Get bids by user (public bid history)
router.get('/user/:userId', optionalAuth, async (req, res) => {
  try {
    const { error, value } = paginationSchema.validate(req.query);
    if (error) {
      return res.status(400).json({ 
        error: 'Validation failed', 
        details: error.details.map(d => d.message) 
      });
    }

    const { limit, offset } = value;
    const { userId } = req.params;

    // Check if user exists
    const [users] = await pool.query(
      'SELECT id, alias FROM users WHERE id = ?',
      [userId]
    );

    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    // Get user's public bid history
    const [bids] = await pool.query(
      `SELECT 
        b.id,
        b.amount,
        b.placed_at,
        b.is_winning,
        i.id as item_id,
        i.title as item_title,
        i.current_price as item_current_price,
        i.end_date as item_end_date,
        i.status as item_status,
        c.name as category_name
      FROM bids b
      JOIN items i ON b.item_id = i.id
      JOIN categories c ON i.category_id = c.id
      WHERE b.bidder_id = ?
      ORDER BY b.placed_at DESC
      LIMIT ? OFFSET ?`,
      [userId, limit, offset]
    );

    // Get total count for pagination
    const [countResult] = await pool.query(
      'SELECT COUNT(*) as total FROM bids WHERE bidder_id = ?',
      [userId]
    );

    const total = countResult[0].total;

    res.json({
      user: users[0],
      bids,
      count: bids.length,
      total,
      limit,
      offset
    });

  } catch (err) {
    console.error('User bids fetch error:', err);
    res.status(500).json({ error: 'Failed to fetch user bids' });
  }
});

// Delete/retract bid
router.delete('/:id', authenticateToken, checkBidOwnership, async (req, res) => {
  const connection = await db.getConnection();
  
  try {
    await connection.beginTransaction();

    const bidId = req.params.id;
    const bid = req.bid; // From checkBidOwnership middleware

    // Check if bid can be retracted
    const retractCheck = await canRetractBid(bidId, req.user.id);
    if (!retractCheck.canRetract) {
      return res.status(400).json({ 
        error: retractCheck.message 
      });
    }

    // Get current highest bid info
    const [highestBids] = await connection.query(
      `SELECT 
        MAX(amount) as max_amount,
        MAX(id) as max_bid_id,
        bidder_id as highest_bidder_id
      FROM bids 
      WHERE item_id = ? AND id != ?`,
      [bid.item_id, bidId]
    );

    // Delete the bid
    await connection.query(
      'DELETE FROM bids WHERE id = ?',
      [bidId]
    );

    // Update item current_price if this was the highest bid
    if (highestBids[0].max_amount) {
      await connection.query(
        'UPDATE items SET current_price = ? WHERE id = ?',
        [highestBids[0].max_amount, bid.item_id]
      );
    } else {
      // No other bids, reset to starting price
      const [items] = await connection.query(
        'SELECT starting_price FROM items WHERE id = ?',
        [bid.item_id]
      );
      
      if (items.length > 0) {
        await connection.query(
          'UPDATE items SET current_price = ? WHERE id = ?',
          [items[0].starting_price, bid.item_id]
        );
      }
    }

    // Refund credits to user
    await connection.query(
      'UPDATE users SET credits = credits + ? WHERE id = ?',
      [bid.amount, bid.bidder_id]
    );

    // Record the refund transaction
    await connection.query(
      `INSERT INTO credit_transactions 
       (user_id, type, amount, description, status, reference) 
       VALUES (?, 'refund', ?, 'Bid retraction refund', 'completed', ?)`,
      [bid.bidder_id, bid.amount, bid.item_id]
    );

    await connection.commit();

    res.json({
      message: 'Bid retracted successfully',
      refunded_amount: bid.amount,
      bid_id: bidId
    });

  } catch (err) {
    await connection.rollback();
    console.error('Bid retraction error:', err);
    res.status(500).json({ error: 'Failed to retract bid' });
  } finally {
    connection.release();
  }
});

module.exports = router;
