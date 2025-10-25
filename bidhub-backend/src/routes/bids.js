// Fixed bidding route with proper validation order
// This addresses the "Insufficient credits" error when bid amount is too low

const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { pool } = require('../config/database');
const {
  fetchItemWithErrorInfo,
  validateItemForBidding
} = require('../utils/itemHelpers');

const router = express.Router();

// Place a bid - unified lookup and validation
router.post('/place', authenticateToken, async (req, res) => {
  console.log('=== BID PLACEMENT REQUEST RECEIVED ===');
  console.log('Headers:', req.headers);
  console.log('Request body:', req.body);
  console.log('User from JWT:', req.user);

  const connection = await pool.getConnection();

  try {
    await connection.query('SET SESSION wait_timeout = 30');
    await connection.beginTransaction();

    const { item_id, amount } = req.body;
    const bidder_id = req.user.id;
    const bidder_alias = req.user.alias;

    const normalizedItemId = String(item_id ?? '').trim();
    const bidAmount = Number(amount);

    if (!normalizedItemId || !Number.isFinite(bidAmount) || bidAmount <= 0) {
      console.log('Validation failed: invalid bid payload', {
        normalizedItemId,
        bidAmount
      });
      await connection.rollback();
      return res.status(400).json({
        error: 'invalid_bid',
        details: 'invalid_payload',
        message: 'Invalid bid data'
      });
    }

    const { item, error: itemError } = await fetchItemWithErrorInfo(
      connection,
      normalizedItemId
    );

    if (!item) {
      console.log('Item lookup failed for bid', {
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
          request_item_id: normalizedItemId
        });
    }

    const { valid, error: validationError } = validateItemForBidding(
      item,
      bidder_id
    );

    if (!valid) {
      console.log('Item not valid for bidding', {
        normalizedItemId,
        validationError
      });
      await connection.rollback();
      return res
        .status(validationError.http_status)
        .json(validationError.json);
    }

    const canonicalItemId = item.canonical_id || normalizedItemId;
    const startingPrice = Number(
      item.starting_price ?? item.starting_bid ?? 0
    );

    let currentMaxBid = 0;
    try {
      const [currentBids] = await connection.query(
        'SELECT MAX(amount) as max_bid FROM bids WHERE item_id = ? OR item_uuid_id = ?',
        [canonicalItemId, canonicalItemId]
      );
      currentMaxBid = Number(currentBids[0]?.max_bid ?? 0);
    } catch (lookupError) {
      if (lookupError.code === 'ER_BAD_FIELD_ERROR') {
        const [currentBids] = await connection.query(
          'SELECT MAX(amount) as max_bid FROM bids WHERE item_id = ?',
          [canonicalItemId]
        );
        currentMaxBid = Number(currentBids[0]?.max_bid ?? 0);
      } else {
        throw lookupError;
      }
    }

    const minimumBid = Math.max(
      currentMaxBid || startingPrice,
      startingPrice
    );

    if (bidAmount <= minimumBid) {
      await connection.rollback();
      return res.status(400).json({
        error: 'bid_too_low',
        details: 'amount_not_high_enough',
        message: `Bid must be higher than current highest bid (₱${minimumBid}).`,
        current_bid: minimumBid,
        your_bid: bidAmount,
        required_bid: minimumBid + 1
      });
    }

    let users = [];
    try {
      [users] = await connection.query(
        'SELECT id, email, alias, credits FROM users WHERE id = ?',
        [parseInt(bidder_id, 10)]
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
      const [emailUsers] = await connection.query(
        'SELECT id, email, alias, credits FROM users WHERE email = ?',
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
          user_email: req.user.email
        });
      }
    }

    const userCredits = Number(users[0].credits ?? 0);
    const actualUserId = users[0].id;

    if (userCredits < bidAmount) {
      await connection.rollback();
      return res.status(400).json({
        error: 'insufficient_credits',
        details: 'balance_too_low',
        message: `Insufficient credits. Required: ₱${bidAmount}, Available: ₱${userCredits}`,
        required: bidAmount,
        available: userCredits,
        user_id: actualUserId
      });
    }

    await connection.query('CALL PlaceBid(?, ?, ?, ?)', [
      canonicalItemId,
      actualUserId,
      bidAmount,
      bidder_alias
    ]);

    await connection.commit();

    return res.json({
      message: 'Bid placed successfully',
      bid_amount: bidAmount,
      item_id: canonicalItemId
    });
  } catch (err) {
    await connection.rollback();
    console.error('Bid placement error:', err);

    if (err.sqlMessage) {
      return res.status(400).json({
        error: 'bid_failed',
        details: 'sql_error',
        message: err.sqlMessage
      });
    }

    return res.status(500).json({
      error: 'bid_failed',
      details: 'internal_error',
      message: 'Failed to place bid'
    });
  } finally {
    if (connection) {
      connection.release();
    }
  }
});

module.exports = router;
