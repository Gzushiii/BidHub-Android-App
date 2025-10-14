const jwt = require('jsonwebtoken');
const db = require('../config/database');

const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: 'Access token required' });
  }

  jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ error: 'Invalid or expired token' });
    }
    req.user = user;
    next();
  });
};

const optionalAuth = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    req.user = null;
    return next();
  }

  jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
    if (err) {
      req.user = null;
    } else {
      req.user = user;
    }
    next();
  });
};

/**
 * Middleware to check if user owns an item
 * Attaches the item to req.item if found and owned
 */
const checkItemOwnership = async (req, res, next) => {
  try {
    const itemId = req.params.id;
    const userId = req.user.id;

    if (!itemId) {
      return res.status(400).json({ error: 'Item ID is required' });
    }

    // Get item details
    const [items] = await db.query(
      'SELECT * FROM items WHERE id = ?',
      [itemId]
    );

    if (items.length === 0) {
      return res.status(404).json({ error: 'Item not found' });
    }

    const item = items[0];

    // Check ownership
    if (item.seller_id !== userId) {
      return res.status(403).json({ 
        error: 'Access denied. You can only modify your own items.' 
      });
    }

    // Attach item to request for use in route handlers
    req.item = item;
    next();

  } catch (error) {
    console.error('Error checking item ownership:', error);
    res.status(500).json({ error: 'Error verifying item ownership' });
  }
};

/**
 * Middleware to check if user owns a bid
 * Attaches the bid to req.bid if found and owned
 */
const checkBidOwnership = async (req, res, next) => {
  try {
    const bidId = req.params.id;
    const userId = req.user.id;

    if (!bidId) {
      return res.status(400).json({ error: 'Bid ID is required' });
    }

    // Get bid details
    const [bids] = await db.query(
      'SELECT * FROM bids WHERE id = ?',
      [bidId]
    );

    if (bids.length === 0) {
      return res.status(404).json({ error: 'Bid not found' });
    }

    const bid = bids[0];

    // Check ownership
    if (bid.bidder_id !== userId) {
      return res.status(403).json({ 
        error: 'Access denied. You can only modify your own bids.' 
      });
    }

    // Attach bid to request for use in route handlers
    req.bid = bid;
    next();

  } catch (error) {
    console.error('Error checking bid ownership:', error);
    res.status(500).json({ error: 'Error verifying bid ownership' });
  }
};

/**
 * Middleware to check if user can access their own data
 * Used for user-specific endpoints like /api/credits/balance
 */
const checkUserAccess = (req, res, next) => {
  const requestedUserId = req.params.userId || req.params.id;
  const authenticatedUserId = req.user.id;

  if (requestedUserId && parseInt(requestedUserId) !== authenticatedUserId) {
    return res.status(403).json({ 
      error: 'Access denied. You can only access your own data.' 
    });
  }

  next();
};

module.exports = { 
  authenticateToken, 
  optionalAuth, 
  checkItemOwnership, 
  checkBidOwnership, 
  checkUserAccess 
};
