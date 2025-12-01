// Fixed bidding route with proper validation order
// This addresses the "Insufficient credits" error when bid amount is too low

const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { pool } = require('../config/database');
const {
  fetchItemWithErrorInfo,
  validateItemForBidding
} = require('../utils/itemHelpers');
const { getItemWithErrorInfo } = require('../utils/itemResolver');

const router = express.Router();

// Place a bid - unified lookup and validation
router.post('/place', authenticateToken, async (req, res) => {
  const correlationId = req.headers['x-correlation-id'] || require('crypto').randomUUID();
  
  console.log('=== BID PLACEMENT REQUEST RECEIVED ===');
  console.log('Correlation ID:', correlationId);
  console.log('Route: POST /api/bids/place');
  console.log('Headers:', req.headers);
  console.log('Request body:', req.body);
  console.log('User from JWT:', req.user);

  const connection = await pool.getConnection();

  try {
    await connection.query('SET SESSION wait_timeout = 30');
    await connection.beginTransaction();

    const { item_id, amount } = req.body;
    const bidder_id = req.user.id;
    let bidder_alias = req.user.alias || req.user.username || req.user.email;

    // If alias is still missing, fetch it from database
    if (!bidder_alias || bidder_alias.trim() === '') {
      const [userRows] = await connection.query(
        'SELECT alias, username, email FROM users WHERE id = ?',
        [bidder_id]
      );
      if (userRows.length > 0) {
        bidder_alias = userRows[0].alias || userRows[0].username || userRows[0].email || `user_${bidder_id}`;
      } else {
        bidder_alias = `user_${bidder_id}`;
      }
    }

    const normalizedItemId = String(item_id ?? '').trim();
    const bidAmount = Number(amount);

    console.log('Bid request details:', {
      correlationId,
      route: 'POST /api/bids/place',
      normalizedItemId,
      bidder_id,
      bidAmount
    });

    // Log database connection info
    const [dbInfo] = await connection.query('SELECT DATABASE() AS db, @@hostname AS host');
    console.log('Database Info:', dbInfo[0]);

    if (!normalizedItemId || !Number.isFinite(bidAmount) || bidAmount <= 0) {
      console.log('Validation failed: invalid bid payload', {
        correlationId,
        normalizedItemId,
        bidAmount
      });
      await connection.rollback();
      return res.status(400).json({
        error: 'invalid_bid',
        details: 'invalid_payload',
        message: 'Invalid bid data',
        correlationId
      });
    }

    // Use flexible item resolver to handle multiple ID formats
    const { found, item, error: itemError } = await getItemWithErrorInfo(
      connection,
      normalizedItemId
    );

    console.log('Bid item lookup result:', {
      correlationId,
      route: 'POST /api/bids/place',
      itemId: normalizedItemId,
      found: found,
      itemError: itemError?.type
    });

    if (!found || !item) {
      console.log('Item lookup failed for bid', {
        correlationId,
        normalizedItemId,
        itemError
      });
      await connection.rollback();
      return res
        .status(itemError?.http_status || 404)
        .json({
          ...(itemError?.json || {
            error: 'item_not_found',
            message: 'Item not found'
          }),
          request_item_id: normalizedItemId,
          correlationId
        });
    }

    const { valid, error: validationError } = validateItemForBidding(
      item,
      bidder_id
    );

    console.log('Bid validation result:', {
      correlationId,
      valid,
      validationError: validationError?.json?.error
    });

    if (!valid) {
      console.log('Item not valid for bidding', {
        correlationId,
        normalizedItemId,
        validationError
      });
      await connection.rollback();
      return res
        .status(validationError.http_status)
        .json({
          ...validationError.json,
          correlationId
        });
    }

    const canonicalItemId = item.canonical_id || normalizedItemId;
    const numericItemId = item.id; // Numeric INT ID for stored procedure

    // Enhanced logging for debugging ID issues
    console.log('Item ID analysis:', {
      correlationId,
      requestedItemId: normalizedItemId,
      itemObject: {
        id: item.id,
        uuid_id: item.uuid_id,
        canonical_id: item.canonical_id
      },
      numericItemId,
      numericItemIdType: typeof numericItemId
    });

    // Defensive check: ensure we have a numeric ID
    if (!numericItemId || isNaN(parseInt(numericItemId, 10))) {
      console.log('Missing or invalid numeric item ID', {
        correlationId,
        item_id: item.id,
        uuid_id: item.uuid_id
      });
      await connection.rollback();
      return res.status(500).json({
        error: 'internal_error',
        details: 'missing_numeric_id',
        message: 'Item numeric ID not found',
        correlationId
      });
    }

    // CRITICAL: Lock item row to prevent race conditions
    // Check auction end date and lock item row atomically
    const [lockedItems] = await connection.query(
      `SELECT id, starting_price, starting_bid, end_date, status, seller_id
       FROM items 
       WHERE id = ? 
       FOR UPDATE`,
      [numericItemId]
    );

    if (lockedItems.length === 0) {
      await connection.rollback();
      return res.status(404).json({
        error: 'item_not_found',
        details: 'item_does_not_exist',
        message: 'Item not found',
        correlationId
      });
    }

    const lockedItem = lockedItems[0];

    // Check if auction has ended
    if (lockedItem.end_date && new Date(lockedItem.end_date) <= new Date()) {
      await connection.rollback();
      return res.status(400).json({
        error: 'auction_ended',
        details: 'auction_already_ended',
        message: 'This auction has already ended',
        end_date: lockedItem.end_date,
        correlationId
      });
    }

    // Check if item is still active (double-check after lock)
    if (lockedItem.status !== 'active') {
      await connection.rollback();
      return res.status(400).json({
        error: 'auction_ended',
        details: 'item_not_active',
        message: `Item is not available for bidding (status: ${lockedItem.status})`,
        item_status: lockedItem.status,
        correlationId
      });
    }

    const startingPrice = Number(
      lockedItem.starting_price ?? lockedItem.starting_bid ?? 0
    );

    // CRITICAL: Get current max bid WITH row lock to prevent race conditions
    // This must be done AFTER locking the item to ensure consistency
    let currentMaxBid = 0;
    try {
      const [currentBids] = await connection.query(
        `SELECT COALESCE(MAX(amount), 0) as max_bid 
         FROM bids 
         WHERE item_id = ? 
         AND status IN ('active', 'winning')`,
        [numericItemId]
      );
      currentMaxBid = Number(currentBids[0]?.max_bid ?? 0);
    } catch (lookupError) {
      console.error('Error looking up current max bid:', lookupError);
      await connection.rollback();
      throw lookupError;
    }

    // Calculate minimum bid: if there are existing bids, must be higher than max bid
    // Otherwise, must be at least the starting price
    const minimumBid = currentMaxBid > 0 
      ? currentMaxBid + 1  // Must be at least 1 higher than current max bid
      : Math.max(startingPrice, 1);  // Must be at least the starting price (or 1 if starting price is 0)

    if (bidAmount < minimumBid) {
      await connection.rollback();
      return res.status(400).json({
        error: 'bid_too_low',
        details: 'amount_not_high_enough',
        message: currentMaxBid > 0 
          ? `Bid must be higher than current highest bid (₱${currentMaxBid}). Minimum bid: ₱${minimumBid}`
          : `Bid must be at least the starting price (₱${startingPrice}).`,
        current_bid: currentMaxBid,
        starting_price: startingPrice,
        your_bid: bidAmount,
        required_bid: minimumBid,
        correlationId
      });
    }

    // CRITICAL: Lock user row to prevent race conditions on credit balance
    let users = [];
    try {
      [users] = await connection.query(
        'SELECT id, email, alias, credits FROM users WHERE id = ? FOR UPDATE',
        [parseInt(bidder_id, 10)]
      );
    } catch (intError) {
      try {
        [users] = await connection.query(
          'SELECT id, email, alias, credits FROM users WHERE id = ? FOR UPDATE',
          [String(bidder_id)]
        );
      } catch (stringError) {
        [users] = await connection.query(
          'SELECT id, email, alias, credits FROM users WHERE CAST(id AS CHAR) = ? FOR UPDATE',
          [String(bidder_id)]
        );
      }
    }

    if (users.length === 0) {
      const [emailUsers] = await connection.query(
        'SELECT id, email, alias, credits FROM users WHERE email = ? FOR UPDATE',
        [req.user.email]
      );

      if (emailUsers.length > 0) {
        users = emailUsers;
      } else {
        await connection.rollback();
        return res.status(404).json({
          error: 'user_not_found',
          details: 'bidder_missing',
          message: 'User not found',
          bidder_id: bidder_id,
          user_email: req.user.email,
          correlationId
        });
      }
    }

    const userCredits = Number(users[0].credits ?? 0);
    const actualUserId = users[0].id;

    // Re-check credits after lock (in case balance changed)
    if (userCredits < bidAmount) {
      await connection.rollback();
      return res.status(400).json({
        error: 'insufficient_credits',
        details: 'balance_too_low',
        message: `Insufficient credits. Required: ₱${bidAmount}, Available: ₱${userCredits}`,
        required: bidAmount,
        available: userCredits,
        user_id: actualUserId,
        correlationId
      });
    }

    // Call stored procedure with numeric INT ID (not UUID)
    console.log('Calling PlaceBid stored procedure', {
      correlationId,
      numericItemId,
      actualUserId,
      bidAmount,
      bidder_alias
    });

    await connection.query('CALL PlaceBid(?, ?, ?, ?)', [
      numericItemId,  // Use numeric INT ID instead of UUID
      actualUserId,
      bidAmount,
      bidder_alias
    ]);

    await connection.commit();

    console.log('Bid placed successfully', {
      correlationId,
      bid_amount: bidAmount,
      item_id: canonicalItemId,
      numeric_item_id: numericItemId
    });

    return res.json({
      message: 'Bid placed successfully',
      bid_amount: bidAmount,
      item_id: canonicalItemId,  // Return UUID for client compatibility
      correlationId
    });
  } catch (err) {
    await connection.rollback();
    console.error('Bid placement error:', err);

    if (err.sqlMessage) {
      return res.status(400).json({
        error: 'bid_failed',
        details: 'sql_error',
        message: err.sqlMessage,
        correlationId
      });
    }

    return res.status(500).json({
      error: 'bid_failed',
      details: 'internal_error',
      message: 'Failed to place bid',
      correlationId
    });
  } finally {
    if (connection) {
      connection.release();
    }
  }
});

module.exports = router;
