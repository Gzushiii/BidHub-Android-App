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
        u.username as seller_username,
        u.email as seller_user_email,
        c.name as category_name,
        c.description as category_description
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
        u.username as seller_username,
        u.email as seller_user_email,
        c.name as category_name,
        c.description as category_description
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
 * Get item images by UUID ID
 * @param {string} itemId - The UUID of the item
 * @param {object} connection - Database connection (optional)
 * @returns {Promise<Array>} Array of image objects
 */
async function getItemImages(itemId, connection = null) {
  if (!itemId || typeof itemId !== 'string') {
    return [];
  }

  const id = itemId.trim();
  const db = connection || pool;

  try {
    const [rows] = await db.execute(
      'SELECT * FROM item_images WHERE item_uuid_id = ? ORDER BY display_order',
      [id]
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
  if (!itemId || typeof itemId !== 'string') {
    return [];
  }

  const id = itemId.trim();
  const db = connection || pool;

  try {
    const [rows] = await db.execute(
      `SELECT b.*, u.alias as bidder_alias 
       FROM bids b 
       JOIN users u ON b.bidder_id = u.id 
       WHERE b.item_uuid_id = ? 
       ORDER BY b.placed_at DESC 
       LIMIT 10`,
      [id]
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
