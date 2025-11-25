require('dotenv').config();
const db = require('./src/config/database');

async function testConnection() {
  try {
    console.log('Testing database connection...');
    
    // Test basic connection
    const connection = await db.getConnection();
    console.log('✅ Database connected successfully');
    
    // Test a simple query
    const [rows] = await connection.execute('SELECT COUNT(*) as count FROM users');
    console.log('✅ Query test successful:', rows[0]);
    
    // Test categories query
    const [categories] = await connection.execute('SELECT COUNT(*) as count FROM categories');
    console.log('✅ Categories query successful:', categories[0]);
    
    // Test items query
    const [items] = await connection.execute('SELECT COUNT(*) as count FROM items');
    console.log('✅ Items query successful:', items[0]);
    
    connection.release();
    console.log('✅ All tests passed!');
    
    process.exit(0);
  } catch (error) {
    console.error('❌ Database connection failed:', error.message);
    console.error('Full error:', error);
    process.exit(1);
  }
}

testConnection();
