const express = require('express');
const db = require('../config/database');

const router = express.Router();

// Get all categories with subcategories
router.get('/', async (req, res) => {
  try {
    const [categories] = await db.query(
      `SELECT c.*, 
              COUNT(sc.id) as subcategory_count
       FROM categories c
       LEFT JOIN categories sc ON c.id = sc.parent_id
       WHERE c.parent_id IS NULL
       GROUP BY c.id
       ORDER BY c.name`
    );

    // Get subcategories for each category
    for (let category of categories) {
      const [subcategories] = await db.query(
        'SELECT * FROM categories WHERE parent_id = ? ORDER BY name',
        [category.id]
      );
      category.subcategories = subcategories;
    }

    res.json({ categories });
  } catch (err) {
    console.error('Categories fetch error:', err);
    res.status(500).json({ error: 'Failed to fetch categories' });
  }
});

// Get category by ID
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const [categories] = await db.query(
      'SELECT * FROM categories WHERE id = ?',
      [id]
    );

    if (categories.length === 0) {
      return res.status(404).json({ error: 'Category not found' });
    }

    const category = categories[0];

    // Get subcategories if it's a parent category
    if (!category.parent_id) {
      const [subcategories] = await db.query(
        'SELECT * FROM categories WHERE parent_id = ? ORDER BY name',
        [id]
      );
      category.subcategories = subcategories;
    }

    res.json({ category });
  } catch (err) {
    console.error('Category fetch error:', err);
    res.status(500).json({ error: 'Failed to fetch category' });
  }
});

module.exports = router;
