const express = require('express');
const { pool } = require('../config/database');
const { authenticateToken, checkItemOwnership } = require('../middleware/auth');
const { createItemSchema, updateItemSchema, paginationSchema } = require('../validators/items');
const { calculateEndDate, canUpdateItem, canDeleteItem } = require('../utils/validators');
const {
  fetchActiveItem,
  fetchItemById,
  getItemImages,
  getItemBids
} = require('../utils/itemLookup');
const {
  fetchItemRecord,
  fetchItemWithErrorInfo,
  validateItemForBuyNow
} = require('../utils/itemHelpers');
const { getItemWithErrorInfo } = require('../utils/itemResolver');

const router = express.Router();

// Get all items with filtering and pagination (must come before /:id route)
router.get('/', async (req, res) => {
  try {
    const { 
      status = 'active', 
      category_id, 
      search,
      min_price,
      max_price,
      seller_email,
      limit = 20, 
      offset = 0 
    } = req.query;

    let query = 'SELECT * FROM v_active_items WHERE 1=1';
    const params = [];

    if (category_id) {
      query += ' AND category_id = ?';
      params.push(category_id);
    }

    if (search) {
      query += ' AND (title LIKE ? OR description LIKE ?)';
      params.push(`%${search}%`, `%${search}%`);
    }

    if (min_price) {
      query += ' AND current_price >= ?';
      params.push(parseFloat(min_price));
    }

    if (max_price) {
      query += ' AND current_price <= ?';
      params.push(parseFloat(max_price));
    }

    if (seller_email) {
      query += ' AND seller_email = ?';
      params.push(seller_email);
    }

    query += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
    params.push(parseInt(limit), parseInt(offset));

    const [items] = await pool.query(query, params);

    // Enhance items with bid_count and ensure seller_username is present
    // FIX: Properly map field names from v_active_items view to consistent API response format
    const enhancedItems = await Promise.all(items.map(async (item) => {
      // v_active_items returns: id (integer), uuid_id, starting_price, current_price
      // Need to map to: id (uuid), uuid_id, starting_bid/starting_price, current_bid/current_price
      const integerId = item.id; // Integer ID from view
      const uuidId = item.uuid_id; // UUID ID from view
      
      // Get bid count for this item
      const [bidCountResult] = await pool.query(
        'SELECT COUNT(*) as bid_count FROM bids WHERE item_id = ? OR item_uuid_id = ?',
        [integerId, uuidId]
      );
      
      const bidCount = bidCountResult[0]?.bid_count || 0;
      
      // Get images for this item - item_images table uses integer item_id FK
      let imageUrls = [];
      try {
        const [images] = await pool.query(
          `SELECT image_url FROM item_images 
           WHERE item_id = ? 
           ORDER BY display_order ASC`,
          [integerId]
        );
        
        // Extract image URLs and filter out null/empty values
        imageUrls = images
          .map(img => img.image_url)
          .filter(url => url != null && url.trim() !== '' && url !== 'null');
      } catch (imageError) {
        console.error(`Error fetching images for item ${integerId}:`, imageError);
        imageUrls = [];
      }
      
      // Normalize field names for frontend compatibility
      // View has: starting_price, current_price
      // Frontend expects: starting_bid (or starting_price), current_bid (or current_price)
      return {
        id: uuidId || integerId, // Use UUID as primary ID, fallback to integer
        uuid_id: uuidId, // Explicitly include UUID
        integer_id: integerId, // Include integer ID for reference
        title: item.title,
        description: item.description || '',
        category_id: item.category_id,
        category_name: item.category_name || null,
        seller_id: item.seller_id,
        seller_email: item.seller_email || null,
        seller_username: item.seller_username || null,
        seller_alias: item.seller_alias || null,
        // Provide both field name variants for compatibility
        starting_price: item.starting_price || item.starting_bid || 0,
        starting_bid: item.starting_bid || item.starting_price || 0,
        current_price: item.current_price || item.current_bid || 0,
        current_bid: item.current_bid || item.current_price || 0,
        buy_now_price: item.buy_now_price || null,
        status: item.status || 'active',
        condition: item.item_condition || item.condition || 'good',
        end_date: item.end_date || item.bid_deadline || null,
        bid_deadline: item.bid_deadline || item.end_date || null,
        created_at: item.created_at || null,
        updated_at: item.updated_at || null,
        bid_count: bidCount,
        images: imageUrls, // Always return array, even if empty
      };
    }));

    // Get total count for pagination
    let countQuery = 'SELECT COUNT(*) as total FROM v_active_items WHERE 1=1';
    const countParams = [];
    
    if (category_id) {
      countQuery += ' AND category_id = ?';
      countParams.push(category_id);
    }
    
    if (search) {
      countQuery += ' AND (title LIKE ? OR description LIKE ?)';
      countParams.push(`%${search}%`, `%${search}%`);
    }
    
    if (min_price) {
      countQuery += ' AND current_price >= ?';
      countParams.push(parseFloat(min_price));
    }
    
    if (max_price) {
      countQuery += ' AND current_price <= ?';
      countParams.push(parseFloat(max_price));
    }
    
    if (seller_email) {
      countQuery += ' AND seller_email = ?';
      countParams.push(seller_email);
    }

    const [countResult] = await pool.query(countQuery, countParams);
    const total = countResult[0].total;

    res.json({ 
      items: enhancedItems, 
      count: enhancedItems.length,
      total,
      limit: parseInt(limit),
      offset: parseInt(offset)
    });
  } catch (err) {
    console.error('Items fetch error:', err);
    res.status(500).json({ error: 'Failed to fetch items' });
  }
});

