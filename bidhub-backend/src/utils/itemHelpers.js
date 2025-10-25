/**
 * Unified Item Lookup Utilities
 *
 * This module provides consistent item lookup logic across all endpoints
 * to prevent 404 mismatches between READ (GET item details) and WRITE (bid/buy-now) operations.
 */

/**
 * Fetch an active item that's available for transactions (bid/buy-now)
 *
 * Uses the same filtering logic as the v_active_items view to ensure
 * items that display in the UI are also transactable.
 *
 * @param {object} connection - MySQL connection from pool
 * @param {string} itemId - Item ID (UUID or integer)
 * @returns {Promise<object|null>} Item object or null if not found/not active
 */
async function fetchActiveItem(connection, itemId) {
  try {
    // Normalize ID (trim whitespace, preserve exact casing for UUIDs)
    const normalizedId = String(itemId).trim();

    console.log('fetchActiveItem: Looking up item:', normalizedId);

    // First, try to find the item using the same view that GET /api/items/:id uses
    // This ensures consistency between what the UI shows and what transactions can access
    const [viewItems] = await connection.query(
      'SELECT * FROM v_active_items WHERE id = ?',
      [normalizedId]
    );

    if (viewItems.length > 0) {
      console.log('fetchActiveItem: Found item in v_active_items view');
      return viewItems[0];
    }

    // If not in view, check if item exists at all (for better error messages)
    const [allItems] = await connection.query(
      'SELECT id, title, status, created_at, deleted_at FROM items WHERE id = ?',
      [normalizedId]
    );

    if (allItems.length === 0) {
      console.log('fetchActiveItem: Item does not exist');
      return null; // Item doesn't exist at all
    }

    // Item exists but not in active view
    const item = allItems[0];
    console.log('fetchActiveItem: Item exists but not active:', {
      id: item.id,
      status: item.status,
      deleted_at: item.deleted_at
    });

    return null; // Item exists but not available for transactions
  } catch (error) {
    console.error('fetchActiveItem: Error:', error);
    throw error;
  }
}

/**
 * Fetch an item with detailed error information
 *
 * @param {object} connection - MySQL connection from pool
 * @param {string} itemId - Item ID
 * @returns {Promise<{item: object|null, error: object|null}>}
 */
async function fetchItemWithErrorInfo(connection, itemId) {
  const normalizedId = String(itemId).trim();

  try {
    // Try v_active_items view first
    const [viewItems] = await connection.query(
      'SELECT * FROM v_active_items WHERE id = ?',
      [normalizedId]
    );

    if (viewItems.length > 0) {
      return { item: viewItems[0], error: null };
    }

    // Check if item exists at all
    const [allItems] = await connection.query(
      'SELECT id, title, status, created_at, deleted_at, end_date FROM items WHERE id = ?',
      [normalizedId]
    );

    if (allItems.length === 0) {
      return {
        item: null,
        error: {
          code: 'NOT_FOUND',
          message: 'Item not found',
          http_status: 404,
          json: {
            error: 'not_found',
            details: 'item_does_not_exist',
            message: 'Item not found',
            item_id: normalizedId
          }
        }
      };
    }

    // Item exists but not active
    const item = allItems[0];
    const now = new Date();
    let reason = 'unknown';

    if (item.deleted_at !== null) {
      reason = 'deleted';
    } else if (item.status === 'draft') {
      reason = 'not_published';
    } else if (item.status === 'ended' || item.status === 'sold') {
      reason = 'auction_ended';
    } else if (item.end_date && new Date(item.end_date) < now) {
      reason = 'expired';
    } else {
      reason = `invalid_status_${item.status}`;
    }

    return {
      item: null,
      error: {
        code: 'NOT_ACTIVE',
        message: `Item is not available for transactions (${reason})`,
        http_status: 400,
        json: {
          error: 'not_active_or_available',
          details: reason,
          message: `Item is not available for transactions`,
          item_id: normalizedId,
          item_status: item.status,
          item_deleted: item.deleted_at !== null
        }
      }
    };
  } catch (error) {
    console.error('fetchItemWithErrorInfo: Database error:', error);
    return {
      item: null,
      error: {
        code: 'DB_ERROR',
        message: 'Database error while fetching item',
        http_status: 500,
        json: {
          error: 'internal_error',
          details: 'database_query_failed',
          message: 'Failed to fetch item'
        }
      }
    };
  }
}

/**
 * Validate that an item is available for bidding
 *
 * @param {object} item - Item object from database
 * @param {number} bidderId - User ID of the bidder
 * @returns {{valid: boolean, error: object|null}}
 */
function validateItemForBidding(item, bidderId) {
  if (!item) {
    return {
      valid: false,
      error: {
        http_status: 404,
        json: {
          error: 'not_found',
          message: 'Item not found'
        }
      }
    };
  }

  // Check if bidder is the seller
  if (item.seller_id === bidderId) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'forbidden',
          details: 'cannot_bid_own_item',
          message: 'Cannot bid on your own item'
        }
      }
    };
  }

  // Check if auction has ended
  if (item.end_date && new Date(item.end_date) < new Date()) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'forbidden',
          details: 'auction_ended',
          message: 'Auction has ended',
          end_date: item.end_date
        }
      }
    };
  }

  // Check if item has buy_now_only flag (if such a field exists)
  if (item.buy_now_only === 1 || item.buy_now_only === true) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'not_available',
          details: 'buy_now_only',
          message: 'This item is available for Buy Now only, not for bidding'
        }
      }
    };
  }

  return { valid: true, error: null };
}

/**
 * Validate that an item is available for Buy Now
 *
 * @param {object} item - Item object from database
 * @param {number} buyerId - User ID of the buyer
 * @returns {{valid: boolean, error: object|null}}
 */
function validateItemForBuyNow(item, buyerId) {
  if (!item) {
    return {
      valid: false,
      error: {
        http_status: 404,
        json: {
          error: 'not_found',
          message: 'Item not found'
        }
      }
    };
  }

  // Check if buyer is the seller
  if (item.seller_id === buyerId) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'forbidden',
          details: 'cannot_buy_own_item',
          message: 'Cannot buy your own item'
        }
      }
    };
  }

  // Check if item has buy_now_price
  if (!item.buy_now_price || item.buy_now_price <= 0) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'not_available',
          details: 'no_buy_now_price',
          message: 'Item does not have a Buy Now option'
        }
      }
    };
  }

  // Check if auction/sale has ended
  if (item.end_date && new Date(item.end_date) < new Date()) {
    return {
      valid: false,
      error: {
        http_status: 400,
        json: {
          error: 'forbidden',
          details: 'sale_ended',
          message: 'Sale period has ended',
          end_date: item.end_date
        }
      }
    };
  }

  return { valid: true, error: null };
}

module.exports = {
  fetchActiveItem,
  fetchItemWithErrorInfo,
  validateItemForBidding,
  validateItemForBuyNow
};
