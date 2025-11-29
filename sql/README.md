# SQL Files Directory

This directory contains all SQL scripts and database-related files for the BidHub project.

**Note:** This directory has been cleaned up to remove redundant files. See `REDUNDANT_FILES_TO_REMOVE.md` for details on what was removed.

## 📁 File Organization

### Database Schema Files
- `bidhub_schema_step1_cleanup.sql` - Cleanup script to remove existing tables
- `bidhub_schema_step2_database.sql` - Create the main database
- `bidhub_schema_step3_minimal.sql` - Create essential tables (users, items, categories, etc.)
- `bidhub_schema_step5_data_final.sql` - Insert sample data
- `complete_database_recreation.sql` - Complete database recreation with all tables, procedures, and sample data

### Database Setup Scripts
- `run-schema.sh` - Main script to run all schema files in order
- `setup_database.sh` - Database setup automation script

### Database Maintenance Files
- `add_missing_columns.sql` - Add missing columns to existing tables (updated_at, transaction_date, etc.)
- `add_manual_topup_tables.sql` - Add manual top-up support tables and procedures
- `fix_api_schema_compatibility.sql` - Comprehensive fix for API schema compatibility
- `fix_all_missing_components.sql` - Fix all missing database components
- `fix_all_procedure_issues.sql` - Fix all stored procedure issues (PlaceBid, BuyNow)
- `fix_all_items_status.sql` - Fix item status alignment issues
- `fix_bidding_system_proper_flow.sql` - Fix bidding system flow
- `fix_bidding_validation_logic.sql` - Fix bidding validation logic
- `fix_buy_now_and_bidding_errors.sql` - Fix buy now and bidding errors
- `fix_credit_system_comprehensive.sql` - Comprehensive credit system fixes
- `fix_credit_balance_sync_issue.sql` - Fix credit balance synchronization
- `fix_item_posting_database.sql` - Fix item posting database issues
- `fix_remaining_database_errors.sql` - Fix remaining database errors
- `remove_location_column.sql` - Remove location column from items table
- `safe_database_queries.sql` - Safe database operations

### Database Testing & Verification Files
- `check_actual_schema.sql` - Verify current database schema (comprehensive)
- `check_bids_table.sql` - Check bids table structure
- `check_credit_recording.sql` - Check credit recording functionality
- `check_database_functionality.sql` - Test database functionality
- `check_database_status.sql` - Check overall database status
- `check_existing_items.sql` - Check existing items in database
- `check_item_posting_issues.sql` - Check item posting issues
- `check_items_persistence.sql` - Test item data persistence
- `check_registered_users.sql` - Check registered users
- `check_triggers_and_constraints.sql` - Check triggers and constraints
- `check_user_password.sql` - Check user password information
- `check_users.sql` - Check users in database
- `check_users_table_structure.sql` - Check users table structure
- `check_uuid_status.sql` - Check UUID status
- `check_views.sql` - Check database views
- `test_database_connection.sql` - Test database connectivity
- `test_item_persistence.sql` - Test item persistence functionality
- `test_placebid_procedure.sql` - Test PlaceBid procedure
- `verify_latest_user.sql` - Verify latest user registration
- `verify_production_schema.sql` - Verify production schema
- `verify_render_db_schema.sql` - Verify Render database schema

### Diagnostic & Analysis Files
- `analyze_missing_database_components.sql` - Analyze missing database components
- `comprehensive_database_diagnostic.sql` - Comprehensive database diagnostic (most complete)
- `debug_insufficient_credits.sql` - Debug insufficient credits issues
- `debug_placebid_procedure.sql` - Debug PlaceBid procedure
- `debug_user_credits.sql` - Debug user credits

### Database Views & Procedures
- `create_active_items_view.sql` - Create view for active items
- `create_bids_table.sql` - Create bids table
- `create_test_items.sql` - Create test items for testing

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
2. Verify schema with `check_actual_schema.sql` or `verify_production_schema.sql`
3. Run comprehensive diagnostic with `comprehensive_database_diagnostic.sql`
4. Test data persistence with `test_item_persistence.sql`
5. Check for missing components with `analyze_missing_database_components.sql`

## 📋 Cleanup Summary

This directory has been cleaned up to remove ~60+ redundant files. The remaining files are:
- **Essential schema files** for database setup
- **Comprehensive fix scripts** that consolidate multiple smaller fixes
- **Most useful diagnostic/check scripts** for troubleshooting
- **Production-ready procedures and views**

For details on what was removed, see `REDUNDANT_FILES_TO_REMOVE.md`.
