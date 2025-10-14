const express = require('express');
const db = require('../config/database');

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

    query += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
    params.push(parseInt(limit), parseInt(offset));

    const [items] = await db.query(query, params);

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

    const [countResult] = await db.query(countQuery, countParams);
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

    const [items] = await db.query(
      'SELECT * FROM v_active_items WHERE id = ?',
      [id]
    );

    if (items.length === 0) {
      return res.status(404).json({ error: 'Item not found' });
    }

    const item = items[0];

    // Get item images
    const [images] = await db.query(
      'SELECT * FROM item_images WHERE item_id = ? ORDER BY display_order',
      [id]
    );

    // Get seller information
    const [sellers] = await db.query(
      'SELECT id, username, alias, first_name, last_name, created_at FROM users WHERE id = ?',
      [item.seller_id]
    );

    // Get recent bids
    const [bids] = await db.query(
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

module.exports = router;
