/**
 * Unified Item Lookup Utility
 * 
 * This utility provides consistent item lookup across all endpoints,
 * ensuring that read and write paths use the same logic and data source.
 */

const { pool } = require('../config/database');

/**
 * Fetch an active item by UUID ID
 * @param {string} itemId - The UUID of the item to fetch
 * @param {object} connection - Database connection (optional, will use pool if not provided)
 * @returns {Promise<object|null>} The item object or null if not found
 */
async function fetchActiveItem(itemId, connection = null) {
  if (!itemId || typeof itemId !== 'string') {
    throw new Error('Invalid item ID provided');
  }

  const id = itemId.trim();
  const db = connection || pool;

  try {
    console.log('=== UNIFIED ITEM LOOKUP ===');
    console.log('Looking up item with ID:', id);
    console.log('ID type:', typeof id);
    console.log('ID length:', id.length);

    // Use the same query logic as v_active_items view
    const [rows] = await db.execute(
      `SELECT 
        i.uuid_id as id,
        i.title,
        i.description,
        i.category_id,
        i.seller_id,
        i.seller_email,
        i.starting_bid,
        i.current_bid,
        i.buy_now_price,
        i.status,
        i.created_at,
        i.updated_at,
        i.end_date,
        u.username as seller_username,
        u.email as seller_user_email,
        c.name as category_name,
        c.description as category_description,
        (SELECT COUNT(*) FROM bids WHERE item_id = i.id OR item_uuid_id = i.uuid_id) as bid_count
      FROM items i
      LEFT JOIN users u ON i.seller_id = u.id
      LEFT JOIN categories c ON i.category_id = c.id
      WHERE i.uuid_id = ? AND i.status = 'active' AND i.uuid_id IS NOT NULL`,
      [id]
    );

    console.log('Item lookup result:', rows.length > 0 ? 'FOUND' : 'NOT FOUND');
    if (rows.length > 0) {
      console.log('Item details:', {
        id: rows[0].id,
        title: rows[0].title,
        status: rows[0].status,
        seller_id: rows[0].seller_id
      });
    }

    return rows.length > 0 ? rows[0] : null;
  } catch (error) {
    console.error('Error in fetchActiveItem:', error);
    throw error;
  }
}

/**
 * Fetch an item by UUID ID (including draft items)
 * @param {string} itemId - The UUID of the item to fetch
 * @param {object} connection - Database connection (optional)
 * @returns {Promise<object|null>} The item object or null if not found
 */
async function fetchItemById(itemId, connection = null) {
  if (!itemId || typeof itemId !== 'string') {
    throw new Error('Invalid item ID provided');
  }

  const id = itemId.trim();
  const db = connection || pool;

  try {
    console.log('=== ITEM LOOKUP (ALL STATUSES) ===');
    console.log('Looking up item with ID:', id);

    const [rows] = await db.execute(
      `SELECT 
        i.uuid_id as id,
        i.title,
        i.description,
        i.category_id,
        i.seller_id,
        i.seller_email,
        i.starting_bid,
        i.current_bid,
        i.buy_now_price,
        i.status,
        i.created_at,
        i.updated_at,
        i.end_date,
        u.username as seller_username,
        u.email as seller_user_email,
        c.name as category_name,
        c.description as category_description,
        (SELECT COUNT(*) FROM bids WHERE item_id = i.id OR item_uuid_id = i.uuid_id) as bid_count
      FROM items i
      LEFT JOIN users u ON i.seller_id = u.id
      LEFT JOIN categories c ON i.category_id = c.id
      WHERE i.uuid_id = ? AND i.uuid_id IS NOT NULL`,
      [id]
    );

    console.log('Item lookup result:', rows.length > 0 ? 'FOUND' : 'NOT FOUND');
    if (rows.length > 0) {
      console.log('Item details:', {
        id: rows[0].id,
        title: rows[0].title,
        status: rows[0].status,
        seller_id: rows[0].seller_id
      });
    }

    return rows.length > 0 ? rows[0] : null;
  } catch (error) {
    console.error('Error in fetchItemById:', error);
    throw error;
  }
}

