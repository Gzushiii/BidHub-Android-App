#!/usr/bin/env node
/**
 * Generate Sample Data with Proper Bcrypt Hashes
 * This script generates a SQL file with properly hashed passwords
 */

// Run from bidhub-backend directory to have access to bcryptjs
const bcrypt = require('bcryptjs');
const fs = require('fs');
const path = require('path');

// Sample users with plain text passwords that will be hashed
const sampleUsers = [
    {
        username: 'alex_smith',
        email: 'alex.smith@example.com',
        phone_number: '+1234567890',
        password: 'password123',
        first_name: 'Alex',
        last_name: 'Smith',
        alias: 'AlexS',
        credits: 150.00
    },
    {
        username: 'jane_doe',
        email: 'jane.doe@example.com',
        phone_number: '+1234567891',
        password: 'password123',
        first_name: 'Jane',
        last_name: 'Doe',
        alias: 'JaneD',
        credits: 200.00
    },
    {
        username: 'bob_wilson',
        email: 'bob.wilson@example.com',
        phone_number: '+1234567892',
        password: 'password123',
        first_name: 'Bob',
        last_name: 'Wilson',
        alias: 'BobW',
        credits: 100.00
    },
    {
        username: 'test_user',
        email: 'test@example.com',
        phone_number: '+1234567893',
        password: 'test1234',
        first_name: 'Test',
        last_name: 'User',
        alias: 'TestUser',
        credits: 500.00
    }
];

async function generateHashes() {
    console.log('Generating bcrypt hashes for sample users...');
    
    const usersWithHashes = [];
    
    for (const user of sampleUsers) {
        // Hash the password with 8 rounds (matching backend config)
        const password_hash = await bcrypt.hash(user.password, 8);
        // Generate random salt (bcrypt has its own salt, but we keep this for schema compatibility)
        const salt = require('crypto').randomBytes(16).toString('hex');
        
        usersWithHashes.push({
            ...user,
            password_hash,
            salt
        });
        
        console.log(`✓ Hashed password for: ${user.email}`);
    }
    
    // Generate SQL file
    generateSQL(usersWithHashes);
}

