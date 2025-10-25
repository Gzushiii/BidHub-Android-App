# Fix Guide: 404 "Item not found" on Bid & Buy Now

## 🔴 Problem Summary

Items load successfully in the UI (GET /api/items/:id works), but when trying to bid or buy, you get a 404 error. This indicates a **schema mismatch** between READ and WRITE endpoints.

---

## 🎯 Root Cause

Different endpoints use different queries to fetch items:

| Endpoint | Query | Status Filter |
|----------|-------|---------------|
| **GET /api/items/:id** (UI display) | `SELECT * FROM v_active_items WHERE id = ?` | Uses view's filter logic |
| **POST /api/bids/place** (bidding) | `SELECT * FROM items WHERE id = ? AND status IN ('active', 'draft')` | Hardcoded status check |
| **POST /api/items/:id/buy-now** (purchase) | `SELECT * FROM items WHERE id = ?` | Checks status separately |

**The mismatch:** The `v_active_items` view might include items with statuses that don't match the hardcoded filters in bid/buy-now endpoints!

---

## 🔍 Step 1: Diagnose the Issue

Run the diagnostic script to see exactly what's wrong:

```bash
mysql -h bidhub-bidhub.b.aivencloud.com -P 27575 \
  -u avnadmin -pAVNS_68KTsuwZOAe-MjO1IPK \
  defaultdb < diagnose_404_issue.sql
```

**Edit the script first** to set your item ID:
```sql
SET @item_id = 'c156913a-2739-44aa-9cb7-c8aa77e7a8eb';  -- Your actual item ID
```

### What to look for:

1. **Item exists in `items` table?**
   - If NO → Item was never created (fix posting flow)
   - If YES → Continue to #2

2. **Item's status value:**
   - If status is NOT `'active'` or `'draft'` → **THIS IS THE PROBLEM**
   - Common wrong values: `'live'`, `'published'`, `'open'`, `'available'`, etc.

3. **Item exists in `v_active_items` view?**
   - If YES (UI works) but status ≠ 'active'/'draft' → View has different logic than endpoints
   - If NO → View filter is too restrictive

4. **View definition:**
   - Shows what WHERE clause the view uses
   - This is what GET endpoint uses (working)
   - Bid/Buy-Now endpoints need to match this

---

## 🔧 Step 2: Apply the Fix

### Option A: Quick Fix (Update Item Status)

If the diagnostic shows your item has the wrong status (e.g., 'published' instead of 'active'):

```sql
UPDATE items
SET status = 'active'
WHERE id = 'c156913a-2739-44aa-9cb7-c8aa77e7a8eb';
```

**Test immediately:**
- Try bidding again
- Try Buy Now again
- Should work now! ✅

### Option B: Proper Fix (Align All Endpoints)

Use the unified item helpers I've created:

#### 1. Copy the new utility file:

```bash
# File already created at:
# bidhub-backend/src/utils/itemHelpers.js
```

#### 2. Update bids.js to use unified lookup:

**Replace this code** (around line 40-114 in `bids.js`):

```javascript
// OLD CODE (inconsistent)
const [items] = await connection.query(
  'SELECT * FROM items WHERE id = ? AND status IN (?, ?)',
  [item_id, 'active', 'draft']
);
if (items.length === 0) {
  return res.status(404).json({ error: 'Item not found or not active' });
}
const item = items[0];
```

**With this code:**

```javascript
// NEW CODE (uses unified helper)
const { fetchItemWithErrorInfo, validateItemForBidding } = require('../utils/itemHelpers');

const { item, error } = await fetchItemWithErrorInfo(connection, item_id);
if (error) {
  return res.status(error.http_status).json(error.json);
}

// Validate item is biddable
const validation = validateItemForBidding(item, bidder_id);
if (!validation.valid) {
  return res.status(validation.error.http_status).json(validation.error.json);
}
```

#### 3. Update items.js (buy-now) to use unified lookup:

**Replace this code** (around line 448-476 in `items.js`):

```javascript
// OLD CODE
const [items] = await connection.query('SELECT * FROM items WHERE id = ?', [itemId]);
if (items.length === 0) {
  return res.status(404).json({ error: 'Item not found' });
}
const item = items[0];
if (item.status !== 'active' && item.status !== 'draft') {
  return res.status(400).json({ error: 'Item is not available for purchase' });
}
```

**With this code:**

```javascript
// NEW CODE (uses unified helper)
const { fetchItemWithErrorInfo, validateItemForBuyNow } = require('../utils/itemHelpers');

const { item, error } = await fetchItemWithErrorInfo(connection, itemId);
if (error) {
  return res.status(error.http_status).json(error.json);
}

// Validate item is purchasable
const validation = validateItemForBuyNow(item, buyerId);
if (!validation.valid) {
  return res.status(validation.error.http_status).json(validation.error.json);
}
```

#### 4. Redeploy Backend:

```bash
cd bidhub-backend
git add .
git commit -m "fix: unify item lookup logic across all endpoints"
git push origin master
```

Render will auto-deploy the changes.

---

## 🔧 Step 3: Fix the Posting/Publishing Flow

Ensure items are created with the correct status from the start.

### Find your item creation endpoint:

```bash
cd bidhub-backend/src/routes
grep -r "POST.*items" *.js
```

### Verify it sets status correctly:

```javascript
// When creating an item
INSERT INTO items (
  id, title, description, seller_id,
  status,        // ← Must be 'active' when published
  created_at,
  ...
) VALUES (?, ?, ?, ?, 'active', NOW(), ...)

// Or when publishing a draft
UPDATE items
SET status = 'active',  // ← Not 'published', 'live', etc.
    published_at = NOW()
WHERE id = ? AND seller_id = ?
```

