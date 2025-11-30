# SQL Files Consolidation Plan

## Files to KEEP (Essential)

### Schema & Setup
- `bidhub_schema.sql` - Main comprehensive schema
- `add_manual_topup_tables.sql` - Top-up support (recently fixed)
- `migrate_fix_generated_ref_size.sql` - Migration we just created

### Comprehensive Files (Already Good)
- `comprehensive_database_diagnostic.sql` - Most comprehensive diagnostic
- `fix_credit_system_comprehensive.sql` - Most comprehensive fix

### Essential Utilities
- `add_missing_columns.sql` - Utility for adding columns
- `create_active_items_view.sql` - View creation
- `create_bids_table.sql` - Table creation utility

## Files to MERGE

### Diagnostic Files → Merge into comprehensive_database_diagnostic.sql
- `check_actual_schema.sql`
- `check_bids_table.sql`
- `check_credit_recording.sql`
- `check_database_functionality.sql`
- `check_database_status.sql`
- `check_existing_items.sql`
- `check_item_posting_issues.sql`
- `check_items_persistence.sql`
- `check_registered_users.sql`
- `check_triggers_and_constraints.sql`
- `check_user_password.sql`
- `check_users.sql`
- `check_users_table_structure.sql`
- `check_uuid_status.sql`
- `check_views.sql`
- `analyze_missing_database_components.sql`
- `debug_insufficient_credits.sql`
- `debug_placebid_procedure.sql`
- `debug_user_credits.sql`

### Fix Files → Merge into fix_all_database_issues.sql (new consolidated file)
- `fix_all_items_status.sql`
- `fix_all_missing_components.sql`
- `fix_all_procedure_issues.sql`
- `fix_api_schema_compatibility.sql`
- `fix_bidding_system_proper_flow.sql`
- `fix_bidding_validation_logic.sql`
- `fix_buy_now_and_bidding_errors.sql`
- `fix_credit_balance_sync_issue.sql`
- `fix_item_posting_database.sql`
- `fix_remaining_database_errors.sql`
- `apply_posting_flow_fixes.sql`

### Test Files → Merge into test_suite.sql (new consolidated file)
- `test_bid_buynow_fixes.sql`
- `test_credit_system_fixes.sql`
- `test_credit_validation.sql`
- `test_database_connection.sql`
- `test_item_persistence.sql`
- `test_placebid_procedure.sql`

## Files to REMOVE (Redundant/Obsolete)

### Duplicate Schema Files
- `bidhub_schema_step1_cleanup.sql` - Redundant with main schema
- `bidhub_schema_step2_database.sql` - Redundant with main schema
- `bidhub_schema_step3_minimal.sql` - Redundant with main schema
- `bidhub_schema_step5_data_final.sql` - Sample data, not needed in production
- `complete_database_recreation.sql` - Redundant with bidhub_schema.sql

### Obsolete Files
- `create_test_items.sql` - Test data, not needed
- `insert_sample_data.sql` - Sample data, not needed
- `migrate_to_uuid_ids.sql` - One-time migration, can archive
- `remove_location_column.sql` - One-time fix, can archive
- `optimize_auth_performance.sql` - Specific optimization, can archive
- `safe_database_queries.sql` - Utility, can merge or remove
- `verify_latest_user.sql` - Simple query, redundant
- `verify_production_schema.sql` - Redundant with comprehensive_database_diagnostic.sql
- `verify_render_db_schema.sql` - Redundant with comprehensive_database_diagnostic.sql
- `cleanup_all_users.sql` - Dangerous utility, should be removed or heavily documented
- `cleanup_specific_user.sql` - Dangerous utility, should be removed or heavily documented

## Final Structure

```
sql/
├── README.md
├── 00_CONSOLIDATION_PLAN.md (this file)
│
├── schema/
│   ├── bidhub_schema.sql (main schema)
│   ├── add_manual_topup_tables.sql
│   └── migrate_fix_generated_ref_size.sql
│
├── diagnostics/
│   └── comprehensive_database_diagnostic.sql (merged all check_*.sql)
│
├── fixes/
│   ├── fix_credit_system_comprehensive.sql
│   └── fix_all_database_issues.sql (new, merged all other fix_*.sql)
│
├── utilities/
│   ├── add_missing_columns.sql
│   ├── create_active_items_view.sql
│   └── create_bids_table.sql
│
└── tests/
    └── test_suite.sql (merged all test_*.sql)
```

