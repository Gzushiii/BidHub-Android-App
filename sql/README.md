# SQL Files Directory

This directory contains all SQL scripts and database-related files for the BidHub project.

## 📁 File Organization

### Database Schema Files
- `bidhub_schema_step1_cleanup.sql` - Cleanup script to remove existing tables
- `bidhub_schema_step2_database.sql` - Create the main database
- `bidhub_schema_step3_minimal.sql` - Create essential tables (users, items, categories, etc.)
- `bidhub_schema_step5_data_final.sql` - Insert sample data

### Database Setup Scripts
- `run-schema.sh` - Main script to run all schema files in order
- `setup_database.sh` - Database setup automation script

### Database Maintenance Files
- `add_missing_columns.sql` - Add missing columns to existing tables
- `fix_items_table.sql` - Fix issues with items table structure
- `remove_location_column.sql` - Remove location column from items table
- `safe_database_queries.sql` - Safe database operations

### Database Testing & Verification Files
- `check_database_status.sql` - Check overall database status
- `check_database_functionality.sql` - Test database functionality
- `check_actual_schema.sql` - Verify current database schema
- `check_items_columns.sql` - Check items table structure
- `check_items_persistence.sql` - Test item data persistence
- `check_users_simple.sql` - Simple user verification queries
- `test_database_connection.sql` - Test database connectivity
- `test_item_persistence.sql` - Test item persistence functionality
- `verify_latest_user.sql` - Verify latest user registration

### Database Views & Queries
- `create_active_items_view.sql` - Create view for active items
- `fixed_database_queries.sql` - Fixed and corrected database queries

## 🚀 Usage

### Quick Setup
```bash
cd sql
./run-schema.sh
```

### Manual Setup
1. Run cleanup: `mysql < bidhub_schema_step1_cleanup.sql`
2. Create database: `mysql < bidhub_schema_step2_database.sql`
3. Create tables: `mysql < bidhub_schema_step3_minimal.sql`
4. Insert data: `mysql < bidhub_schema_step5_data_final.sql`

### Testing
```bash
# Test database connection
mysql < test_database_connection.sql

# Check database status
mysql < check_database_status.sql

# Verify data
mysql < verify_latest_user.sql
```

## 📝 Notes

- All scripts are designed to work with MySQL/MariaDB
- Make sure to have proper database credentials configured
- Some scripts may require specific database permissions
- Always backup your database before running schema changes

## 🔧 Troubleshooting

If you encounter issues:
1. Check database connection with `test_database_connection.sql`
2. Verify schema with `check_actual_schema.sql`
3. Check for missing columns with `check_items_columns.sql`
4. Test data persistence with `test_item_persistence.sql`
