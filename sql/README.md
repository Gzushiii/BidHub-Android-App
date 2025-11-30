# SQL Files Directory

This directory contains all SQL scripts and database-related files for the BidHub project.

**Note:** This directory has been cleaned up and consolidated. Redundant files have been removed. See `FILES_TO_REMOVE.md` for details.

## 📁 File Organization

### Core Schema Files
- **`bidhub_schema.sql`** - Main comprehensive database schema with all tables, procedures, and views
- **`add_manual_topup_tables.sql`** - Manual top-up support tables, procedures, and views (idempotent)
- **`migrate_fix_generated_ref_size.sql`** - Migration to fix `generated_ref` column size (VARCHAR(16) → VARCHAR(50))

### Database Diagnostics
- **`comprehensive_database_diagnostic.sql`** - Comprehensive diagnostic script that checks:
  - Schema verification (tables, columns, constraints)
  - Data integrity and balance verification
  - Bidding flow analysis
  - Buy Now flow analysis
  - Transaction isolation checks
  - Index analysis
  - Recent errors and patterns

### Database Fixes
- **`fix_credit_system_comprehensive.sql`** - Comprehensive fix for credit system issues:
  - PlaceBid procedure with proper locking and outbid refunds
  - BuyNow procedure with proper locking
  - EndAuction procedure
  - Idempotency support
  - Performance indices

- **`fix_api_schema_compatibility.sql`** - Fixes API schema compatibility:
  - Adds missing columns (uuid_id, starting_bid, reserve_price, end_date)
  - Creates item_images table
  - Creates credit_transactions table
  - Creates/updates v_active_items view
  - Updates stored procedures

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
# Option 1: Use the setup script
cd sql
./setup_database.sh

# Option 2: Manual setup
mysql -u username -p defaultdb < bidhub_schema.sql
mysql -u username -p defaultdb < add_manual_topup_tables.sql
```

### Adding Top-Up Support

If you need to add manual top-up support to an existing database:

```bash
mysql -u username -p defaultdb < add_manual_topup_tables.sql
```

### Running Migrations

For specific migrations:

```bash
# Fix generated_ref column size
mysql -u username -p defaultdb < migrate_fix_generated_ref_size.sql
```

### Database Diagnostics

To diagnose database issues:

```bash
mysql -u username -p defaultdb < comprehensive_database_diagnostic.sql > diagnostic_report.txt
```

### Applying Fixes

To fix database issues:

```bash
# Fix credit system issues
mysql -u username -p defaultdb < fix_credit_system_comprehensive.sql

# Fix API schema compatibility
mysql -u username -p defaultdb < fix_api_schema_compatibility.sql
```

## 📝 File Descriptions

### Schema Files

**bidhub_schema.sql**
- Complete database schema
- Creates all core tables (users, items, categories, bids, credit_transactions, etc.)
- Includes stored procedures (PlaceBid, BuyNow, EndAuction)
- Includes views (v_active_items, v_user_bids, v_credit_summary)
- Includes default categories

**add_manual_topup_tables.sql**
- Creates topups table for manual top-up requests
- Creates credit_ledger table for audit trail
- Creates supporting views (v_pending_topups, v_user_topup_stats, etc.)
- Creates stored procedures (sp_confirm_topup, sp_reject_topup)
- **Idempotent** - safe to run multiple times

**migrate_fix_generated_ref_size.sql**
- Fixes the `generated_ref` column in `topups` table
- Changes from VARCHAR(16) to VARCHAR(50) to accommodate 17-character reference codes
- Includes verification queries

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