function generateSQL(users) {
    const timestamp = new Date().toISOString();
    
    let sql = `-- =====================================================
-- SAMPLE DATA FOR BIDHUB APP
-- Generated: ${timestamp}
-- =====================================================
-- This script inserts sample data with properly hashed bcrypt passwords
-- Default password for all users: password123
-- Test user password: test1234
-- =====================================================

USE defaultdb;

-- =====================================================
-- INSERT CATEGORIES (if not exists)
-- =====================================================

-- Main categories
INSERT IGNORE INTO categories (name, description, parent_id, sort_order) VALUES
('Electronics', 'Electronic devices and accessories', NULL, 1),
('Fashion', 'Clothing, shoes, and accessories', NULL, 2),
('Home & Garden', 'Home improvement and garden items', NULL, 3),
('Sports & Outdoors', 'Sports equipment and outdoor gear', NULL, 4),
('Books & Media', 'Books, movies, music, and games', NULL, 5),
('Automotive', 'Car parts and automotive accessories', NULL, 6),
('Health & Beauty', 'Health and beauty products', NULL, 7),
('Toys & Games', 'Toys and gaming items', NULL, 8),
('Collectibles', 'Collectible items and memorabilia', NULL, 9),
('Others', 'Miscellaneous items that don\\'t fit specific categories', NULL, 10);

-- Electronics subcategories
INSERT IGNORE INTO categories (name, description, parent_id, sort_order) VALUES
('Smartphones', 'Mobile phones and accessories', 1, 1),
('Laptops', 'Laptop computers and accessories', 1, 2),
('Tablets', 'Tablet computers and accessories', 1, 3),
('Audio', 'Headphones, speakers, and audio equipment', 1, 4),
('Cameras', 'Cameras and photography equipment', 1, 5),
('Gaming', 'Gaming consoles and accessories', 1, 6);

-- Fashion subcategories
INSERT IGNORE INTO categories (name, description, parent_id, sort_order) VALUES
('Men\\'s Clothing', 'Men\\'s apparel and accessories', 2, 1),
('Women\\'s Clothing', 'Women\\'s apparel and accessories', 2, 2),
('Shoes', 'Footwear for men and women', 2, 3),
('Accessories', 'Bags, jewelry, and other accessories', 2, 4);

-- Home & Garden subcategories
INSERT IGNORE INTO categories (name, description, parent_id, sort_order) VALUES
('Furniture', 'Home furniture and decor', 3, 1),
('Kitchen', 'Kitchen appliances and tools', 3, 2),
('Garden', 'Garden tools and outdoor equipment', 3, 3),
('Tools', 'Hand tools and power tools', 3, 4);

-- =====================================================
-- INSERT SAMPLE USERS (with proper bcrypt hashes)
-- =====================================================

-- Delete existing test users first
DELETE FROM users WHERE email IN (
${users.map(u => `    '${u.email}'`).join(',\n')}
);

-- Insert users with proper password hashes
INSERT INTO users (username, email, phone_number, password_hash, salt, first_name, last_name, alias, credits, is_verified, is_active) VALUES
${users.map((user, index) => 
    `('${user.username}', '${user.email}', '${user.phone_number}', '${user.password_hash}', '${user.salt}', '${user.first_name}', '${user.last_name}', '${user.alias}', ${user.credits}, TRUE, TRUE)${index < users.length - 1 ? ',' : ';'}`
).join('\n')}

-- =====================================================
-- INSERT SAMPLE ITEMS
-- =====================================================

-- Get user IDs (assuming they were inserted in order)
SET @alex_id = (SELECT id FROM users WHERE email = 'alex.smith@example.com' LIMIT 1);
SET @jane_id = (SELECT id FROM users WHERE email = 'jane.doe@example.com' LIMIT 1);
SET @bob_id = (SELECT id FROM users WHERE email = 'bob.wilson@example.com' LIMIT 1);
SET @test_id = (SELECT id FROM users WHERE email = 'test@example.com' LIMIT 1);

-- Delete existing test items
DELETE FROM items WHERE seller_id IN (@alex_id, @jane_id, @bob_id, @test_id);

-- Insert sample items
INSERT INTO items (title, description, category_id, seller_id, starting_price, current_bid, buy_now_price, bid_deadline, billing_deadline, item_condition, status, location, created_at, updated_at) VALUES
('Vintage Nikon Camera', 'Beautiful vintage Nikon camera from the 1980s. Fully functional with original lens and case. Excellent condition.', 5, @alex_id, 150.00, 150.00, 250.00, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), 'good', 'active', 'New York, NY', NOW(), NOW()),
('Designer Leather Handbag', 'Luxury designer leather handbag from premium brand. Barely used, perfect condition. Includes dust bag and authenticity card.', 4, @jane_id, 800.00, 800.00, 1200.00, DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY), 'like_new', 'active', 'Los Angeles, CA', NOW(), NOW()),
('Modern Sectional Sofa', 'Comfortable modern sectional sofa in light gray fabric. Perfect for large living rooms. Includes throw pillows.', 11, @alex_id, 1200.00, 1200.00, 1500.00, DATE_ADD(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 11 DAY), 'good', 'active', 'Chicago, IL', NOW(), NOW()),
('Rare Coin Collection', 'Collection of rare silver coins from different eras. Includes Morgan dollars, Peace dollars, and other collectibles.', 9, @bob_id, 50.00, 50.00, 100.00, DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY), 'good', 'active', 'Miami, FL', NOW(), NOW()),
('Wireless Headphones', 'High-quality wireless noise-cancelling headphones. Brand new in box, never opened. Latest model with 30-hour battery life.', 4, @test_id, 200.00, 200.00, 300.00, DATE_ADD(NOW(), INTERVAL 14 DAY), DATE_ADD(NOW(), INTERVAL 15 DAY), 'new', 'active', 'San Francisco, CA', NOW(), NOW()),
('Gaming Laptop', 'Powerful gaming laptop with RTX graphics card. 16GB RAM, 1TB SSD, 15-inch display. Excellent for gaming and work.', 2, @alex_id, 1000.00, 1050.00, 1500.00, DATE_ADD(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 'like_new', 'active', 'Seattle, WA', NOW(), NOW()),
('Vintage Watch Collection', 'Collection of three vintage watches from the 1960s-1970s. All in working condition with leather straps.', 9, @jane_id, 300.00, 300.00, 500.00, DATE_ADD(NOW(), INTERVAL 8 DAY), DATE_ADD(NOW(), INTERVAL 9 DAY), 'good', 'active', 'Boston, MA', NOW(), NOW()),
('Designer Sunglasses', 'Luxury designer sunglasses with UV protection. Brand new with original case and cleaning cloth.', 4, @bob_id, 100.00, 100.00, 200.00, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), 'new', 'active', 'Austin, TX', NOW(), NOW());

-- =====================================================
-- VERIFY DATA INSERTED
-- =====================================================

SELECT '=================================================' as message
UNION ALL
SELECT 'Sample Data Inserted Successfully!'
UNION ALL
SELECT '=================================================';

SELECT 'Categories' as table_name, COUNT(*) as record_count FROM categories
UNION ALL
SELECT 'Users', COUNT(*) FROM users
UNION ALL
SELECT 'Items', COUNT(*) FROM items;

-- Show users with their credentials
SELECT 
    'User Credentials' as info
UNION ALL
SELECT '=================================================='
UNION ALL
SELECT CONCAT('Email: ', email, ' | Password: ', 
    CASE 
        WHEN email = 'test@example.com' THEN 'test1234'
        ELSE 'password123'
    END) as credentials
FROM users 
ORDER BY id;

-- Show sample items
SELECT 
    id,
    title,
    CONCAT('$', FORMAT(starting_price, 2)) as starting_price,
    item_condition,
    status,
    location
FROM items 
ORDER BY created_at DESC
LIMIT 10;

SELECT '=================================================' as message
UNION ALL
SELECT 'Sample data insertion complete!'
UNION ALL
SELECT 'You can now test the app with these accounts.'
UNION ALL
SELECT '=================================================';
`;

    // Write to file (go up one directory if running from bidhub-backend)
    const outputPath = path.join(__dirname, '..', 'sql', 'insert_sample_data.sql');
    fs.writeFileSync(outputPath, sql);
    console.log('\n✓ SQL file generated: sql/insert_sample_data.sql');
    console.log('\nSample data ready to insert!');
    console.log('\nUser credentials:');
    console.log('  - All users (except test@example.com): password123');
    console.log('  - test@example.com: test1234');
    console.log('\nRun this SQL file in your database to populate sample data.');
}

// Run the script
generateHashes().catch(err => {
    console.error('Error generating sample data:', err);
    process.exit(1);
});

