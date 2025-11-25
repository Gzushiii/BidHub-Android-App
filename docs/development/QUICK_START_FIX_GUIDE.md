# Quick Start: Fix "Insufficient Credits" Error

## 🌐 Database: Aiven Cloud MySQL

This guide is configured for your **Aiven Cloud** MySQL database:
- **Host:** `bidhub-bidhub.b.aivencloud.com:27575`
- **Database:** `defaultdb`

## 🚀 One-Command Fix

```bash
cd /home/dane/Desktop/CC106-G5-BIDHUB/BidHub-Android-App
./apply_credit_system_fix.sh \
  --host bidhub-bidhub.b.aivencloud.com \
  --port 27575 \
  -u avnadmin \
  -p [YOUR_DATABASE_PASSWORD] \
  -d defaultdb
```

That's it! The script will:
- ✅ Automatically backup your database
- ✅ Run diagnostics before and after
- ✅ Apply all fixes safely
- ✅ Verify everything worked
- ✅ Generate a detailed report

---

## 📋 Prerequisites

Before running the script, make sure you have:

1. **MySQL client installed**
   ```bash
   # Check if installed
   mysql --version
   mysqldump --version
   ```

2. **Database credentials (Aiven Cloud)**
   - **Hostname:** `bidhub-bidhub.b.aivencloud.com`
   - **Port:** `27575`
   - **Username:** `avnadmin`
   - **Password:** `[YOUR_DATABASE_PASSWORD]`
   - **Database:** `defaultdb`

---

## 🎯 Running the Fix

### Option 1: With Aiven Credentials (Recommended)

```bash
./apply_credit_system_fix.sh \
  --host bidhub-bidhub.b.aivencloud.com \
  --port 27575 \
  -u avnadmin \
  -p [YOUR_DATABASE_PASSWORD] \
  -d defaultdb
```

### Option 2: Interactive Mode

```bash
./apply_credit_system_fix.sh
```

The script will:
1. Ask for your MySQL password (replace [YOUR_DATABASE_PASSWORD] below)
2. Show you what it's about to do
3. Ask for confirmation
4. Execute the fix
5. Show you the results

### Option 3: With Environment Variables

```bash
DB_HOST=bidhub-bidhub.b.aivencloud.com \
DB_PORT=27575 \
DB_USER=avnadmin \
DB_PASSWORD=[YOUR_DATABASE_PASSWORD] \
DB_NAME=defaultdb \
./apply_credit_system_fix.sh
```

---

## 📊 What Gets Fixed

The script applies the following changes to your database:

### 1. **Adds Row-Level Locking** 🔒
   - Prevents race conditions
   - Eliminates lost updates
   - Ensures balance consistency

### 2. **Updates PlaceBid Procedure**
   - Adds `FOR UPDATE` locking
   - Implements automatic outbid refunds
   - Records all transactions in ledger

### 3. **Updates BuyNow Procedure**
   - Adds `FOR UPDATE` locking on buyer and seller rows
   - Validates balance before deduction
   - Atomic credit transfer

### 4. **Adds Idempotency Support**
   - Prevents duplicate transactions
   - Protects against double-charging
   - Adds `idempotency_key` column

### 5. **Adds Performance Indices**
   - Speeds up balance queries
   - Optimizes transaction lookups
   - Improves bidding performance

---

## ✅ Expected Results

After running the fix, you should see:

### ✅ Before Fix Issues (RESOLVED):
- ❌ "Insufficient Credits" errors when balance is sufficient
- ❌ Balance drift (stored ≠ computed)
- ❌ Race conditions causing lost updates
- ❌ Double-charging on network delays

### ✅ After Fix Benefits:
- ✓ Buy Now works correctly with sufficient balance
- ✓ Balances stay consistent
- ✓ Concurrent transactions handled properly
- ✓ Better error messages showing exact amounts

---

## 📁 Files Generated

The script creates several files for your records:

```
backups/
  └── backup_before_fix_YYYYMMDD_HHMMSS.sql    # Database backup

logs/
  ├── diagnostic_before_YYYYMMDD_HHMMSS.txt     # Pre-fix state
  ├── diagnostic_after_YYYYMMDD_HHMMSS.txt      # Post-fix state
  └── fix_application_YYYYMMDD_HHMMSS.log       # Fix execution log
```

### 📥 Backup File

**Location:** `backups/backup_before_fix_YYYYMMDD_HHMMSS.sql`

This is a **complete backup** of your database before any changes. Keep this file safe!

**To restore from backup (if needed):**
```bash
mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -p[YOUR_DATABASE_PASSWORD] \
  defaultdb < backups/backup_before_fix_YYYYMMDD_HHMMSS.sql
```

### 📋 Diagnostic Files

**Before:** Shows current database state with issues
**After:** Shows database state after fix applied

**To compare:**
```bash
diff logs/diagnostic_before_*.txt logs/diagnostic_after_*.txt
```

---

## 🧪 Testing After Fix

### Test 1: Normal Buy Now Purchase

1. Log into the Android app
2. Find an item with Buy Now price < your balance
3. Click "Buy Now" → Confirm
4. **Expected:** Purchase succeeds, balance updated correctly

### Test 2: Bidding

1. Find an active auction
2. Place a bid higher than current price
3. Have another user outbid you
4. **Expected:** Your credits are refunded automatically

### Test 3: Insufficient Credits Error

1. Find an item more expensive than your balance
2. Click "Buy Now"
3. **Expected:** Clear error message showing:
   - Required amount
   - Your current balance
   - How many more credits you need

