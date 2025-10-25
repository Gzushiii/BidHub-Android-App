const express = require('express');
const { pool } = require('../config/database');
const { authenticateToken, checkItemOwnership } = require('../middleware/auth');
const { createItemSchema, updateItemSchema, paginationSchema } = require('../validators/items');
const { calculateEndDate, canUpdateItem, canDeleteItem } = require('../utils/validators');

const router = express.Router();

// Get all items with filtering and pagination
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
      items, 
      count: items.length,
      total,
      limit: parseInt(limit),
      offset: parseInt(offset)
    });
  } catch (err) {
    console.error('Items fetch error:', err);
    res.status(500).json({ error: 'Failed to fetch items' });
  }
});

// Get item by ID
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const [items] = await pool.query(
      'SELECT * FROM v_active_items WHERE id = ?',
      [id]
    );

    if (items.length === 0) {
      return res.status(404).json({ error: 'Item not found' });
    }

    const item = items[0];

    // Get item images
    const [images] = await pool.query(
      'SELECT * FROM item_images WHERE item_id = ? ORDER BY display_order',
      [id]
    );

    // Get seller information
    const [sellers] = await pool.query(
      'SELECT id, username, alias, first_name, last_name, created_at FROM users WHERE id = ?',
      [item.seller_id]
    );

    // Get recent bids
    const [bids] = await pool.query(
      `SELECT b.*, u.alias as bidder_alias 
       FROM bids b 
       JOIN users u ON b.bidder_id = u.id 
       WHERE b.item_id = ? 
       ORDER BY b.placed_at DESC 
       LIMIT 10`,
      [id]
    );

    res.json({
      ...item,
      images,
      seller: sellers[0] || null,
      recent_bids: bids
    });
  } catch (err) {
    console.error('Item fetch error:', err);
    res.status(500).json({ error: 'Failed to fetch item' });
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
      images = []
    } = value;

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
      return res.status(401).json({ error: 'Unauthorized: seller id not resolved' });
    }
    const end_date = calculateEndDate(duration_days);

    // Create the item
    const [result] = await connection.query(
      `INSERT INTO items 
       (title, description, category_id, seller_id, starting_price, reserve_price, 
        current_price, end_date, status, created_at, updated_at) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'active', NOW(), NOW())`,
      [title, description, category_id, seller_id, starting_price, reserve_price, starting_price, end_date]
    );

    const itemId = result.insertId;

    // Add images if provided
    if (images.length > 0) {
      const imageValues = images.map((imageUrl, index) => 
        [itemId, imageUrl, index + 1]
      );

      await connection.query(
        `INSERT INTO item_images (item_id, image_url, display_order) VALUES ?`,
        [imageValues]
      );
    }

    await connection.commit();

    // Get the created item with details
    const [items] = await connection.query(
      'SELECT * FROM items WHERE id = ?',
      [itemId]
    );

    const [itemImages] = await connection.query(
      'SELECT * FROM item_images WHERE item_id = ? ORDER BY display_order',
      [itemId]
    );

    res.status(201).json({
      message: 'Item created successfully',
      item: {
        ...items[0],
        images: itemImages
      }
    });

  } catch (err) {
    await connection.rollback();
    console.error('Item creation error:', err);
    res.status(500).json({ error: 'Failed to create item' });
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
      updateFields.push('description = ?');
      updateValues.push(description);
    }
    if (category_id !== undefined) {
      updateFields.push('category_id = ?');
      updateValues.push(category_id);
    }

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No valid fields to update' });
    }

    updateFields.push('updated_at = NOW()');
    updateValues.push(itemId);

    // Update the item
    await connection.query(
      `UPDATE items SET ${updateFields.join(', ')} WHERE id = ?`,
      updateValues
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

module.exports = router;
 
// Buy Now endpoint - completes purchase immediately using BuyNow procedure
router.post('/:id/buy-now', authenticateToken, async (req, res) => {
  console.log('=== BUY NOW REQUEST RECEIVED ===');
  console.log('Headers:', req.headers);
  console.log('Request body:', req.body);
  console.log('User from JWT:', req.user);
  
  const connection = await pool.getConnection();
  try {
    // Set connection timeout to prevent hanging
    await connection.query('SET SESSION wait_timeout = 30');
    const itemId = req.params.id; // Keep as string since it's a UUID
    const buyerId = req.user.id;
    const buyNowPrice = parseFloat(req.body?.amount);

    console.log('=== BUY NOW DEBUG ===');
    console.log('Item ID:', itemId);
    console.log('Buyer ID:', buyerId);
    console.log('Buy Now Price:', buyNowPrice);
    console.log('Request params:', req.params);
    console.log('Request body:', req.body);

    if (!itemId || !buyNowPrice || buyNowPrice <= 0) {
      console.log('Validation failed:', { itemId, buyNowPrice, hasAmount: !!req.body?.amount });
      return res.status(400).json({ 
        error: 'validation_failed', 
        details: 'invalid_purchase_data',
        message: 'Invalid purchase data - missing or invalid item ID or amount'
      });
    }

    // Get item details to validate buy now price
    console.log('=== ITEM LOOKUP DEBUG ===');
    console.log('Querying item with ID:', itemId);
    console.log('Item ID type:', typeof itemId);
    console.log('Item ID length:', itemId ? itemId.length : 'null');
    
    // First check what database we're connected to
    const [dbInfo] = await connection.query('SELECT DATABASE() as current_db');
    console.log('Current database:', dbInfo[0].current_db);
    
    // Check if items table exists
    const [tableCheck] = await connection.query('SHOW TABLES LIKE ?', ['items']);
    console.log('Items table exists:', tableCheck.length > 0);
    
    // Check items table structure
    const [columns] = await connection.query('DESCRIBE items');
    console.log('Items table columns:', columns.map(c => c.Field).join(', '));
    
    // Try the item query
    const [items] = await connection.query('SELECT * FROM items WHERE id = ?', [itemId]);
    console.log('Item query result:', items);
    console.log('Item query result length:', items.length);
    
    // Also try a broader search to see if the item exists with different casing or format
    const [allItems] = await connection.query('SELECT id, title, status FROM items LIMIT 5');
    console.log('Sample items in database:', allItems);
    
    if (items.length === 0) {
      console.log('Item not found for ID:', itemId);
      return res.status(404).json({ 
        error: 'item_not_found', 
        details: 'item_does_not_exist',
        message: 'Item not found'
      });
    }
    const item = items[0];
    console.log('Item found:', { id: item.id, status: item.status, seller_id: item.seller_id, buy_now_price: item.buy_now_price });

    if (item.status !== 'active') {
      console.log('Item not active:', item.status);
      return res.status(400).json({ 
        error: 'item_not_available', 
        details: 'item_not_active',
        message: 'Item is not available for purchase'
      });
    }

    if (item.seller_id === buyerId) {
      console.log('Buyer is seller:', { buyerId, sellerId: item.seller_id });
      return res.status(400).json({ 
        error: 'invalid_purchase', 
        details: 'cannot_buy_own_item',
        message: 'Cannot buy your own item'
      });
    }

    // Use the BuyNow stored procedure for proper credit handling
    console.log('Calling BuyNow procedure with:', { itemId, buyerId, buyNowPrice });
    try {
      await connection.query('CALL BuyNow(?, ?, ?)', [itemId, buyerId, buyNowPrice]);
      console.log('BuyNow procedure completed successfully');
    } catch (procError) {
      console.error('BuyNow procedure error:', procError);
      console.error('Error details:', {
        message: procError.message,
        sqlMessage: procError.sqlMessage,
        code: procError.code,
        errno: procError.errno
      });
      
      if (procError.sqlMessage) {
        return res.status(400).json({ 
          error: 'purchase_failed', 
          details: 'stored_procedure_error',
          message: procError.sqlMessage
        });
      } else {
        return res.status(500).json({ 
          error: 'purchase_failed', 
          details: 'internal_error',
          message: 'Failed to process buy now'
        });
      }
    }

    res.json({ 
      message: 'Purchase completed successfully', 
      item_id: itemId, 
      amount: buyNowPrice 
    });
  } catch (err) {
    console.error('Buy Now error:', err);
    console.error('Error stack:', err.stack);
    console.error('Error details:', {
      message: err.message,
      sqlMessage: err.sqlMessage,
      code: err.code,
      errno: err.errno
    });
    
    if (err.sqlMessage) {
      res.status(400).json({ 
        error: 'purchase_failed', 
        details: 'database_error',
        message: err.sqlMessage
      });
    } else if (err.message) {
      res.status(500).json({ 
        error: 'purchase_failed', 
        details: 'internal_error',
        message: 'Failed to complete purchase: ' + err.message
      });
    } else {
      res.status(500).json({ 
        error: 'purchase_failed', 
        details: 'unknown_error',
        message: 'Failed to complete purchase'
      });
    }
  } finally {
    if (connection) {
      connection.release();
    }
  }
});
