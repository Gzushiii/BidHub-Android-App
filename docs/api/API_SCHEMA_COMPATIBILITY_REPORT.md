# API Endpoint & Database Schema Compatibility Report

**Generated:** 2025-01-XX  
**Status:** ⚠️ **CRITICAL ISSUES FOUND**

---

## Executive Summary

The API endpoints in the BidHub backend expect a database schema that **does not match** the schema defined in `complete_database_recreation.sql`. This mismatch will cause **runtime errors** when the API tries to access database tables, columns, and views that don't exist.

---

## 🔴 Critical Issues

### 1. **Missing `item_images` Table**
**Impact:** Item creation and updates will fail  
**SQL File:** ❌ Does not create this table  
**API Expects:** ✅ Table with columns: `item_id`, `image_url`, `display_order`

**Affected Endpoints:**
- `POST /api/items` - Creates items with images
- `PUT /api/items/:id` - Updates item images
- `GET /api/items/:id` - Fetches item images

**Error:** `Table 'defaultdb.item_images' doesn't exist`

---

### 2. **Wrong Transactions Table Name**
**Impact:** Credit transactions will fail  
**SQL File:** Creates `transactions` table  
**API Expects:** `credit_transactions` table

**Affected Endpoints:**
- `GET /api/credits/balance` - Fetches recent transactions
- `GET /api/credits/transactions` - Lists transaction history
- `POST /api/credits/purchase` - Records credit purchases

**Error:** `Table 'defaultdb.credit_transactions' doesn't exist`

---

### 3. **Missing `v_active_items` View**
**Impact:** Item listing will fail  
**SQL File:** Creates `active_items` view (wrong name)  
**API Expects:** `v_active_items` view with specific columns

**Affected Endpoints:**
- `GET /api/items` - Lists all active items
- All item browsing functionality

**Error:** `Table 'defaultdb.v_active_items' doesn't exist`

---

### 4. **Items Table Column Mismatches**

#### Missing Columns:
| Column | API Expects | SQL Has | Impact |
|--------|------------|---------|--------|
| `uuid_id` | ✅ VARCHAR(36) UUID | ❌ Missing | Item lookup by UUID fails |
| `end_date` | ✅ DATETIME | ❌ Missing | Auction deadlines not tracked |
| `reserve_price` | ✅ DECIMAL(10,2) | ❌ Missing | Reserve price not supported |
| `starting_bid` | ✅ DECIMAL(10,2) | ❌ Has `starting_price` | Column name mismatch |

#### Column Name Differences:
- SQL: `starting_price` → API: `starting_bid`
- SQL: `bid_deadline` → API: `end_date`
- SQL: `billing_deadline` → API: Not used by API
- SQL: `item_condition` → API: Not consistently used

**Affected Endpoints:**
- `POST /api/items` - Creates items with UUID and reserve price
- `POST /api/items/:id/publish` - Sets end_date
- `GET /api/items/:id` - Looks up items by UUID
- `POST /api/bids/place` - Uses stored procedures that reference columns

**Error:** `Unknown column 'uuid_id' in 'field list'`

---

### 5. **Stored Procedure Column Mismatches**

#### PlaceBid Procedure:
- Uses `starting_price` (SQL) but should reference `starting_bid` if API uses it
- References columns that may not match actual schema

#### BuyNow Procedure:
- Should work, but depends on items table structure matching

**Affected Endpoints:**
- `POST /api/bids/place` - Uses `PlaceBid` stored procedure
- `POST /api/items/:id/buy-now` - Uses `BuyNow` stored procedure

---

### 6. **View Column Mismatches**

**SQL Creates:**
```sql
CREATE VIEW active_items AS
SELECT i.id, i.title, i.starting_price, i.current_bid, ...
```

**API Expects:**
```sql
SELECT * FROM v_active_items WHERE ... -- expects uuid_id as id, starting_bid, etc.
```