---

## 🔍 Verification Commands

### Check Stored Procedures

```bash
mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -p[YOUR_DATABASE_PASSWORD] defaultdb -e "
SELECT ROUTINE_NAME, LAST_ALTERED
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'defaultdb'
AND ROUTINE_NAME IN ('PlaceBid', 'BuyNow', 'EndAuction');"
```

**Expected output:** All 3 procedures with recent LAST_ALTERED timestamps

### Check for Balance Issues

```bash
mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -p[YOUR_DATABASE_PASSWORD] \
  defaultdb < sql/comprehensive_database_diagnostic.sql | grep -A 10 "Balance Mismatches"
```

**Expected output:** 0 users with balance mismatches

### Check Recent Transactions

```bash
mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -p[YOUR_DATABASE_PASSWORD] defaultdb -e "
SELECT id, user_id, type, amount, status, reference, transaction_date
FROM credit_transactions
ORDER BY transaction_date DESC
LIMIT 20;"
```

**Expected output:** Transaction history with proper refunds for outbid bids

---

## ⚠️ Troubleshooting

### Script Won't Run

**Error:** `Permission denied`
```bash
# Fix: Make script executable
chmod +x apply_credit_system_fix.sh
./apply_credit_system_fix.sh
```

### Can't Connect to Database

**Error:** `Cannot connect to database`

**Solutions:**
1. Verify Aiven cloud database is accessible:
   ```bash
   ping bidhub-bidhub.b.aivencloud.com
   ```

2. Test database connection:
   ```bash
   mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
     -u avnadmin -p[YOUR_DATABASE_PASSWORD] -e "SELECT 1"
   ```

3. Check database exists:
   ```bash
   mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
     -u avnadmin -p[YOUR_DATABASE_PASSWORD] -e "SHOW DATABASES"
   ```

4. Check firewall/network: Ensure port 27575 is not blocked

### Fix Failed Midway

**Error:** `Fix application failed`

The script automatically offers to rollback. If you declined:

```bash
# Manual rollback
mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -p[YOUR_DATABASE_PASSWORD] \
  defaultdb < backups/backup_before_fix_YYYYMMDD_HHMMSS.sql
```

### Want to Re-run the Fix

The fix is **idempotent** - safe to run multiple times:

```bash
./apply_credit_system_fix.sh
```

It will skip steps that are already done and only apply missing changes.

---

## 🔄 Rollback Instructions

If you need to undo the fix for any reason:

### Automatic Rollback (During Script Execution)

If the script fails, it will offer to rollback automatically. Choose "yes".

### Manual Rollback (After Script Completed)

```bash
# Find your backup file
ls -lt backups/

# Restore from backup
mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -p[YOUR_DATABASE_PASSWORD] \
  defaultdb < backups/backup_before_fix_YYYYMMDD_HHMMSS.sql
```

**⚠️ WARNING:** Rollback will lose any transactions that occurred after the fix!

---

## 📞 Support

If you encounter issues:

1. **Check the logs:**
   ```bash
   tail -50 logs/fix_application_*.log
   ```

2. **Review diagnostic output:**
   ```bash
   cat logs/diagnostic_after_*.txt
   ```

3. **Check for detailed analysis:**
   - Read: `INSUFFICIENT_CREDITS_ROOT_CAUSE_ANALYSIS.md`
   - Review: `DATABASE_INVESTIGATION_REPORT.md`

4. **Backup is always available:**
   ```bash
   ls -lh backups/
   ```

---

## ✨ Success Indicators

After the fix, your terminal should show:

```
╔════════════════════════════════════════════════════════════════════╗
║                    FIX DEPLOYED SUCCESSFULLY                       ║
╚════════════════════════════════════════════════════════════════════╝

Deployment Details:
  • Timestamp:        20251025_143022
  • Database:         defaultdb
  • Host:             bidhub-bidhub.b.aivencloud.com:27575
  • User:             avnadmin

Changes Applied:
  ✓ Added row-level locking (FOR UPDATE) to all procedures
  ✓ PlaceBid procedure updated with outbid refund logic
  ✓ BuyNow procedure updated with proper validation
  ✓ EndAuction procedure updated
  ✓ Added idempotency support
  ✓ Added balance versioning
  ✓ Created performance indices

Expected Improvements:
  ✓ "Insufficient Credits" false positives eliminated
  ✓ Race conditions prevented
  ✓ Balance consistency guaranteed
  ✓ Concurrent transaction support
```

---

## 🎯 Next Steps

After successful deployment:

1. **Test the Android app**
   - Try Buy Now on multiple items
   - Test bidding functionality
   - Verify balance updates correctly

2. **Monitor for issues**
   - Check application logs
   - Watch for error patterns
   - Verify transaction completions

3. **Keep the backup**
   - Don't delete `backups/` folder
   - Keep for at least 30 days
   - Store in safe location

---

## 📚 Additional Resources

- **Root Cause Analysis:** `INSUFFICIENT_CREDITS_ROOT_CAUSE_ANALYSIS.md`
- **Database Investigation:** `DATABASE_INVESTIGATION_REPORT.md`
- **Diagnostic Script:** `sql/comprehensive_database_diagnostic.sql`
- **Fix Script:** `sql/fix_credit_system_comprehensive.sql`

---

**Ready to fix your credit system?**

```bash
./apply_credit_system_fix.sh
```

🚀 **Good luck!**