/**
 * Validate item for bidding/purchasing
 * @param {object} item - The item object
 * @param {number} userId - The user ID attempting the action
 * @returns {object} Validation result with success boolean and error details
 */
function validateItemForAction(item, userId) {
  if (!item) {
    return {
      success: false,
      error: 'item_not_found',
      details: 'item_does_not_exist',
      message: 'Item not found'
    };
  }

  if (item.status !== 'active') {
    return {
      success: false,
      error: 'item_not_available',
      details: 'item_not_active',
      message: `Item is not available for purchase (status: ${item.status})`
    };
  }

  if (item.seller_id === userId) {
    return {
      success: false,
      error: 'invalid_action',
      details: 'cannot_buy_own_item',
      message: 'Cannot perform this action on your own item'
    };
  }

  return { success: true };
}

/**
 * Get item images by UUID ID or integer ID
 * @param {string|number} itemId - The UUID or integer ID of the item
 * @param {object} connection - Database connection (optional)
 * @returns {Promise<Array>} Array of image objects
 */
async function getItemImages(itemId, connection = null) {
  if (!itemId) {
    return [];
  }

  const db = connection || pool;

  try {
    // First, try to resolve UUID to integer ID if needed
    // The item_images table uses item_id (integer), not item_uuid_id
    let integerItemId = null;
    
    // If itemId is a number, use it directly
    if (typeof itemId === 'number' || /^\d+$/.test(String(itemId))) {
      integerItemId = parseInt(itemId, 10);
    } else {
      // If it's a UUID, resolve it to integer ID via items table
      const [itemRows] = await db.execute(
        'SELECT id FROM items WHERE uuid_id = ? OR id = ? LIMIT 1',
        [itemId, itemId]
      );
      
      if (itemRows.length > 0) {
        integerItemId = itemRows[0].id;
      } else {
        // Fallback: try direct lookup if itemId might be integer as string
        integerItemId = parseInt(itemId, 10);
        if (isNaN(integerItemId)) {
          console.warn(`getItemImages: Could not resolve item ID: ${itemId}`);
          return [];
        }
      }
    }

    // Query item_images using integer ID (the actual column in the table)
    const [rows] = await db.execute(
      'SELECT * FROM item_images WHERE item_id = ? ORDER BY display_order',
      [integerItemId]
    );

    return rows;
  } catch (error) {
    console.error('Error in getItemImages:', error);
    return [];
  }
}

/**
 * Get recent bids for an item by UUID ID
 * @param {string} itemId - The UUID of the item
 * @param {object} connection - Database connection (optional)
 * @returns {Promise<Array>} Array of bid objects
 */
async function getItemBids(itemId, connection = null) {
  if (!itemId) {
    return [];
  }

  const db = connection || pool;

  try {
    // First, resolve UUID to integer ID if needed (bids table only has item_id as integer FK)
    let integerItemId = null;
    
    if (typeof itemId === 'string' && itemId.trim().length > 0) {
      const id = itemId.trim();
      // Check if it's a UUID or numeric ID
      const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id);
      
      if (isUuid) {
        // Resolve UUID to integer ID
        const [itemRows] = await db.execute(
          'SELECT id FROM items WHERE uuid_id = ? LIMIT 1',
          [id]
        );
        if (itemRows.length > 0) {
          integerItemId = itemRows[0].id;
        }
      } else if (!isNaN(parseInt(id, 10))) {
        // It's already a numeric ID
        integerItemId = parseInt(id, 10);
      }
    } else if (typeof itemId === 'number') {
      integerItemId = itemId;
    }

    if (!integerItemId) {
      return [];
    }

    // Query bids using integer item_id
    const [rows] = await db.execute(
      `SELECT b.*, u.alias as bidder_alias 
       FROM bids b 
       JOIN users u ON b.bidder_id = u.id 
       WHERE b.item_id = ? 
       ORDER BY b.created_at DESC 
       LIMIT 10`,
      [integerItemId]
    );

    return rows;
  } catch (error) {
    console.error('Error in getItemBids:', error);
    return [];
  }
}

module.exports = {
  fetchActiveItem,
  fetchItemById,
  validateItemForAction,
  getItemImages,
  getItemBids
};
