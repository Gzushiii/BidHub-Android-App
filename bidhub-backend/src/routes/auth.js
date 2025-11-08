const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const { registerValidator, loginValidator } = require('../validators/auth');
const { pool } = require('../config/database');

const router = express.Router();

// Performance: Use lower bcrypt rounds for faster hashing (8 rounds = ~4x faster than 10, still secure)
const BCRYPT_ROUNDS = parseInt(process.env.BCRYPT_ROUNDS) || 8;
const isDevelopment = process.env.NODE_ENV !== 'production';

// Register endpoint - OPTIMIZED
router.post('/register', async (req, res) => {
  try {
    if (isDevelopment) {
      console.log('Registration attempt:', { email: req.body.email, username: req.body.username });
    }
    
    const { error } = registerValidator.validate(req.body);
    if (error) {
      return res.status(400).json({ error: error.details[0].message });
    }

    const { username, email, phone_number, password, first_name, last_name, alias } = req.body;

    // OPTIMIZED: Check if user exists using UNION (faster than OR, uses indexes better)
    // This allows MySQL to use indexes on email, username, and alias separately
    const [existing] = await pool.query(
      `SELECT id FROM users WHERE email = ? 
       UNION 
       SELECT id FROM users WHERE username = ? 
       UNION 
       SELECT id FROM users WHERE alias = ? 
       LIMIT 1`,
      [email, username, alias]
    );

    if (existing.length > 0) {
      return res.status(409).json({ error: 'User already exists' });
    }

    // OPTIMIZED: Hash password with lower rounds (8 rounds = ~4x faster than 10, still secure)
    // bcrypt.hash automatically generates and includes salt in the hash
    const password_hash = await bcrypt.hash(password, BCRYPT_ROUNDS);
    // Generate simple salt for database column (bcrypt hash already contains salt, this is just for schema compatibility)
    const salt = crypto.randomBytes(16).toString('hex');

    // Insert user
    const [result] = await pool.query(
      `INSERT INTO users (username, email, phone_number, password_hash, salt, 
       first_name, last_name, alias, credits) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, 100.00)`,
      [username, email, phone_number, password_hash, salt, first_name, last_name, alias]
    );

    // Generate JWT token (synchronous, very fast)
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
    console.error('Registration error:', err.message);
    if (isDevelopment) {
      console.error('Error stack:', err.stack);
    }
    res.status(500).json({ 
      error: 'Registration failed',
      ...(isDevelopment && { details: err.message })
    });
  }
});

// Login endpoint - OPTIMIZED
router.post('/login', async (req, res) => {
  try {
    const { error } = loginValidator.validate(req.body);
    if (error) {
      return res.status(400).json({ error: error.details[0].message });
    }

    const { email, password } = req.body;

    // OPTIMIZED: Select only needed columns (faster query, less network overhead)
    // Excludes large fields like profile_picture that aren't needed for login
    const [users] = await pool.query(
      `SELECT id, email, username, alias, password_hash, first_name, last_name, credits, is_active
       FROM users WHERE email = ? LIMIT 1`,
      [email]
    );

    if (users.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const user = users[0];

    // Check if account is active
    if (user.is_active === false || user.is_active === 0) {
      return res.status(403).json({ error: 'Account is inactive' });
    }

    // Verify password (bcrypt.compare is already async/optimized)
    const isValidPassword = await bcrypt.compare(password, user.password_hash);
    if (!isValidPassword) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    // OPTIMIZED: Update last_login timestamp (non-blocking, don't wait for completion)
    pool.query(
      'UPDATE users SET last_login = NOW() WHERE id = ?',
      [user.id]
    ).catch(err => {
      // Log but don't block response
      if (isDevelopment) {
        console.error('Failed to update last_login:', err.message);
      }
    });

    // Generate JWT token (synchronous, very fast)
    const token = jwt.sign(
      { 
        id: user.id, 
        email: user.email, 
        username: user.username, 
        alias: user.alias 
      },
      process.env.JWT_SECRET,
      { expiresIn: '7d' }
    );

    res.json({
      message: 'Login successful',
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        first_name: user.first_name,
        last_name: user.last_name,
        alias: user.alias,
        credits: user.credits
      }
    });
  } catch (err) {
    console.error('Login error:', err.message);
    if (isDevelopment) {
      console.error('Error stack:', err.stack);
    }
    res.status(500).json({ error: 'Login failed' });
  }
});

module.exports = router;
