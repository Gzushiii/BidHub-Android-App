# MySQL Workbench Setup Guide for BidHub Database

This guide provides step-by-step instructions for setting up the BidHub database using MySQL Workbench.

## Prerequisites

- MySQL Server 8.0 or higher installed
- MySQL Workbench installed
- Administrative access to MySQL server

## Step 1: Install MySQL Server (if not already installed)

### Windows:
1. Download MySQL Installer from https://dev.mysql.com/downloads/installer/
2. Run the installer and select "MySQL Server" and "MySQL Workbench"
3. Follow the installation wizard
4. Set a root password during installation

### macOS:
```bash
# Using Homebrew
brew install mysql
brew services start mysql

# Set root password
mysql_secure_installation
```

### Linux (Ubuntu/Debian):
```bash
# Update package list
sudo apt update

# Install MySQL Server
sudo apt install mysql-server

# Start MySQL service
sudo systemctl start mysql
sudo systemctl enable mysql

# Secure installation
sudo mysql_secure_installation
```

## Step 2: Open MySQL Workbench

1. Launch MySQL Workbench
2. You should see your local MySQL connection in the main screen
3. Click on the connection to open it

## Step 3: Connect to MySQL Server

1. **If this is your first time connecting:**
   - Click the "+" button next to "MySQL Connections"
   - Enter connection details:
     - **Connection Name**: `BidHub Local` (or any name you prefer)
     - **Hostname**: `localhost` (or `127.0.0.1`)
     - **Port**: `3306` (default MySQL port)
     - **Username**: `root` (or your MySQL username)
     - **Password**: Click "Store in Vault..." and enter your MySQL password
   - Click "Test Connection" to verify
   - Click "OK" to save the connection

2. **Connect to the server:**
   - Double-click on your connection
   - Enter your password if prompted
   - You should now see the MySQL Workbench interface

## Step 4: Import the Database Schema

### Method 1: Using SQL Editor (Recommended)

1. **Open the SQL file:**
   - Go to `File` → `Open SQL Script...`
   - Navigate to the `bidhub_database_schema.sql` file
   - Click "Open"

2. **Execute the script:**
   - The SQL script will open in the SQL Editor
   - Click the "Execute" button (⚡ lightning bolt icon) or press `Ctrl+Shift+Enter`
   - Wait for the script to complete (should take a few seconds)

3. **Verify the database was created:**
   - In the Navigator panel on the left, click the refresh button
   - You should see `bidhub_db` in the SCHEMAS section

### Method 2: Using Import Wizard

1. **Open Import Wizard:**
   - Go to `Server` → `Data Import`
   - Select "Import from Self-Contained File"
   - Browse to the `bidhub_database_schema.sql` file
   - Select "New" under "Default Target Schema" and enter `bidhub_db`
   - Click "Start Import"

## Step 5: Verify Database Setup

1. **Check tables were created:**
   - In the Navigator panel, expand `bidhub_db` → `Tables`
   - You should see all 12 tables:
     - `audit_logs`
     - `bids`
     - `categories`
     - `credit_transactions`
     - `item_images`
     - `items`
     - `notifications`
     - `redemption_codes`
     - `user_sessions`
     - `users`
     - `watchlist`

2. **Check views were created:**
   - Expand `bidhub_db` → `Views`
   - You should see 3 views:
     - `v_active_items`
     - `v_user_bids`
     - `v_user_credits`

3. **Check stored procedures:**
   - Expand `bidhub_db` → `Stored Procedures`
   - You should see 2 procedures:
     - `EndAuction`
     - `PlaceBid`

4. **Check default data:**
   - Right-click on `categories` table → `Select Rows - Limit 1000`
   - You should see 20+ categories including main categories and subcategories

## Step 6: Test the Database

### Test 1: Insert a Test User

```sql
-- Switch to the bidhub_db database
USE bidhub_db;

-- Insert a test user
INSERT INTO users (username, email, phone_number, password_hash, salt, first_name, last_name, alias)
VALUES ('testuser', 'test@example.com', '+1234567890', 'hashed_password', 'salt_value', 'Test', 'User', 'testuser123');

-- Verify the user was inserted
SELECT * FROM users WHERE username = 'testuser';
```

### Test 2: Test the PlaceBid Procedure

