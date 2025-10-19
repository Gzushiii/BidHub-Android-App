const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { registerValidator } = require('../../src/validators/auth');
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

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const { error } = registerValidator.validate(req.body);
    if (error) {
      return res.status(400).json({ error: error.details[0].message });
    }

    const { username, email, phone_number, password, first_name, last_name, alias } = req.body;

    // Check if user exists
    const [existing] = await db.query(
      'SELECT id FROM users WHERE email = ? OR username = ? OR alias = ?',
      [email, username, alias]
    );

    if (existing.length > 0) {
      return res.status(409).json({ error: 'User already exists' });
    }

    // Hash password
    const salt = await bcrypt.genSalt(10);
    const password_hash = await bcrypt.hash(password, salt);

    // Insert user
    const [result] = await db.query(
      `INSERT INTO users (username, email, phone_number, password_hash, salt, 
       first_name, last_name, alias, credits) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, 100.00)`,
      [username, email, phone_number, password_hash, salt, first_name, last_name, alias]
    );

    // Generate JWT token
    const token = jwt.sign(
      { 
        id: result.insertId, 
        email, 
        username, 
        alias 
      },
      process.env.JWT_SECRET,
      { expiresIn: '7d' }
    );

    res.status(201).json({
      message: 'User registered successfully',
      token,
      user: {
        id: result.insertId,
        username,
        email,
        first_name,
        last_name,
        alias,
        credits: 100.00
      }
    });
  } catch (err) {
    console.error('Registration error:', err);
    res.status(500).json({ error: 'Registration failed' });
  }
};
