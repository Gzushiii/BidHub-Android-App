# SQL Files Directory

This directory contains all SQL scripts and database-related files for the BidHub project.

**Note:** This directory has been cleaned up and consolidated. Redundant files have been removed. See `FILES_TO_REMOVE.md` for details.

## 📁 File Organization

### Core Schema Files
- **`bidhub_complete_schema.sql`** ⭐ **RECOMMENDED** - Complete consolidated schema file that includes:
  - All core tables (users, items, categories, bids, credit_transactions, etc.)
  - All API compatibility columns and fixes
  - All stored procedures (PlaceBid, BuyNow, EndAuction) with latest fixes including SQL mode compatibility
  - Manual top-up support tables and procedures
  - All views (v_active_items, v_user_bids, etc.)
  - All performance indices
  - Default categories
  - **Use this for fresh installations or complete database rebuilds**

### Legacy Schema Files (Kept for Reference)
- **`bidhub_schema.sql`** - Original base schema (now merged into bidhub_complete_schema.sql)
- **`add_manual_topup_tables.sql`** - Manual top-up support (now merged into bidhub_complete_schema.sql)
- **`migrate_fix_generated_ref_size.sql`** - Migration to fix `generated_ref` column size (now included in bidhub_complete_schema.sql)

### Database Diagnostics
- **`comprehensive_database_diagnostic.sql`** - Comprehensive diagnostic script that checks:
  - Schema verification (tables, columns, constraints)
  - Data integrity and balance verification
  - Bidding flow analysis
  - Buy Now flow analysis
  - Transaction isolation checks
  - Index analysis
  - Recent errors and patterns

### Database Fixes (Now Merged into bidhub_complete_schema.sql)
- **`fix_credit_system_comprehensive.sql`** - Comprehensive fix for credit system issues (now merged)
- **`fix_api_schema_compatibility.sql`** - Fixes API schema compatibility (now merged)
- **`fix_placebid_sql_mode_error.sql`** - Fixes PlaceBid SQL mode error (now merged)

### Utilities
- **`add_missing_columns.sql`** - Utility to add missing columns to existing tables
- **`create_active_items_view.sql`** - Creates the v_active_items view
- **`create_bids_table.sql`** - Creates the bids table if missing

### Setup Scripts
- **`run-schema.sh`** - Main script to run all schema files in order
- **`setup_database.sh`** - Database setup automation script

## 🚀 Usage

### Initial Database Setup

For a fresh database installation:

```bash
# RECOMMENDED: Use the complete consolidated schema
mysql -u username -p defaultdb < bidhub_complete_schema.sql

# OR use the setup script
cd sql
./setup_database.sh
```

**Note:** The `bidhub_complete_schema.sql` file includes everything needed for a complete installation:
- All tables with proper structure
- All stored procedures with latest fixes
- All views
- Manual top-up support
- Default categories
- All performance indices

### Adding Top-Up Support

If you need to add manual top-up support to an existing database:

```bash
# Option 1: Use the complete schema (idempotent, safe to run)
mysql -u username -p defaultdb < bidhub_complete_schema.sql

# Option 2: Use the standalone topup script
mysql -u username -p defaultdb < add_manual_topup_tables.sql
```

### Running Migrations

For specific migrations on existing databases:

```bash
# Fix generated_ref column size (if you have an old topups table)
mysql -u username -p defaultdb < migrate_fix_generated_ref_size.sql

# Fix credit system issues (if you have an old database)
mysql -u username -p defaultdb < fix_credit_system_comprehensive.sql

# Fix API schema compatibility (if you have an old database)
mysql -u username -p defaultdb < fix_api_schema_compatibility.sql

# Fix PlaceBid SQL mode error (if you have an old database)
mysql -u username -p defaultdb < fix_placebid_sql_mode_error.sql
```

**Note:** For fresh installations, use `bidhub_complete_schema.sql` which includes all fixes.

### Database Diagnostics

To diagnose database issues:

```bash
mysql -u username -p defaultdb < comprehensive_database_diagnostic.sql > diagnostic_report.txt
```

### Applying Fixes

To fix database issues on existing databases:

