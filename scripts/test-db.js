require('dotenv').config();
const db = require('./src/config/database');

async function testDatabase() {
  try {
    console.log('Testing database connection...');
    console.log('Host:', process.env.DB_HOST);
    console.log('Port:', process.env.DB_PORT);
    console.log('Database:', process.env.DB_NAME);
    console.log('User:', process.env.DB_USER);
    
    // Test basic connection
    const connection = await db.getConnection();
    console.log('✅ Database connected successfully');
    
    // Test a simple query
    const [rows] = await connection.execute('SELECT COUNT(*) as count FROM users');
    console.log('✅ Users table query successful:', rows[0]);
    
    // Test categories query
    const [categories] = await connection.execute('SELECT COUNT(*) as count FROM categories');
    console.log('✅ Categories table query successful:', categories[0]);
    
    // Test items query
    const [items] = await connection.execute('SELECT COUNT(*) as count FROM items');
    console.log('✅ Items table query successful:', items[0]);
    
    connection.release();
    console.log('✅ All database tests passed!');
    
    process.exit(0);
  } catch (error) {
    console.error('❌ Database connection failed:', error.message);
    console.error('Full error:', error);
    process.exit(1);
  }
}

testDatabase();
