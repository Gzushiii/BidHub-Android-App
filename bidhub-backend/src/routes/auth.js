const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { registerValidator, loginValidator } = require('../validators/auth');
const { pool } = require('../config/database');

const router = express.Router();

// Register endpoint
router.post('/register', async (req, res) => {
  try {
    console.log('Registration attempt:', req.body);
    const { error } = registerValidator.validate(req.body);
    if (error) {
      console.log('Validation error:', error.details[0].message);
      return res.status(400).json({ error: error.details[0].message });
    }

    const { username, email, phone_number, password, first_name, last_name, alias } = req.body;

    // Check if user exists
    console.log('Checking if user exists...');
    const [existing] = await pool.query(
      'SELECT id FROM users WHERE email = ? OR username = ? OR alias = ?',
      [email, username, alias]
    );

    if (existing.length > 0) {
      console.log('User already exists');
      return res.status(409).json({ error: 'User already exists' });
    }

    // Hash password
    const salt = await bcrypt.genSalt(10);
    const password_hash = await bcrypt.hash(password, salt);

    // Insert user
    console.log('Inserting user into database...');
    const [result] = await pool.query(
      `INSERT INTO users (username, email, phone_number, password_hash, salt, 
       first_name, last_name, alias, credits) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, 100.00)`,
      [username, email, phone_number, password_hash, salt, first_name, last_name, alias]
    );
    console.log('User inserted successfully, ID:', result.insertId);

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
    console.error('Error details:', err.message);
    console.error('Error stack:', err.stack);
    res.status(500).json({ error: 'Registration failed', details: err.message });
  }
});

// Login endpoint
router.post('/login', async (req, res) => {
  try {
    const { error } = loginValidator.validate(req.body);
    if (error) {
      return res.status(400).json({ error: error.details[0].message });
    }

    const { email, password } = req.body;

    // Find user by email
    const [users] = await pool.query(
      'SELECT * FROM users WHERE email = ?',
      [email]
    );

    if (users.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const user = users[0];

    // Verify password
    const isValidPassword = await bcrypt.compare(password, user.password_hash);
    if (!isValidPassword) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    // Generate JWT token
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
    console.error('Login error:', err);
    res.status(500).json({ error: 'Login failed' });
  }
});

module.exports = router;
