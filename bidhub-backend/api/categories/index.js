const db = require('../../src/config/database');

module.exports = async (req, res) => {
  // Set CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

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
};