// Get specific item by ID (for existence checking and 404 recovery)
router.get('/:id', async (req, res) => {
  const connection = await pool.getConnection();
  const correlationId = `get_item_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  
  try {
    const itemId = req.params.id;
    
    if (!itemId) {
      return res.status(400).json({
        error: 'invalid_request',
        message: 'Item ID is required',
        correlationId
      });
    }

    // Use flexible item resolver to handle multiple ID formats
    const { found, item, error: lookupError } = await getItemWithErrorInfo(
      connection,
      itemId
    );

    console.log('Item lookup result:', {
      correlationId,
      route: 'GET /api/items/:id',
      itemId: itemId,
      found: found,
      lookupError: lookupError?.type
    });

    if (!found || !item) {
      return res.status(404).json({
        error: 'item_not_found',
        message: 'Item not found',
        requested_id: itemId,
        correlationId
      });
    }

    // Get complete item details with seller info, bid count, and images
    const [itemDetails] = await connection.query(
      `SELECT 
        i.*,
        u.username as seller_username,
        u.email as seller_email,
        u.alias as seller_alias,
        c.name as category_name,
        c.description as category_description,
        (SELECT COUNT(*) FROM bids WHERE item_id = i.id OR item_uuid_id = i.uuid_id) as bid_count
      FROM items i
      LEFT JOIN users u ON i.seller_id = u.id
      LEFT JOIN categories c ON i.category_id = c.id
      WHERE i.id = ? OR i.uuid_id = ?
      LIMIT 1`,
      [item.id, item.uuid_id || item.id]
    );

    if (itemDetails.length === 0) {
      return res.status(404).json({
        error: 'item_not_found',
        message: 'Item not found',
        requested_id: itemId,
        correlationId
      });
    }

    const fullItem = itemDetails[0];

    // Get images for this item - item_images table uses integer item_id FK
    const [images] = await connection.query(
      'SELECT image_url, display_order FROM item_images WHERE item_id = ? ORDER BY display_order',
      [fullItem.id] // Use integer ID from items table
    );

    // Get recent bids
    const bids = await getItemBids(fullItem.uuid_id || fullItem.id, connection);

    // Return complete item details
    return res.json({
      success: true,
      item: {
        id: fullItem.uuid_id || fullItem.id,
        uuid_id: fullItem.uuid_id,
        title: fullItem.title,
        description: fullItem.description,
        category_id: fullItem.category_id,
        category_name: fullItem.category_name,
        seller_id: fullItem.seller_id,
        seller_email: fullItem.seller_email,
        seller_username: fullItem.seller_username,
        seller_alias: fullItem.seller_alias,
        starting_bid: fullItem.starting_bid || fullItem.starting_price,
        current_bid: fullItem.current_bid || fullItem.current_price,
        buy_now_price: fullItem.buy_now_price,
        status: fullItem.status,
        condition: fullItem.item_condition || fullItem.condition,
        end_date: fullItem.end_date || fullItem.bid_deadline,
        bid_count: fullItem.bid_count || 0,
        images: images.map(img => img.image_url),
        bids: bids,
        created_at: fullItem.created_at,
        updated_at: fullItem.updated_at
      },
      correlationId
    });

  } catch (error) {
    console.error('Error in GET /api/items/:id:', {
      correlationId,
      error: error.message,
      stack: error.stack
    });
    
    return res.status(500).json({
      error: 'internal_error',
      message: 'Internal server error',
      correlationId
    });
  } finally {
    connection.release();
  }
});

// Create new item (requires auth)
router.post('/', authenticateToken, async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    await connection.beginTransaction();

    // Validate input
    const { error, value } = createItemSchema.validate(req.body);
    if (error) {
      connection.release();
      return res.status(400).json({ 
        error: 'Validation failed', 
        details: error.details.map(d => d.message) 
      });
    }

    const {
      title,
      description,
      category_id,
      starting_price,
      reserve_price,
      duration_days,
      images = [],
      status = 'active' // Allow status to be specified
    } = value;

    // Provide default description if null or empty (database requires NOT NULL)
    const itemDescription = description && description.trim() !== '' ? description : 'No description provided';

    // Determine seller from authenticated user
    let seller_id = req.user?.id;
    if (!seller_id && req.body.seller_email) {
      // Fallback for legacy clients
      const [users] = await connection.query(
        'SELECT id FROM users WHERE email = ?',
        [req.body.seller_email]
      );
      if (users.length > 0) seller_id = users[0].id;
    }
    if (!seller_id) {
      await connection.rollback();
      connection.release();
      return res.status(401).json({ error: 'Unauthorized: seller id not resolved' });
    }

    // For draft items, don't set end_date (duration starts only after publish)
    let end_date = null;
    if (status === 'active') {
      end_date = calculateEndDate(duration_days);
    }

    // Create the item with UUID
    const itemUuid = require('crypto').randomUUID();
    // FIX: Insert both starting_price/starting_bid and current_price/current_bid for compatibility
    // The view uses starting_price and current_price, but stored procedures may use starting_bid/current_bid
    const [result] = await connection.query(
      `INSERT INTO items 
       (uuid_id, title, description, category_id, seller_id, starting_price, starting_bid, reserve_price,
        current_price, current_bid, end_date, status, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())`,
      [itemUuid, title, itemDescription, category_id, seller_id, starting_price, starting_price, reserve_price, starting_price, starting_price, end_date, status]
    );

    const itemIntegerId = result.insertId; // Integer ID for foreign keys
    const itemUuidId = itemUuid; // UUID for API responses

    // Add images if provided
    if (images.length > 0) {
      const imageValues = images.map((imageUrl, index) => 
        [itemIntegerId, imageUrl, index + 1] // Use integer ID for item_images FK
      );

      await connection.query(
        `INSERT INTO item_images (item_id, image_url, display_order) VALUES ?`,
        [imageValues]
      );
    }

    await connection.commit();

    // Get the created item with details using a new query (connection might be released)
    const [items] = await pool.query(
      'SELECT * FROM items WHERE uuid_id = ?',
      [itemUuidId]
    );
    
    const [itemImages] = await pool.query(
      'SELECT * FROM item_images WHERE item_id = ? ORDER BY display_order',
      [itemIntegerId] // Use integer ID to query item_images
    );

    const createdItem = items[0] || null;
    
    // Normalize response to include both field name variants for frontend compatibility
    const normalizedImages = (itemImages || []).map(img => 
      typeof img === 'string' ? img : (img.image_url || img.imageUrl || img)
    );

    res.status(201).json({
      message: 'Item created successfully',
      item: {
        id: itemUuidId,
        uuid_id: itemUuidId,
        integer_id: createdItem?.id || itemIntegerId,
        title: createdItem?.title || title,
        description: createdItem?.description || itemDescription,
        category_id: createdItem?.category_id || category_id,
        seller_id: createdItem?.seller_id || seller_id,
        starting_price: createdItem?.starting_price || createdItem?.starting_bid || starting_price,
        starting_bid: createdItem?.starting_bid || createdItem?.starting_price || starting_price,
        current_price: createdItem?.current_price || createdItem?.current_bid || starting_price,
        current_bid: createdItem?.current_bid || createdItem?.current_price || starting_price,
        buy_now_price: createdItem?.buy_now_price || null,
        status: createdItem?.status || status,
        end_date: createdItem?.end_date || end_date,
        bid_deadline: createdItem?.bid_deadline || createdItem?.end_date || end_date,
        created_at: createdItem?.created_at || null,
        updated_at: createdItem?.updated_at || null,
        images: normalizedImages
      }
    });

  } catch (err) {
    await connection.rollback();
    console.error('Item creation error:', err);
    console.error('Error stack:', err.stack);
    console.error('Error details:', {
      message: err.message,
      code: err.code,
      sqlState: err.sqlState,
      sqlMessage: err.sqlMessage
    });
    res.status(500).json({ 
      error: 'Failed to create item',
      details: process.env.NODE_ENV === 'development' ? err.message : undefined
    });
  } finally {
    connection.release();
  }
});

// Update existing item
router.put('/:id', authenticateToken, checkItemOwnership, async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    await connection.beginTransaction();

    const itemId = req.params.id;

    // Check if item can be updated
    const updateCheck = await canUpdateItem(itemId);
    if (!updateCheck.canUpdate) {
      return res.status(400).json({ 
        error: updateCheck.message,
        bidCount: updateCheck.bidCount
      });
    }

    // Validate input
    const { error, value } = updateItemSchema.validate(req.body);
    if (error) {
      return res.status(400).json({ 
        error: 'Validation failed', 
        details: error.details.map(d => d.message) 
      });
    }

    const { title, description, category_id, images } = value;

    // Build update query dynamically
    const updateFields = [];
    const updateValues = [];

    if (title !== undefined) {
      updateFields.push('title = ?');
      updateValues.push(title);
    }
    if (description !== undefined) {
      // Provide default if description is null or empty (database requires NOT NULL)
      const itemDescription = description && description.trim() !== '' ? description : 'No description provided';
      updateFields.push('description = ?');
      updateValues.push(itemDescription);
    }
    if (category_id !== undefined) {
      updateFields.push('category_id = ?');
      updateValues.push(category_id);
    }

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No valid fields to update' });
    }

    updateFields.push('updated_at = NOW()');

    // Update the item - itemId is used in WHERE clause, not in SET
    await connection.query(
      `UPDATE items SET ${updateFields.join(', ')} WHERE id = ? OR uuid_id = ?`,
      [...updateValues, itemId, itemId]
    );

    // Update images if provided
    if (images !== undefined) {
      // Remove existing images
      await connection.query(
        'DELETE FROM item_images WHERE item_id = ?',
        [itemId]
      );

      // Add new images
      if (images.length > 0) {
        const imageValues = images.map((imageUrl, index) => 
          [itemId, imageUrl, index + 1]
        );

        await connection.query(
          `INSERT INTO item_images (item_id, image_url, display_order) VALUES ?`,
          [imageValues]
        );
      }
    }

    await connection.commit();

    // Get the updated item with details
    const [items] = await connection.query(
      'SELECT * FROM items WHERE id = ?',
      [itemId]
    );

    const [itemImages] = await connection.query(
      'SELECT * FROM item_images WHERE item_id = ? ORDER BY display_order',
      [itemId]
    );

    res.json({
      message: 'Item updated successfully',
      item: {
        ...items[0],
        images: itemImages
      }
    });

  } catch (err) {
    await connection.rollback();
    console.error('Item update error:', err);
    res.status(500).json({ error: 'Failed to update item' });
  } finally {
    connection.release();
  }
});

// Delete/cancel item
router.delete('/:id', authenticateToken, checkItemOwnership, async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    await connection.beginTransaction();

    const itemId = req.params.id;

    // Check if item can be deleted
    const deleteCheck = await canDeleteItem(itemId);
    if (!deleteCheck.canDelete) {
      return res.status(400).json({ 
        error: deleteCheck.message,
        bidCount: deleteCheck.bidCount
      });
    }

    // Set status to cancelled instead of hard delete
    await connection.query(
      'UPDATE items SET status = ?, updated_at = NOW() WHERE id = ?',
      ['cancelled', itemId]
    );

    await connection.commit();

    res.json({
      message: 'Item cancelled successfully',
      item_id: itemId
    });

  } catch (err) {
    await connection.rollback();
    console.error('Item deletion error:', err);
    res.status(500).json({ error: 'Failed to cancel item' });
  } finally {
    connection.release();
  }
});

// Publish draft item (requires auth and ownership)
router.post('/:id/publish', authenticateToken, checkItemOwnership, async (req, res) => {
  const connection = await pool.getConnection();
  
  try {
    await connection.beginTransaction();

    const itemId = req.params.id;
    const { duration_days = 7 } = req.body;

    const itemRecord = await fetchItemRecord(connection, itemId);

    if (
      !itemRecord ||
      String(itemRecord.status || '').trim().toLowerCase() !== 'draft'
    ) {
      await connection.rollback();
      return res.status(404).json({
        error: 'draft_not_found',
        details: 'item_not_in_draft_state',
        message: 'Draft item not found',
        item_id: itemId
      });
    }

    const canonicalItemId = itemRecord.canonical_id || itemId;

    // Calculate end date for active status
    const end_date = calculateEndDate(duration_days);
    const startTimestamp = new Date();

    const runUpdateWithFallback = async (sqlFragment, params) => {
      const variants = [
        {
          sql: `${sqlFragment} WHERE id = ? OR uuid_id = ?`,
          params: [...params, canonicalItemId, canonicalItemId],
          tolerate: ['ER_BAD_FIELD_ERROR']
        },
        {
          sql: `${sqlFragment} WHERE id = ?`,
          params: [...params, canonicalItemId],
          tolerate: []
        },
        {
          sql: `${sqlFragment} WHERE uuid_id = ?`,
          params: [...params, canonicalItemId],
          tolerate: ['ER_BAD_FIELD_ERROR']
        }
      ];

      for (const variant of variants) {
        try {
          const [result] = await connection.query(variant.sql, variant.params);
          if (result.affectedRows > 0) {
            return result;
          }
        } catch (error) {
          const tolerated = variant.tolerate || [];
          if (!tolerated.includes(error.code)) {
            throw error;
          }
        }
      }

      return null;
    };

    // Update item to active status and set end date
    const primaryUpdate = await runUpdateWithFallback(
      'UPDATE items SET status = ?, end_date = ?, updated_at = NOW()',
      ['active', end_date]
    );

    if (!primaryUpdate) {
      throw new Error('Failed to publish draft item: no rows updated');
    }

    // Note: Only updating columns that actually exist in the database
    // The database schema only has: id, uuid_id, title, description, category_id,
    // seller_id, seller_email, starting_bid, current_bid, buy_now_price, 
    // status, created_at, updated_at

    await connection.commit();

    res.json({
      message: 'Item published successfully',
      item_id: canonicalItemId,
      end_date: end_date
    });

  } catch (err) {
    await connection.rollback();
    console.error('Item publish error:', err);
    res.status(500).json({ error: 'Failed to publish item' });
  } finally {
    connection.release();
  }
});
 
// Buy Now endpoint - completes purchase immediately using BuyNow procedure
router.post('/:id/buy-now', authenticateToken, async (req, res) => {
  const correlationId = req.headers['x-correlation-id'] || require('crypto').randomUUID();
  
  console.log('=== BUY NOW REQUEST RECEIVED ===');
  console.log('Correlation ID:', correlationId);
  console.log('Route: POST /api/items/:id/buy-now');
  console.log('Headers:', req.headers);
  console.log('Request body:', req.body);
  console.log('User from JWT:', req.user);

  const connection = await pool.getConnection();

  try {
    await connection.query('SET SESSION wait_timeout = 30');

    const normalizedItemId = String(req.params.id ?? '').trim();
    const buyerId = req.user.id;
    const requestedAmount =
      req.body?.amount !== undefined ? Number(req.body.amount) : NaN;

    console.log('Buy-now request details:', {
      correlationId,
      route: 'POST /api/items/:id/buy-now',
      normalizedItemId,
      buyerId,
      requestedAmount
    });

    // Log database connection info
    const [dbInfo] = await connection.query('SELECT DATABASE() AS db, @@hostname AS host');
    console.log('Database Info:', dbInfo[0]);

    if (!normalizedItemId) {
      return res.status(400).json({
        error: 'invalid_purchase',
        details: 'missing_item_id',
        message: 'Item ID is required',
        correlationId
      });
    }

    // Use flexible item resolver to handle multiple ID formats
    const { found, item, error: lookupError } = await getItemWithErrorInfo(
      connection,
      normalizedItemId
    );

    console.log('Buy-now item lookup result:', {
      correlationId,
      route: 'POST /api/items/:id/buy-now',
      itemId: normalizedItemId,
      found: found,
      lookupError: lookupError?.type
    });

    if (!found || !item) {
      console.log('Buy-now lookup failed', {
        correlationId,
        normalizedItemId,
        lookupError
      });
      return res
        .status(lookupError?.http_status || 404)
        .json({
          ...(lookupError?.json || {
            error: 'item_not_found',
            message: 'Item not found'
          }),
          request_item_id: normalizedItemId,
          correlationId
        });
    }

    const { valid, error: validationError } = validateItemForBuyNow(
      item,
      buyerId
    );

    console.log('Buy-now validation result:', {
      correlationId,
      valid,
      validationError: validationError?.json?.error
    });

    if (!valid) {
      console.log('Buy-now validation failed', {
        correlationId,
        normalizedItemId,
        validationError
      });
      return res
        .status(validationError.http_status)
        .json({
          ...validationError.json,
          correlationId
        });
    }

    const canonicalItemId = item.canonical_id || normalizedItemId;
    const numericItemId = item.id; // Use numeric INT ID for stored procedure
    
    // Enhanced logging for debugging ID issues
    console.log('Buy-now item ID analysis:', {
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
      console.log('Missing or invalid numeric item ID for buy-now', {
        correlationId,
        item_id: item.id,
        uuid_id: item.uuid_id
      });
      connection.release();
      return res.status(500).json({
        error: 'internal_error',
        details: 'invalid_item_id',
        message: 'Invalid item ID for purchase',
        correlationId
      });
    }

    const itemBuyNowPrice = Number(
      item.buy_now_price ?? item.buy_now_amount ?? item.buy_now
    );
    const purchaseAmount =
      Number.isFinite(requestedAmount) && requestedAmount > 0
        ? requestedAmount
        : itemBuyNowPrice;

    if (!Number.isFinite(purchaseAmount) || purchaseAmount <= 0) {
      return res.status(400).json({
        error: 'invalid_purchase',
        details: 'invalid_amount',
        message: 'A valid Buy Now amount is required',
        correlationId
      });
    }

    console.log('Buy-now proceeding with purchase:', {
      correlationId,
      canonicalItemId,
      numericItemId,
      buyerId,
      purchaseAmount
    });

    await connection.query('CALL BuyNow(?, ?, ?)', [
      numericItemId,  // Use numeric INT ID instead of UUID
      buyerId,
      purchaseAmount
    ]);

    return res.json({
      message: 'Purchase completed successfully', 
      item_id: canonicalItemId,
      amount: purchaseAmount,
      correlationId
    });
  } catch (err) {
    console.error('Buy Now error:', err);
    console.error('Correlation ID:', correlationId);

    if (err.sqlMessage) {
      return res.status(400).json({
        error: 'purchase_failed',
        details: 'database_error',
        message: err.sqlMessage,
        correlationId
      });
    }

    return res.status(500).json({
      error: 'purchase_failed',
      details: 'internal_error',
      message: 'Failed to complete purchase',
      correlationId
    });
  } finally {
    if (connection) {
    connection.release();
    }
  }
});

module.exports = router;
