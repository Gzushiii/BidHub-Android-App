const { pool } = require('../config/database');

/**
 * Calculate auction end date based on duration in days
 * @param {number} durationDays - Number of days for auction
 * @returns {Date} - End date of the auction
 */
const calculateEndDate = (durationDays) => {
  const endDate = new Date();
  endDate.setDate(endDate.getDate() + durationDays);
  return endDate;
};

/**
 * Validate if a bid amount is valid for an item
 * @param {number} itemId - ID of the item
 * @param {number} bidAmount - Amount to bid
 * @param {number} currentUserId - ID of the user placing the bid
 * @returns {Promise<Object>} - Validation result with isValid and message
 */
const validateBidAmount = async (itemId, bidAmount, currentUserId) => {
  try {
    // Get item details
    const [items] = await pool.query(
      'SELECT * FROM items WHERE id = ? AND status = ?',
      [itemId, 'active']
    );

    if (items.length === 0) {
      return { isValid: false, message: 'Item not found or not active' };
    }

    const item = items[0];

    // Check if auction has ended
    if (new Date() > new Date(item.end_date)) {
      return { isValid: false, message: 'Auction has ended' };
    }

    // Check if user is not the seller
    if (currentUserId === item.seller_id) {
      return { isValid: false, message: 'Cannot bid on your own item' };
    }

    // Get current highest bid
    const [currentBids] = await pool.query(
      'SELECT MAX(amount) as max_bid FROM bids WHERE item_id = ?',
      [itemId]
    );

    const currentMaxBid = currentBids[0].max_bid || item.starting_price;
    const minimumBid = Math.max(currentMaxBid + 0.01, item.starting_price);

    if (bidAmount <= currentMaxBid) {
      return { 
        isValid: false, 
        message: `Bid must be higher than current highest bid ($${currentMaxBid.toFixed(2)})`,
        minimumBid: minimumBid
      };
    }

    // Check if bid meets reserve price
    if (item.reserve_price && bidAmount < item.reserve_price) {
      return { 
        isValid: false, 
        message: `Bid must meet or exceed reserve price ($${item.reserve_price.toFixed(2)})`,
        minimumBid: item.reserve_price
      };
    }

    return { isValid: true, message: 'Valid bid amount' };

  } catch (error) {
    console.error('Error validating bid amount:', error);
    return { isValid: false, message: 'Error validating bid' };
  }
};

/**
 * Check if an item can be updated (no bids placed yet)
 * @param {number} itemId - ID of the item
 * @returns {Promise<Object>} - Result with canUpdate and message
 */
const canUpdateItem = async (itemId) => {
  try {
    // Check if item has any bids
    const [bids] = await pool.query(
      'SELECT COUNT(*) as bid_count FROM bids WHERE item_id = ?',
      [itemId]
    );

    const bidCount = bids[0].bid_count;

    if (bidCount > 0) {
      return { 
        canUpdate: false, 
        message: 'Cannot update item after bids have been placed',
        bidCount: bidCount
      };
    }

    // Check if auction has ended
    const [items] = await pool.query(
      'SELECT end_date, status FROM items WHERE id = ?',
      [itemId]
    );

    if (items.length === 0) {
      return { canUpdate: false, message: 'Item not found' };
    }

    const item = items[0];

    if (item.status !== 'active' && item.status !== 'draft') {
      return { 
        canUpdate: false, 
        message: `Cannot update item with status: ${item.status}` 
      };
    }

    if (new Date() > new Date(item.end_date)) {
      return { canUpdate: false, message: 'Cannot update item after auction has ended' };
    }

    return { canUpdate: true, message: 'Item can be updated' };

  } catch (error) {
    console.error('Error checking if item can be updated:', error);
    return { canUpdate: false, message: 'Error checking item update status' };
  }
};

/**
 * Check if an item can be deleted
 * @param {number} itemId - ID of the item
 * @returns {Promise<Object>} - Result with canDelete and message
 */
const canDeleteItem = async (itemId) => {
  try {
    // Check if item has any bids
    const [bids] = await pool.query(
      'SELECT COUNT(*) as bid_count FROM bids WHERE item_id = ?',
      [itemId]
    );

    const bidCount = bids[0].bid_count;

    if (bidCount > 0) {
      return { 
        canDelete: false, 
        message: 'Cannot delete item after bids have been placed',
        bidCount: bidCount
      };
    }

    // Check item status
    const [items] = await pool.query(
      'SELECT status FROM items WHERE id = ?',
      [itemId]
    );

    if (items.length === 0) {
      return { canDelete: false, message: 'Item not found' };
    }

    const item = items[0];

    if (item.status === 'cancelled') {
      return { canDelete: false, message: 'Item is already cancelled' };
    }

    return { canDelete: true, message: 'Item can be deleted' };

  } catch (error) {
    console.error('Error checking if item can be deleted:', error);
    return { canDelete: false, message: 'Error checking item delete status' };
  }
};

/**
 * Check if a user can retract a bid
 * @param {number} bidId - ID of the bid
 * @param {number} userId - ID of the user
 * @returns {Promise<Object>} - Result with canRetract and message
 */
const canRetractBid = async (bidId, userId) => {
  try {
    // Get bid details
    const [bids] = await pool.query(
      'SELECT * FROM bids WHERE id = ? AND bidder_id = ?',
      [bidId, userId]
    );

    if (bids.length === 0) {
      return { canRetract: false, message: 'Bid not found or not owned by user' };
    }

    const bid = bids[0];

    // Check if bid is the current highest bid
    const [highestBids] = await pool.query(
      'SELECT MAX(amount) as max_bid, MAX(id) as max_bid_id FROM bids WHERE item_id = ?',
      [bid.item_id]
    );

    const isHighestBid = highestBids[0].max_bid_id === bidId;

    if (isHighestBid) {
      return { 
        canRetract: false, 
        message: 'Cannot retract the current highest bid' 
      };
    }

    // Check if auction has ended
    const [items] = await pool.query(
      'SELECT end_date FROM items WHERE id = ?',
      [bid.item_id]
    );

    if (items.length === 0) {
      return { canRetract: false, message: 'Item not found' };
    }

    const item = items[0];

    if (new Date() > new Date(item.end_date)) {
      return { canRetract: false, message: 'Cannot retract bid after auction has ended' };
    }

    return { canRetract: true, message: 'Bid can be retracted' };

  } catch (error) {
    console.error('Error checking if bid can be retracted:', error);
    return { canRetract: false, message: 'Error checking bid retraction status' };
  }
};

/**
 * Validate credit purchase amount
 * @param {number} amount - Amount to purchase
 * @returns {Object} - Validation result
 */
const validateCreditPurchase = (amount) => {
  const minAmount = 1.00;
  const maxAmount = 10000.00;

  if (amount < minAmount) {
    return { 
      isValid: false, 
      message: `Minimum purchase amount is $${minAmount.toFixed(2)}` 
    };
  }

  if (amount > maxAmount) {
    return { 
      isValid: false, 
      message: `Maximum purchase amount is $${maxAmount.toFixed(2)}` 
    };
  }

  return { isValid: true, message: 'Valid purchase amount' };
};

module.exports = {
  calculateEndDate,
  validateBidAmount,
  canUpdateItem,
  canDeleteItem,
  canRetractBid,
  validateCreditPurchase
};