### Common mistakes to fix:

❌ **Wrong status values:**
```javascript
status = 'published'  // Wrong!
status = 'live'       // Wrong!
status = 'open'       // Wrong!
```

✅ **Correct status value:**
```javascript
status = 'active'     // Correct!
```

---

## 📊 Step 4: Verify the Fix

### Test 1: Check item status in database

```sql
SELECT id, title, status, created_at
FROM items
WHERE id = 'c156913a-2739-44aa-9cb7-c8aa77e7a8eb';

-- Expected: status = 'active'
```

### Test 2: Check item appears in view

```sql
SELECT id, title, status
FROM v_active_items
WHERE id = 'c156913a-2739-44aa-9cb7-c8aa77e7a8eb';

-- Expected: 1 row returned
```

### Test 3: Test bid endpoint via cURL

```bash
curl -i -X POST \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"item_id":"c156913a-2739-44aa-9cb7-c8aa77e7a8eb","amount":1221}' \
  "https://bidhub-android-app.onrender.com/api/bids/place"

# Expected: 200 OK (or 400 if insufficient credits, but NOT 404)
```

### Test 4: Test buy-now endpoint via cURL

```bash
curl -i -X POST \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"amount":12200}' \
  "https://bidhub-android-app.onrender.com/api/items/c156913a-2739-44aa-9cb7-c8aa77e7a8eb/buy-now"

# Expected: 200 OK (or 400 if insufficient credits, but NOT 404)
```

### Test 5: Test in Android app

1. Open item details (should load ✓)
2. Try placing a bid
   - ✅ Expected: Bid succeeds or shows clear error (insufficient credits, etc.)
   - ❌ NOT: "Item not found or not active"

3. Try Buy Now
   - ✅ Expected: Purchase succeeds or shows clear error
   - ❌ NOT: "Item not found"

---

## 🎯 Understanding Status Values

Your database schema should support these status values:

| Status | Meaning | Visible in UI? | Can Bid? | Can Buy Now? |
|--------|---------|----------------|----------|--------------|
| `draft` | Seller is still editing | No | No | No |
| `active` | Live auction/sale | Yes | Yes | Yes |
| `ended` | Auction ended, awaiting payment | Yes (read-only) | No | No |
| `sold` | Completed sale | Yes (read-only) | No | No |
| `cancelled` | Seller cancelled | No | No | No |
| `deleted` | Soft-deleted | No | No | No |

**The v_active_items view should return items where:**
```sql
status IN ('active', 'ended', 'sold')  -- Viewable
AND deleted_at IS NULL                 -- Not deleted
```

**But bid/buy-now should only allow:**
```sql
status = 'active'                      -- Transactable
AND deleted_at IS NULL
AND end_date > NOW()                   -- Not expired
```

---

## 🚨 Common Pitfalls

### 1. **Status string casing**
```sql
-- MySQL string comparison is case-sensitive (depending on collation)
status = 'Active'   -- Wrong if DB uses 'active'
status = 'ACTIVE'   -- Wrong if DB uses 'active'
status = 'active'   -- Correct
```

**Fix:** Always use lowercase, or use `COLLATE utf8mb4_general_ci` for case-insensitive comparison.

### 2. **UUID handling**
```sql
-- UUIDs are case-insensitive but whitespace matters
id = 'C156913A-...'   -- Might not match 'c156913a-...'
id = ' c156913a-...'  -- Won't match (leading space)
```

**Fix:** Always `trim()` IDs in backend, use lowercase UUIDs.

### 3. **Different databases in different environments**
```javascript
// Development
DB_HOST=localhost
DB_NAME=bidhub_local

// Production
DB_HOST=bidhub-bidhub.b.aivencloud.com
DB_NAME=defaultdb
```

**Fix:** Verify `.env` file and Render environment variables match.

---

## 📋 Quick Checklist

After applying fixes, verify:

- [ ] Diagnostic script shows item with status = 'active'
- [ ] Item appears in `v_active_items` view
- [ ] Backend uses `fetchActiveItem()` or `fetchItemWithErrorInfo()` in both bid and buy-now endpoints
- [ ] Posting flow sets `status = 'active'` on publish
- [ ] cURL tests for bid and buy-now return 200 (not 404)
- [ ] Android app can bid and buy successfully
- [ ] Errors are specific (e.g., "insufficient_credits") not vague (e.g., "Network error: null")

---

## 🎯 Summary

**Problem:** Different endpoints use different item lookup logic

**Root Cause:** Hardcoded status filters don't match v_active_items view

**Solution:**
1. **Quick:** Update item status to 'active'
2. **Proper:** Use unified `fetchActiveItem()` helper in all endpoints
3. **Long-term:** Fix posting flow to set correct status from the start

**Files Modified:**
- ✅ Created: `bidhub-backend/src/utils/itemHelpers.js`
- 🔄 Update: `bidhub-backend/src/routes/bids.js`
- 🔄 Update: `bidhub-backend/src/routes/items.js`
- 🔄 Update: Item creation/publishing endpoint

**Time to Fix:** ~15 minutes

---

**Ready to apply the fix?**

1. Run diagnostic: `diagnose_404_issue.sql`
2. Quick fix: Update item status OR
3. Proper fix: Use itemHelpers.js
4. Test with cURL
5. Test in Android app

🚀 **You should see specific errors (like "insufficient credits") instead of vague 404s!**