**Required Columns in `v_active_items`:**
- `id` (should be `uuid_id`)
- `uuid_id`
- `title`
- `description`
- `category_id`
- `seller_id`
- `seller_email`
- `starting_bid` (not `starting_price`)
- `current_bid`
- `current_price` (for price filtering)
- `buy_now_price`
- `status`
- `created_at`
- `updated_at`
- `category_name`
- `seller_alias`

---

## 📊 Detailed Comparison

### Items Table

| Column | SQL Schema | API Expects | Status |
|--------|-----------|-------------|--------|
| `id` | ✅ INT UNSIGNED | ✅ INT | ✅ OK |
| `uuid_id` | ❌ Missing | ✅ VARCHAR(36) | 🔴 **MISSING** |
| `title` | ✅ VARCHAR(255) | ✅ VARCHAR(255) | ✅ OK |
| `description` | ✅ TEXT | ✅ TEXT | ✅ OK |
| `category_id` | ✅ INT UNSIGNED | ✅ INT UNSIGNED | ✅ OK |
| `seller_id` | ✅ INT UNSIGNED | ✅ INT UNSIGNED | ✅ OK |
| `seller_email` | ✅ VARCHAR(255) | ✅ VARCHAR(255) | ✅ OK |
| `starting_price` | ✅ DECIMAL(10,2) | ❌ Expects `starting_bid` | ⚠️ **NAME MISMATCH** |
| `starting_bid` | ❌ Missing | ✅ DECIMAL(10,2) | 🔴 **MISSING** |
| `current_bid` | ✅ DECIMAL(10,2) | ✅ DECIMAL(10,2) | ✅ OK |
| `buy_now_price` | ✅ DECIMAL(10,2) | ✅ DECIMAL(10,2) | ✅ OK |
| `reserve_price` | ❌ Missing | ✅ DECIMAL(10,2) | 🔴 **MISSING** |
| `end_date` | ❌ Missing | ✅ DATETIME | 🔴 **MISSING** |
| `bid_deadline` | ✅ DATETIME | ❌ Not used by API | ⚠️ **UNUSED** |
| `billing_deadline` | ✅ DATETIME | ❌ Not used by API | ⚠️ **UNUSED** |
| `item_condition` | ✅ ENUM | ⚠️ Partially used | ⚠️ **PARTIAL** |
| `status` | ✅ ENUM | ✅ ENUM | ✅ OK |
| `location` | ✅ VARCHAR(255) | ❌ Not used by API | ⚠️ **UNUSED** |
| `created_at` | ✅ TIMESTAMP | ✅ TIMESTAMP | ✅ OK |
| `updated_at` | ✅ TIMESTAMP | ✅ TIMESTAMP | ✅ OK |

### Transactions vs Credit Transactions

| Aspect | SQL Schema | API Expects | Status |
|--------|-----------|-------------|--------|
| Table Name | `transactions` | `credit_transactions` | 🔴 **NAME MISMATCH** |
| `transaction_type` | ENUM(bid, buy_now, refund, ...) | ENUM(purchase, redemption, bid, ...) | ⚠️ **ENUM DIFFERS** |
| `reference` | VARCHAR(255) | ✅ Expected | ✅ OK |
| `status` | ❌ Not in SQL | ✅ ENUM(pending, completed, failed) | 🔴 **MISSING** |
| `payment_method` | ❌ Not in SQL | ✅ VARCHAR(50) | 🔴 **MISSING** |
| `transaction_id` | ❌ Not in SQL | ✅ VARCHAR(255) | 🔴 **MISSING** |

---

## 🔧 Required Fixes

### Priority 1: Critical (Blocks API Functionality)

1. **Add `item_images` table**
   ```sql
   CREATE TABLE item_images (
       id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
       item_id INT UNSIGNED NOT NULL,
       image_url VARCHAR(500) NOT NULL,
       display_order INT DEFAULT 1,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
       INDEX idx_item_images_item_id (item_id)
   );
   ```

