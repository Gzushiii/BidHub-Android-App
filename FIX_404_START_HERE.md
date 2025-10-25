# 🚀 START HERE: Fix 404 "Item not found or not active" Errors

## 📋 Quick Status

**Issue:** Items load successfully in UI but bid/buy-now operations fail with 404 errors.

**Evidence from your logcat:**
```
✅ GET /api/items/0b9cb399-7aa3-4235-a3bd-9650f114358b → 200 OK (item loads)
❌ POST /api/bids/place → 404 "Item not found or not active"
❌ POST /api/items/.../buy-now → 404
```

**Root Cause:** Schema mismatch between read and write endpoints.

---

## 🎯 Three-Step Fix Process

### Step 1: Diagnose (5 minutes) ⚠️ DO THIS FIRST

Run the comprehensive schema verification script:

```bash
cd /home/dane/Desktop/CC106-G5-BIDHUB/BidHub-Android-App

mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -pAVNS_68KTsuwZOAe-MjO1IPK \
  defaultdb < sql/verify_production_schema.sql > verification_output.txt 2>&1
```

**Review the output:**

```bash
cat verification_output.txt
```

**Look for:**
1. **Items table id column type** (INT vs UUID)
2. **Status column type** (ENUM vs VARCHAR)
3. **Current status values** in the database
4. **Your failing item** (0b9cb399-7aa3-4235-a3bd-9650f114358b)
5. **Items visible in UI but failing transactions**

---

### Step 2: Apply Fix (2 minutes)

Based on verification results, choose ONE of these fixes:

#### Fix A: Status Value Mismatch (Most Likely)

If verification shows items have status ≠ 'active' or 'draft':

**Fix single item:**
```bash
mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -pAVNS_68KTsuwZOAe-MjO1IPK \
  defaultdb < quick_fix_item_status.sql
```

**Fix ALL items (recommended):**
```bash
mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -pAVNS_68KTsuwZOAe-MjO1IPK \
  defaultdb < fix_all_items_status.sql
```

**Note:** Script has been updated to work with Aiven MySQL's `sql_require_primary_key` setting.

#### Fix B: Backend Code Inconsistency

If status values are correct but endpoints use different queries:

1. Review `FIX_404_ISSUE_GUIDE.md` for backend code changes
2. Update `bidhub-backend/src/routes/bids.js`
3. Update `bidhub-backend/src/routes/items.js`
4. Use unified `itemHelpers.js` functions
5. Redeploy backend

#### Fix C: View Definition Mismatch

If v_active_items view uses different filter than endpoints:

See `SCHEMA_MISMATCH_ANALYSIS.md` section "Fix 3: View Definition Update"

---

### Step 3: Test (2 minutes)

After applying the fix:

#### Test in Android App:

1. Open item: "bnuys" (ID: 0b9cb399-7aa3-4235-a3bd-9650f114358b)
2. Try placing a bid
3. Try Buy Now

**Expected results:**

✅ **Success:**
```
Bid placed successfully
  OR
Insufficient credits (but specific error, not 404!)
```

✅ **Success:**
```
Purchase completed successfully
  OR
Insufficient credits (but specific error, not 404!)
```

❌ **Still failing:**
```
404 "Item not found or not active"
```

If still failing → review verification output more carefully, check SCHEMA_MISMATCH_ANALYSIS.md

---

## 📚 Documentation Index

| Document | Purpose | When to Read |
|----------|---------|--------------|
| **FIX_404_START_HERE.md** | This document - quick start guide | Read first |
| **sql/verify_production_schema.sql** | Diagnostic script | Run first before any fixes |
| **SCHEMA_MISMATCH_ANALYSIS.md** | Detailed schema analysis and root cause | If verification shows schema issues |
| **RUN_THIS_TO_FIX_404.md** | Step-by-step fix instructions | When applying fixes |
| **FIX_404_ISSUE_GUIDE.md** | Comprehensive explanation and backend code fixes | For deep dive or backend changes |
| **quick_fix_item_status.sql** | Fix single item status | Quick test fix |
| **fix_all_items_status.sql** | Fix all items status | Recommended production fix |
| **diagnose_404_issue.sql** | Simple diagnostic for one item | Alternative to full verification |
| **bidhub-backend/src/utils/itemHelpers.js** | Unified item lookup helpers | Backend code integration |

