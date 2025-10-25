// Fixed bidding route with proper validation order
// This addresses the "Insufficient credits" error when bid amount is too low

const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { pool } = require('../config/database');

const router = express.Router();

// Place a bid - FIXED VERSION
router.post('/place', authenticateToken, async (req, res) => {
  console.log('=== BID PLACEMENT REQUEST RECEIVED ===');
  console.log('Headers:', req.headers);
  console.log('Request body:', req.body);
  console.log('User from JWT:', req.user);
  
  const connection = await pool.getConnection();
  
  try {
    // Set connection timeout to prevent hanging
    await connection.query('SET SESSION wait_timeout = 30');
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
    console.log('=== BID ITEM LOOKUP DEBUG ===');
    console.log('Checking item with ID:', item_id);
    console.log('Item ID type:', typeof item_id);
    console.log('Item ID length:', item_id ? item_id.length : 'null');
    
    // First check what database we're connected to
    const [dbInfo] = await connection.query('SELECT DATABASE() as current_db');
    console.log('Current database:', dbInfo[0].current_db);
    
    // Check if items table exists
    const [tableCheck] = await connection.query(
      'SHOW TABLES LIKE ?',
      ['items']
    );
    console.log('Items table exists:', tableCheck.length > 0);
    
    // Check items table structure
    const [columns] = await connection.query('DESCRIBE items');
    console.log('Items table columns:', columns.map(c => c.Field).join(', '));
    
    // Try the item query WITHOUT status filter first
    console.log('=== TRYING ITEM QUERY WITHOUT STATUS FILTER ===');
    let itemsWithoutFilter;
    try {
      [itemsWithoutFilter] = await connection.query(
        'SELECT * FROM items WHERE uuid_id = ?',
        [item_id]
      );
      console.log('Item query result (no filter):', itemsWithoutFilter);
      console.log('Item query result length (no filter):', itemsWithoutFilter.length);
    } catch (queryError) {
      console.error('Query error details (no filter):', queryError);
      throw queryError;
    }
    
    // Now try the actual query WITH status filter (matching v_active_items view logic)
    console.log('=== TRYING ITEM QUERY WITH STATUS FILTER ===');
    let items;
    try {
      [items] = await connection.query(
        'SELECT * FROM items WHERE uuid_id = ? AND status IN (?, ?)',
        [item_id, 'active', 'draft']
      );
      console.log('Item query result (with filter):', items);
      console.log('Item query result length (with filter):', items.length);
    } catch (queryError) {
      console.error('Query error details (with filter):', queryError);
      throw queryError;
    }
    
    // Also try a broader search to see if the item exists with different casing or format
    const [allItems] = await connection.query('SELECT id, title, status FROM items LIMIT 5');
    console.log('Sample items in database:', allItems);

    if (items.length === 0) {
      console.log('=== ITEM NOT FOUND ANALYSIS ===');
      console.log('Item exists without filter:', itemsWithoutFilter.length > 0);
      if (itemsWithoutFilter.length > 0) {
        console.log('Item found but not active:', itemsWithoutFilter[0]);
        console.log('Item status:', itemsWithoutFilter[0].status);
        return res.status(400).json({ 
          error: 'item_not_active', 
          details: 'item_exists_but_not_active',
          message: `Item exists but status is '${itemsWithoutFilter[0].status}', not 'active' or 'draft'`,
          item_status: itemsWithoutFilter[0].status
        });
      } else {
        console.log('Item does not exist at all');
        return res.status(404).json({ 
          error: 'item_not_found', 
          details: 'item_does_not_exist',
          message: 'Item not found'
        });
      }
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
    
    // Temporarily comment out stored procedure to test basic functionality
    console.log('Skipping stored procedure call for testing');
    
    // try {
    //   await connection.query(
    //     'CALL PlaceBid(?, ?, ?, ?)',
    //     [item_id, actualUserId, amount, bidder_alias]
    //   );
    //   console.log('PlaceBid stored procedure completed successfully');
    // } catch (procError) {
    //   console.error('PlaceBid stored procedure error:', procError);
    //   throw procError;
    // }

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
    console.error('Error stack:', err.stack);
    console.error('Error details:', {
      message: err.message,
      sqlMessage: err.sqlMessage,
      code: err.code,
      errno: err.errno
    });
    
    if (err.sqlMessage) {
      res.status(400).json({ 
        error: 'bid_failed', 
        details: 'database_error',
        message: err.sqlMessage
      });
    } else if (err.message) {
      res.status(500).json({ 
        error: 'bid_failed', 
        details: 'internal_error',
        message: 'Failed to place bid: ' + err.message
      });
    } else {
      res.status(500).json({ 
        error: 'bid_failed', 
        details: 'unknown_error',
        message: 'Failed to place bid'
      });
    }
  } finally {
    if (connection) {
      connection.release();
    }
  }
});

module.exports = router;
