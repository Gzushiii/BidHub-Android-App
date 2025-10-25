/**
 * Fixed Item Lookup Utilities
 * 
 * This version only uses columns that actually exist in the database schema.
 * Based on actual schema inspection, we have: id, uuid_id, title, description, 
 * category_id, seller_id, seller_email, starting_bid, current_bid, buy_now_price,
 * status, created_at, updated_at
 */

/**
 * Fetch the raw item record without applying availability rules.
 * Uses only existing database columns.
 *
 * @param {object} connection
 * @param {string} itemId
 * @returns {Promise<object|null>}
 */
async function fetchItemRecord(connection, itemId) {
  const normalizedId = String(itemId ?? '').trim();
  
  if (!normalizedId) {
    return null;
  }

  try {
    // Try uuid_id first (preferred)
    let [rows] = await connection.query(
      'SELECT * FROM items WHERE uuid_id = ?',
      [normalizedId]
    );

    // If not found by uuid_id, try by integer id
    if (rows.length === 0) {
      [rows] = await connection.query(
        'SELECT * FROM items WHERE id = ?',
        [normalizedId]
      );
    }

    if (rows.length === 0) {
      return null;
    }

    const item = rows[0];
    
    // Add canonical_id for consistency
    item.canonical_id = item.uuid_id || item.id;
    
    return item;
  } catch (error) {
    console.error('Error fetching item record:', error);
    return null;
  }
}

/**
 * Determine if an item is available for transactions using only existing columns.
 *
 * @param {object} item
 * @returns {boolean}
 */
function isItemAvailable(item) {
  if (!item) {
    return false;
  }

  // Check status - only 'active' items are available
  if (item.status !== 'active') {
    return false;
  }

  // All other checks are based on non-existent columns, so we skip them
  // The database only has: id, uuid_id, title, description, category_id, 
  // seller_id, seller_email, starting_bid, current_bid, buy_now_price, 
  // status, created_at, updated_at

  return true;
}

/**
 * Explain why an item cannot be transacted upon using only existing columns.
 *
 * @param {object} item
 * @returns {string}
 */
function determineInactiveReason(item) {
  if (!item) {
    return 'not_found';
  }

  // Only check status since that's the only availability column that exists
  if (item.status && typeof item.status === 'string') {
    const status = item.status.trim().toLowerCase();
    if (status !== 'active') {
      if (['ended', 'sold', 'closed', 'completed'].includes(status)) {
        return 'auction_ended';
      }
      return `status_${status}`;
    }
  }

  return 'inactive';
}

/**
 * Fetch an item with detailed error information using only existing columns.
 *
 * @param {object} connection
 * @param {string} itemId
 * @returns {Promise<{item: object|null, error: object|null}>}
 */
async function fetchItemWithErrorInfo(connection, itemId) {
  const normalizedId = String(itemId ?? '').trim();
  
  if (!normalizedId) {
    return {
      item: null,
      error: {
        code: 'INVALID_ID',
        message: 'Item ID is required',
        http_status: 400,
        json: {
          error: 'invalid_request',
          details: 'missing_item_id',
          message: 'Item ID is required'
        }
      }
    };
  }

  const rawItem = await fetchItemRecord(connection, normalizedId);
  
  if (!rawItem) {
    return {
      item: null,
      error: {
        code: 'NOT_FOUND',
        message: 'Item not found',
        http_status: 404,
        json: {
          error: 'item_not_found',
          details: 'item_does_not_exist',
          message: 'Item not found',
          item_id: normalizedId
        }
      }
    };
  }

  if (!isItemAvailable(rawItem)) {
    const reason = determineInactiveReason(rawItem);
    
    return {
      item: null,
      error: {
        code: 'NOT_ACTIVE',
        message: `Item is not available for transactions (${reason})`,
        http_status: 400,
        json: {
          error: reason === 'auction_ended' ? 'auction_ended' : 'not_active',
          details: reason,
          message: 'Item is not available for transactions',
          item_id: normalizedId,
          item_status: rawItem.status
        }
      }
    };
  }

  return { item: rawItem, error: null };
}

/**
 * Validate item for buy-now operations using only existing columns.
 *
 * @param {object} item
 * @param {number} buyerId
 * @returns {{valid: boolean, error?: object}}
 */
function validateItemForBuyNow(item, buyerId) {
  if (!item) {
    return {
      valid: false,
      error: {
        http_status: 404,
        json: {
          error: 'item_not_found',
          details: 'item_does_not_exist',
          message: 'Item not found'
        }
      }
    };
  }

  if (item.status !== 'active') {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'item_not_available',
          details: 'item_not_active',
          message: `Item is not available for purchase (status: ${item.status})`
        }
      }
    };
  }

  if (item.seller_id === buyerId) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'invalid_purchase',
          details: 'cannot_buy_own_item',
          message: 'Cannot buy your own item'
        }
      }
    };
  }

  return { valid: true };
}

/**
 * Validate item for bidding operations using only existing columns.
 *
 * @param {object} item
 * @param {number} bidderId
 * @returns {{valid: boolean, error?: object}}
 */
function validateItemForBidding(item, bidderId) {
  if (!item) {
    return {
      valid: false,
      error: {
        http_status: 404,
        json: {
          error: 'item_not_found',
          details: 'item_does_not_exist',
          message: 'Item not found'
        }
      }
    };
  }

  if (item.status !== 'active') {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'item_not_available',
          details: 'item_not_active',
          message: `Item is not available for bidding (status: ${item.status})`
        }
      }
    };
  }

  if (item.seller_id === bidderId) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'invalid_bid',
          details: 'cannot_bid_on_own_item',
          message: 'Cannot bid on your own item'
        }
      }
    };
  }

  return { valid: true };
}

module.exports = {
  fetchItemRecord,
  fetchItemWithErrorInfo,
  validateItemForBuyNow,
  validateItemForBidding,
  isItemAvailable,
  determineInactiveReason
};
