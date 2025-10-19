const mysql = require('mysql2/promise');

// Connection pool for Render deployment
const createPool = () => {
  const config = {
    host: process.env.DB_HOST,
    port: parseInt(process.env.DB_PORT),
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0,
    acquireTimeout: 60000,
    timeout: 60000
  };

  console.log('Creating database connection to:', process.env.DB_HOST, process.env.DB_PORT);
  console.log('SSL enabled:', process.env.DB_SSL === 'true');
  return mysql.createPool(config);
};

module.exports = createPool();