```bash
# Option 1: Use complete schema (recommended for major updates)
mysql -u username -p defaultdb < bidhub_complete_schema.sql

# Option 2: Apply individual fixes
mysql -u username -p defaultdb < fix_credit_system_comprehensive.sql
mysql -u username -p defaultdb < fix_api_schema_compatibility.sql
mysql -u username -p defaultdb < fix_placebid_sql_mode_error.sql
```

**Note:** The complete schema file is idempotent where possible, but always backup your database first.

## 📝 File Descriptions

### Schema Files

**bidhub_complete_schema.sql** ⭐ **RECOMMENDED**
- **Complete consolidated schema** that merges all setup and fix files
- Creates all core tables (users, items, categories, bids, credit_transactions, redemption_codes, topups, credit_ledger, item_images)
- Includes all stored procedures (PlaceBid, BuyNow, EndAuction, sp_confirm_topup, sp_reject_topup) with latest fixes
- Includes all views (v_active_items, v_user_bids, v_credit_summary, v_pending_topups, v_user_topup_stats)
- Includes all API compatibility columns (uuid_id, starting_bid, reserve_price, end_date, etc.)
- Includes all performance indices
- Includes default categories
- **Idempotent** - safe to run multiple times (uses IF NOT EXISTS where possible)
- **Use this for fresh installations or complete database rebuilds**

**bidhub_schema.sql** (Legacy - kept for reference)
- Original base schema
- Now merged into bidhub_complete_schema.sql

**add_manual_topup_tables.sql** (Legacy - kept for reference)
- Manual top-up support
- Now merged into bidhub_complete_schema.sql

**migrate_fix_generated_ref_size.sql** (Legacy - kept for reference)
- Migration to fix `generated_ref` column size
- Now included in bidhub_complete_schema.sql (topups table uses VARCHAR(50))

### Diagnostic Files

**comprehensive_database_diagnostic.sql**
- Most comprehensive diagnostic script
- Checks schema, data integrity, procedures, and performance
- Provides detailed analysis of credit system, bidding flow, and Buy Now flow
- Use this for troubleshooting any database issues

### Fix Files

**fix_credit_system_comprehensive.sql**
- Fixes all credit system issues
- Implements proper row-level locking
- Adds outbid refund logic
- Adds idempotency support
- Creates performance indices

**fix_api_schema_compatibility.sql**
- Ensures database schema matches API expectations
- Adds missing columns and tables
- Updates views and procedures
- Handles data migration

## 🔧 Troubleshooting

### Common Issues

1. **"Insufficient Credits" errors**
   - Run: `comprehensive_database_diagnostic.sql` to identify issues
   - Apply: `fix_credit_system_comprehensive.sql` to fix

2. **Schema mismatch errors**
   - Run: `comprehensive_database_diagnostic.sql` to check schema
   - Apply: `fix_api_schema_compatibility.sql` to fix

3. **Top-up reference code errors**
   - Apply: `migrate_fix_generated_ref_size.sql` to fix column size

### Diagnostic Workflow

1. **Check database status:**
   ```bash
   mysql -u username -p defaultdb < comprehensive_database_diagnostic.sql > report.txt
   ```

2. **Review the report** for:
   - Schema issues
   - Data integrity problems
   - Missing procedures
   - Balance mismatches

3. **Apply appropriate fixes:**
   - Credit issues → `fix_credit_system_comprehensive.sql`
   - Schema issues → `fix_api_schema_compatibility.sql`
   - Top-up issues → `migrate_fix_generated_ref_size.sql`

## 📋 Cleanup Summary

This directory has been cleaned up to remove **48 redundant files**. The remaining files are:

- **Essential schema files** for database setup
- **Comprehensive diagnostic script** that consolidates all check scripts
- **Comprehensive fix scripts** that consolidate multiple smaller fixes
- **Essential utilities** for common operations

For details on what was removed, see `FILES_TO_REMOVE.md`.

## ⚠️ Important Notes

- Always **backup your database** before running schema changes or fixes
- Test fixes on a development database first
- Some scripts require specific database permissions
- All scripts are designed for MySQL/MariaDB
- The `add_manual_topup_tables.sql` script is idempotent and safe to run multiple times

## 📚 Related Documentation

- Database documentation: `../docs/database/`
- API documentation: `../docs/api/`
- Development notes: `../docs/development/`
