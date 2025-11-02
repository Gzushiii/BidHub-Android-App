const mysql = require('mysql2/promise');
require('dotenv').config();

// Validate required environment variables (avoid hardcoded secrets in code)
const requiredEnv = ['DB_HOST', 'DB_USER', 'DB_PASSWORD', 'DB_NAME'];
const missingEnv = requiredEnv.filter((key) => !process.env[key] || `${process.env[key]}`.trim() === '');

if (missingEnv.length > 0) {
  console.error('Missing required environment variables:', missingEnv.join(', '));
  console.error('Please set them in your Render environment and redeploy.');
  process.exit(1);
}

// Database configuration (env-driven) - OPTIMIZED for performance
const dbConfig = {
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT) || 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
  charset: 'utf8mb4',
  waitForConnections: true,
  connectionLimit: parseInt(process.env.DB_CONNECTION_LIMIT) || 20, // Increased from 10 to 20
  queueLimit: 0,
  // OPTIMIZED: Connection timeout settings
  acquireTimeout: 60000, // 60 seconds to acquire connection
  timeout: 60000, // 60 seconds query timeout
  // OPTIMIZED: Enable multiple statements for efficiency (safe in our use case)
  multipleStatements: false,
  // OPTIMIZED: Connection pool settings
  enableKeepAlive: true,
  keepAliveInitialDelay: 0
};

// Create connection pool
const pool = mysql.createPool(dbConfig);

// Test database connection
const testConnection = async () => {
  try {
    const connection = await pool.getConnection();
    console.log('✓ Database connected successfully');
    console.log(`M Connected to: ${dbConfig.database} on ${dbConfig.host}:${dbConfig.port}`);
    connection.release();
    return true;
  } catch (error) {
    console.error('X Database connection failed:', error.message);
    return false;
  }
};

// Initialize database connection
const initializeDatabase = async () => {
  const isConnected = await testConnection();
  if (!isConnected) {
    console.error('Failed to connect to database. Please check your configuration.');
    process.exit(1);
  }
};

module.exports = { pool, testConnection, initializeDatabase };
