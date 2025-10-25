# Posting Flow Fixes - Test Verification Guide

## **FIXES IMPLEMENTED**

### ✅ **1. Optional Fields Fixed**
- **Backend**: Removed minimum image requirement from validation schema
- **Frontend**: Made images optional in form validation
- **Files Modified**: 
  - `bidhub-backend/src/validators/items.js` (lines 72-88, 120-136)
  - `bidhub/app/src/main/java/com/cc106/bidhub/fragments/PostFragment.java` (lines 1105-1109)

### ✅ **2. Draft Behavior Fixed**
- **Backend**: Added draft creation support and publish endpoint
- **Database**: Fixed v_active_items view to exclude drafts
- **Duration**: Drafts don't start duration until published
- **Files Modified**:
  - `bidhub-backend/src/routes/items.js` (lines 176, 193-197, 402-448)
  - `sql/create_active_items_view.sql` (line 30)
  - `bidhub/app/src/main/java/com/cc106/bidhub/api/ItemApiClient.java` (added createDraftItem, publishDraftItem methods)
  - `bidhub/app/src/main/java/com/cc106/bidhub/items/ItemManager.java` (lines 233-366)

### ✅ **3. Self-Listing Restrictions** (Already Working)
- **Backend**: Enforces seller != bidder in bids.js
- **Frontend**: Shows Edit button for owners, hides bid buttons
- **Files**: Already properly implemented

### ✅ **4. My Listings Sync Fixed**
- **Added**: Publish draft functionality with immediate refresh
- **Files Modified**:
  - `bidhub/app/src/main/java/com/cc106/bidhub/adapters/MyListingsAdapter.java` (lines 22-28, 265-321)
  - `bidhub/app/src/main/java/com/cc106/bidhub/MyListingsActivity.java` (lines 479-502)

### ✅ **5. Persistence Fixed**
- **Backend**: Drafts now persisted to database
- **API**: Added draft creation and publish endpoints
- **Files**: All backend endpoints now support draft status

---

## **ACCEPTANCE TESTS**

### **Test 1: Optional Fields**
```bash
# Test with no images
curl -X POST https://bidhub-android-app.onrender.com/api/items \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Item",
    "description": "Test description",
    "category_id": 1,
    "starting_price": 10.00,
    "duration_days": 7,
    "images": []
  }'
# Expected: 201 Created
```

### **Test 2: Draft Behavior**
```bash
# Create draft
curl -X POST https://bidhub-android-app.onrender.com/api/items \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Draft Item",
    "description": "Draft description",
    "category_id": 1,
    "starting_price": 10.00,
    "duration_days": 7,
    "status": "draft"
  }'

# Verify draft not in public feed
curl https://bidhub-android-app.onrender.com/api/items
# Expected: Draft item should NOT appear

# Publish draft
curl -X POST https://bidhub-android-app.onrender.com/api/items/ITEM_ID/publish \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"duration_days": 7}'
# Expected: 200 OK with end_date set
```

### **Test 3: Self-Listing Restrictions**
```bash
# Try to bid on own item
curl -X POST https://bidhub-android-app.onrender.com/api/bids/place \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "item_id": "YOUR_ITEM_ID",
    "amount": 15.00
  }'
# Expected: 400 Bad Request - "Cannot bid on your own item"
```

### **Test 4: My Listings Updates**
1. Create a draft item via app
2. Verify it appears in My Listings with "Publish" button
3. Click "Publish" 
4. Verify item status changes to "Active" immediately
5. Verify item now appears in public Browse feed

### **Test 5: Persistence**
1. Create draft item
2. Close and reopen app
3. Verify draft still exists in My Listings
4. Publish draft
5. Close and reopen app
6. Verify published item still exists and is active

---

## **DATABASE VERIFICATION**

```sql
-- Check items table structure
DESCRIBE items;

-- Verify draft items exist
SELECT id, title, status, end_date, created_at 
FROM items 
WHERE status = 'draft';

-- Verify active items have end_date set
SELECT id, title, status, end_date, created_at 
FROM items 
WHERE status = 'active' AND end_date IS NOT NULL;

-- Verify v_active_items view excludes drafts
SELECT COUNT(*) FROM v_active_items;
SELECT COUNT(*) FROM items WHERE status = 'active';
-- These counts should match
```

---

## **FRONTEND VERIFICATION**

### **PostFragment Tests**
1. **Optional Images**: Submit form with no images → Should succeed
2. **Draft Creation**: Click "Save as Draft" → Should save to backend
3. **Form Validation**: All optional fields should be truly optional

### **MyListingsActivity Tests**
1. **Draft Display**: Drafts show with "Publish" and "Edit" buttons
2. **Publish Function**: Click "Publish" → Item becomes active
3. **Live Updates**: List refreshes immediately after publish

### **ItemDetailActivity Tests**
1. **Owner View**: Item owners see no bid button
2. **Non-Owner View**: Other users see bid button
3. **Edit Button**: Owners see edit option in My Listings

---

## **EXPECTED BEHAVIOR SUMMARY**

| Action | Expected Result |
|--------|----------------|
| Submit with no images | ✅ Success (201) |
| Create draft | ✅ Saved to backend, not in public feed |
| Publish draft | ✅ Status → active, duration starts, appears in feed |
| Owner views own item | ✅ No bid button, Edit button in My Listings |
| Non-owner bids on own item | ✅ 400 error |
| My Listings after create/publish | ✅ Immediate update, no refresh needed |
| App restart | ✅ All items persist (draft and active) |

---

## **ROLLBACK INSTRUCTIONS**

If issues occur, revert these files:
1. `bidhub-backend/src/validators/items.js` - Restore image minimum requirement
2. `bidhub-backend/src/routes/items.js` - Remove draft/publish endpoints
3. `sql/create_active_items_view.sql` - Include drafts in view
4. Frontend files - Remove draft/publish functionality

---

## **MONITORING**

Watch for these logs:
- `"Draft item created successfully via API"`
- `"Draft item published successfully via API"`
- `"Item saved as draft successfully"`
- `"Item published successfully"`

Check database for:
- Draft items with `end_date = NULL`
- Active items with `end_date` set
- Proper status transitions
