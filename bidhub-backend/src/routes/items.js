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
      'UPDATE items SET status = "cancelled", updated_at = NOW() WHERE id = ?',
      [itemId]
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
