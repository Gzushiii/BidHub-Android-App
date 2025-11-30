# Files Marked for Removal

These files are redundant and will be removed. Their functionality has been merged into comprehensive files.

## Redundant Check/Diagnostic Files (merged into comprehensive_database_diagnostic.sql)
- check_actual_schema.sql
- check_bids_table.sql
- check_credit_recording.sql
- check_database_functionality.sql
- check_database_status.sql
- check_existing_items.sql
- check_item_posting_issues.sql
- check_items_persistence.sql
- check_registered_users.sql
- check_triggers_and_constraints.sql
- check_user_password.sql
- check_users.sql
- check_users_table_structure.sql
- check_uuid_status.sql
- check_views.sql
- analyze_missing_database_components.sql
- debug_insufficient_credits.sql
- debug_placebid_procedure.sql
- debug_user_credits.sql

## Redundant Fix Files (functionality in fix_credit_system_comprehensive.sql or fix_api_schema_compatibility.sql)
- fix_all_items_status.sql
- fix_all_missing_components.sql
- fix_all_procedure_issues.sql
- fix_bidding_system_proper_flow.sql
- fix_bidding_validation_logic.sql
- fix_buy_now_and_bidding_errors.sql
- fix_credit_balance_sync_issue.sql
- fix_item_posting_database.sql
- fix_remaining_database_errors.sql
- apply_posting_flow_fixes.sql

## Redundant Test Files (can be merged into one test suite)
- test_bid_buynow_fixes.sql
- test_credit_system_fixes.sql
- test_credit_validation.sql
- test_database_connection.sql
- test_item_persistence.sql
- test_placebid_procedure.sql

## Duplicate Schema Files
- bidhub_schema_step1_cleanup.sql (use bidhub_schema.sql)
- bidhub_schema_step2_database.sql (use bidhub_schema.sql)
- bidhub_schema_step3_minimal.sql (use bidhub_schema.sql)
- bidhub_schema_step5_data_final.sql (sample data, not needed)
- complete_database_recreation.sql (redundant with bidhub_schema.sql)

## Obsolete/One-time Files
- create_test_items.sql (test data)
- insert_sample_data.sql (sample data)
- migrate_to_uuid_ids.sql (one-time migration, archive if needed)
- remove_location_column.sql (one-time fix, archive if needed)
- optimize_auth_performance.sql (specific optimization)
- safe_database_queries.sql (simple utility)
- verify_latest_user.sql (simple query)
- verify_production_schema.sql (redundant)
- verify_render_db_schema.sql (redundant)
- cleanup_all_users.sql (dangerous, should be removed)
- cleanup_specific_user.sql (dangerous, should be removed)

## Files to KEEP
- README.md
- bidhub_schema.sql (main schema)
- add_manual_topup_tables.sql (top-up support)
- migrate_fix_generated_ref_size.sql (recent migration)
- comprehensive_database_diagnostic.sql (comprehensive diagnostic)
- fix_credit_system_comprehensive.sql (comprehensive credit fix)
- fix_api_schema_compatibility.sql (API compatibility fixes)
- add_missing_columns.sql (utility)
- create_active_items_view.sql (view creation)
- create_bids_table.sql (table creation)
- run-schema.sh (setup script)
- setup_database.sh (setup script)