2. **Rename/Create `credit_transactions` table**
   - Either rename `transactions` to `credit_transactions`
   - Or create new table with correct schema matching API expectations

3. **Add missing columns to `items` table**
   ```sql
   ALTER TABLE items 
   ADD COLUMN uuid_id VARCHAR(36) UNIQUE NULL AFTER id,
   ADD COLUMN starting_bid DECIMAL(10,2) NULL AFTER starting_price,
   ADD COLUMN reserve_price DECIMAL(10,2) NULL,
   ADD COLUMN end_date DATETIME NULL AFTER bid_deadline;
   ```

4. **Create `v_active_items` view**
   ```sql
   CREATE OR REPLACE VIEW v_active_items AS
   SELECT 
       i.uuid_id as id,
       i.uuid_id,
       i.title,
       i.description,
       i.category_id,
       i.seller_id,
       i.seller_email,
       COALESCE(i.starting_bid, i.starting_price) as starting_bid,
       COALESCE(i.starting_bid, i.starting_price) as starting_price,
       i.current_bid as current_bid,
       i.current_bid as current_price,
       i.buy_now_price,
       i.status,
       i.end_date,
       i.created_at,
       i.updated_at,
       u.alias as seller_alias,
       c.name as category_name
   FROM items i
   LEFT JOIN users u ON i.seller_id = u.id
   LEFT JOIN categories c ON i.category_id = c.id
   WHERE i.status = 'active' AND i.uuid_id IS NOT NULL;
   ```

### Priority 2: Important (Enhances Functionality)

5. **Update stored procedures** to use correct column names
6. **Migrate data** from `starting_price` to `starting_bid` if needed
7. **Populate `uuid_id`** for existing items

---

## 📝 API Endpoint Status

| Endpoint | Status | Issue |
|----------|--------|-------|
| `GET /api/health` | ✅ Working | No DB dependency |
| `POST /api/auth/register` | ✅ Working | Uses `users` table correctly |
| `POST /api/auth/login` | ✅ Working | Uses `users` table correctly |
| `GET /api/categories` | ✅ Working | Uses `categories` table correctly |
| `GET /api/items` | 🔴 **BROKEN** | Missing `v_active_items` view |
| `GET /api/items/:id` | 🔴 **BROKEN** | Missing `uuid_id` column, `item_images` table |
| `POST /api/items` | 🔴 **BROKEN** | Missing `uuid_id`, `reserve_price`, `end_date`, `item_images` |
| `PUT /api/items/:id` | 🔴 **BROKEN** | Missing `item_images` table |
| `POST /api/items/:id/publish` | 🔴 **BROKEN** | Missing `end_date` column |
| `POST /api/bids/place` | ⚠️ **PARTIAL** | Stored procedure may have column issues |
| `POST /api/items/:id/buy-now` | ⚠️ **PARTIAL** | Stored procedure may work |
| `GET /api/credits/balance` | 🔴 **BROKEN** | Wrong table name `credit_transactions` |
| `GET /api/credits/transactions` | 🔴 **BROKEN** | Wrong table name `credit_transactions` |
| `POST /api/credits/purchase` | 🔴 **BROKEN** | Wrong table name `credit_transactions` |

---

## ✅ Testing Recommendations

1. **Test each endpoint** after applying fixes
2. **Verify stored procedures** work with corrected schema
3. **Check data migration** if renaming columns
4. **Validate UUID generation** for new items
5. **Test item image uploads** with new `item_images` table

---

## 🚀 Next Steps

1. ✅ Review this report
2. ⏳ Create fixed SQL migration script
3. ⏳ Test on development database
4. ⏳ Apply to production (Render) database
5. ⏳ Verify all API endpoints work
6. ⏳ Update documentation

---

**Generated by:** API Schema Compatibility Analysis  
**Last Updated:** 2025-01-XX