```sql
-- First, insert a test item
INSERT INTO items (title, description, seller_id, starting_bid, bid_deadline, billing_deadline, condition, status)
VALUES ('Test Item', 'A test item for bidding', 1, 10.00, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), 'good', 'active');

-- Test the PlaceBid procedure
CALL PlaceBid(1, 1, 15.00, 'testuser123');

-- Check if the bid was placed
SELECT * FROM bids WHERE item_id = 1;
SELECT * FROM items WHERE id = 1;
```

### Test 3: Test Views

```sql
-- Test the active items view
SELECT * FROM v_active_items;

-- Test the user bids view
SELECT * FROM v_user_bids;

-- Test the user credits view
SELECT * FROM v_user_credits;
```

## Step 7: Configure for Production (Optional)

### Create Application User

```sql
-- Create a dedicated user for the application
CREATE USER 'bidhub_app'@'localhost' IDENTIFIED BY 'your_secure_password_here';

-- Grant necessary permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON bidhub_db.* TO 'bidhub_app'@'localhost';

-- Grant execute permissions for stored procedures
GRANT EXECUTE ON bidhub_db.* TO 'bidhub_app'@'localhost';

-- Flush privileges
FLUSH PRIVILEGES;
```

### Configure Remote Access (if needed)

```sql
-- Allow remote connections (adjust IP range as needed)
CREATE USER 'bidhub_app'@'%' IDENTIFIED BY 'your_secure_password_here';
GRANT SELECT, INSERT, UPDATE, DELETE ON bidhub_db.* TO 'bidhub_app'@'%';
GRANT EXECUTE ON bidhub_db.* TO 'bidhub_app'@'%';
FLUSH PRIVILEGES;
```

## Step 8: Backup and Maintenance

### Create a Backup

1. **Using MySQL Workbench:**
   - Go to `Server` → `Data Export`
   - Select `bidhub_db` schema
   - Choose export options
   - Click "Start Export"

2. **Using Command Line:**
   ```bash
   mysqldump -u root -p bidhub_db > bidhub_backup.sql
   ```

### Regular Maintenance

```sql
-- Optimize tables
OPTIMIZE TABLE users, items, bids, credit_transactions;

-- Check table status
SHOW TABLE STATUS FROM bidhub_db;

-- Analyze tables for better performance
ANALYZE TABLE users, items, bids, credit_transactions;
```

## Troubleshooting

### Common Issues and Solutions

1. **"Access denied for user" error:**
   - Check username and password
   - Ensure MySQL service is running
   - Verify user has proper permissions

2. **"Database already exists" error:**
   - The database already exists, which is fine
   - You can either drop it first or continue with existing data

3. **"Table already exists" error:**
   - Some tables already exist
   - You can drop the database and recreate it:
     ```sql
     DROP DATABASE IF EXISTS bidhub_db;
     -- Then re-run the schema script
     ```

4. **Connection timeout:**
   - Check if MySQL service is running
   - Verify firewall settings
   - Check if port 3306 is accessible

5. **Syntax errors in SQL:**
   - Ensure you're using MySQL 8.0 or higher
   - Check for any modifications made to the SQL file
   - Verify character encoding (should be UTF-8)

### Performance Optimization

1. **Enable query cache:**
   ```sql
   SET GLOBAL query_cache_size = 268435456;
   SET GLOBAL query_cache_type = ON;
   ```

2. **Configure InnoDB settings:**
   ```sql
   SET GLOBAL innodb_buffer_pool_size = 1073741824; -- 1GB
   ```

3. **Monitor performance:**
   ```sql
   SHOW PROCESSLIST;
   SHOW STATUS LIKE 'Slow_queries';
   ```

## Next Steps

1. **Connect your Android app** to this database using a backend API
2. **Set up regular backups** of the database
3. **Monitor database performance** and optimize as needed
4. **Consider setting up replication** for high availability
5. **Implement proper security measures** for production use

## Database Schema Overview

The BidHub database includes:

- **12 Tables**: Users, Items, Bids, Categories, Credit Transactions, etc.
- **3 Views**: Pre-built queries for common operations
- **2 Stored Procedures**: PlaceBid and EndAuction
- **Multiple Triggers**: For data consistency and audit logging
- **Comprehensive Indexes**: For optimal query performance
- **Default Data**: Sample categories and initial data

This schema is designed to handle all the features of the BidHub auction platform while maintaining data integrity and performance.