---

## 🔍 What Each Script Does

### Diagnostic Scripts (Run BEFORE fixing)

**sql/verify_production_schema.sql**
- ✅ Complete schema analysis
- ✅ Shows actual production schema vs expected
- ✅ Identifies exact issue (status values, id types, view definition)
- ✅ Tests your specific failing item
- ✅ Provides recommendations

**diagnose_404_issue.sql**
- ✅ Simpler diagnostic for single item
- ✅ Shows why item appears in UI but fails transactions
- ✅ Compares view vs direct queries

### Fix Scripts (Run AFTER diagnosis)

**quick_fix_item_status.sql**
- Updates single item (0b9cb399...) to 'active' status
- Good for quick testing
- Use if only one item is affected

**fix_all_items_status.sql**
- Finds ALL items in v_active_items with incompatible status
- Updates them to 'active'
- Shows before/after statistics
- **Recommended for production fix**

### Backend Code Fixes (Optional but Proper)

**bidhub-backend/src/utils/itemHelpers.js**
- Unified item lookup functions
- Consistent error handling
- Uses same view as GET endpoints
- Prevents future schema mismatches

---

## ⚠️ Common Pitfalls

### Pitfall 1: Skipping Verification

**DON'T:**
```bash
# Blindly apply fix without knowing the issue
mysql ... < fix_all_items_status.sql
```

**DO:**
```bash
# Diagnose first, then fix
mysql ... < sql/verify_production_schema.sql > output.txt
cat output.txt  # Review!
mysql ... < fix_all_items_status.sql  # Then fix
```

### Pitfall 2: Fixing One Item When All Are Broken

If verification shows multiple items with wrong status, fix ALL at once:

```bash
mysql ... < fix_all_items_status.sql  # Not quick_fix_item_status.sql
```

### Pitfall 3: Not Testing After Fix

Always test in Android app after applying database fixes. If still failing:
1. Review verification output again
2. Check SCHEMA_MISMATCH_ANALYSIS.md
3. Consider backend code changes from FIX_404_ISSUE_GUIDE.md

---

## 🎯 TL;DR - Fastest Path to Fix

If you just want to fix it NOW:

```bash
cd /home/dane/Desktop/CC106-G5-BIDHUB/BidHub-Android-App

# 1. Diagnose (save output)
mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -pAVNS_68KTsuwZOAe-MjO1IPK \
  defaultdb < sql/verify_production_schema.sql > verify.txt 2>&1

# 2. Review output
cat verify.txt | grep -A 5 "status\|compatibility\|failing"

# 3. If status values are wrong, fix ALL items
mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -pAVNS_68KTsuwZOAe-MjO1IPK \
  defaultdb < fix_all_items_status.sql

# 4. Test in Android app
# Open item "bnuys" → Try bid → Try Buy Now → Should work!
```

**Expected time:** 10 minutes total (5 min diagnose + 2 min fix + 3 min test)

---

## 📞 Still Having Issues?

After running verification and fixes, if you still see 404 errors:

1. **Check verification output** for unexpected findings
2. **Review SCHEMA_MISMATCH_ANALYSIS.md** for schema-specific issues
3. **Check backend logs** on Render for detailed error messages
4. **Consider backend code integration** using itemHelpers.js for long-term fix

---

## ✅ Success Criteria

You know the fix worked when:

✅ Bid placement succeeds OR shows specific error ("Insufficient credits")
✅ Buy Now succeeds OR shows specific error ("Insufficient credits")
✅ **NO MORE:** "Item not found or not active"
✅ **NO MORE:** Generic 404 errors
✅ Errors are specific and actionable

---

**Ready?** Start with Step 1: Diagnose! 🚀
