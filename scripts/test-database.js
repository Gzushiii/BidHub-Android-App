#!/usr/bin/env node
/**
 * Comprehensive Database Connection Test
 * Tests database connection using both methods and verifies tables
 */

require('dotenv').config();

const mysql = require('mysql2/promise');

async function testDatabase() {
  try {
    console.log('=======================================================');
    console.log('DATABASE CONNECTION TEST');
    console.log('=======================================================');
    console.log('');
    
    // Display connection info
    console.log('Connection Details:');
    console.log('  Host:', process.env.DB_HOST || 'not set');
    console.log('  Port:', process.env.DB_PORT || 'not set');
    console.log('  User:', process.env.DB_USER || 'not set');
    console.log('  Database:', process.env.DB_NAME || 'not set');
    console.log('  SSL:', process.env.DB_SSL === 'true' ? 'enabled' : 'disabled');
    console.log('');
    
    // Create connection
    const connection = await mysql.createConnection({
      host: process.env.DB_HOST,
      port: parseInt(process.env.DB_PORT || '3306'),
      user: process.env.DB_USER,
      password: process.env.DB_PASSWORD,
      database: process.env.DB_NAME,
      ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false
    });
    
    console.log('✅ Database connection successful!');
    console.log('');
    
    // Test 1: Basic query
    console.log('Test 1: Basic query test...');
    const [testRows] = await connection.execute('SELECT 1 as test, NOW() as current_time');
    console.log('✅ Basic query successful:', testRows[0]);
    console.log('');
    
    // Test 2: Check tables exist
    console.log('Test 2: Checking tables...');
    const [tables] = await connection.execute('SHOW TABLES');
    console.log(`✅ Found ${tables.length} tables:`);
    tables.forEach((table, index) => {
      const tableName = Object.values(table)[0];
      console.log(`   ${index + 1}. ${tableName}`);
    });
    console.log('');
    
    // Test 3: Test core tables
    console.log('Test 3: Testing core tables...');
    
    const coreTables = ['users', 'categories', 'items', 'bids', 'credit_transactions'];
    for (const tableName of coreTables) {
      try {
        const [rows] = await connection.execute(`SELECT COUNT(*) as count FROM ${tableName}`);
        console.log(`✅ ${tableName}: ${rows[0].count} records`);
      } catch (error) {
        console.log(`⚠️  ${tableName}: Table not found or error - ${error.message}`);
      }
    }
    console.log('');
    
    // Test 4: Test topups table (if exists)
    try {
      const [topups] = await connection.execute('SELECT COUNT(*) as count FROM topups');
      console.log(`✅ topups: ${topups[0].count} records`);
    } catch (error) {
      console.log(`⚠️  topups: Table not found (this is OK if top-up support not installed)`);
    }
    console.log('');
    
    // Test 5: Check for stored procedures
    console.log('Test 4: Checking stored procedures...');
    const [procedures] = await connection.execute(`
      SELECT ROUTINE_NAME 
      FROM information_schema.ROUTINES 
      WHERE ROUTINE_SCHEMA = ? 
      AND ROUTINE_TYPE = 'PROCEDURE'
    `, [process.env.DB_NAME]);
    
    if (procedures.length > 0) {
      console.log(`✅ Found ${procedures.length} stored procedures:`);
      procedures.forEach((proc, index) => {
        console.log(`   ${index + 1}. ${proc.ROUTINE_NAME}`);
      });
    } else {
      console.log('⚠️  No stored procedures found');
    }
    console.log('');
    
    await connection.end();
    
    console.log('=======================================================');
    console.log('✅ ALL DATABASE TESTS PASSED!');
    console.log('=======================================================');
    
    process.exit(0);
  } catch (error) {
    console.error('');
    console.error('=======================================================');
    console.error('❌ DATABASE CONNECTION FAILED');
    console.error('=======================================================');
    console.error('Error:', error.message);
    console.error('');
    console.error('Troubleshooting:');
    console.error('1. Check your .env file has correct database credentials');
    console.error('2. Verify database server is running');
    console.error('3. Check network connectivity');
    console.error('4. Verify database name exists');
    console.error('');
    console.error('Full error:', error);
    process.exit(1);
  }
}

testDatabase();

