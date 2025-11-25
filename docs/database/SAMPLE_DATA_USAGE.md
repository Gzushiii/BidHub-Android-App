# Sample Data for BidHub App

## Overview

The file `sql/insert_sample_data.sql` contains ready-to-use sample data with:
- ✅ Proper bcrypt password hashes
- ✅ 10 main categories + 14 subcategories
- ✅ 4 test users with different credit balances
- ✅ 8 sample auction items
- ✅ All users marked as verified and active

---

## How to Use

### Option 1: Run via MySQL Command Line
```bash
mysql -u your_username -p defaultdb < sql/insert_sample_data.sql
```

### Option 2: Run via MySQL Workbench
1. Open MySQL Workbench
2. Connect to your database
3. Open `sql/insert_sample_data.sql`
4. Click "Execute" or press `Ctrl+Shift+Enter`

### Option 3: Run via Command
```bash
cd bidhub-backend
node generate_sample_data.js
# Then run the generated SQL file in your database
```

---

## Test User Accounts

| Email | Password | Credits | Name |
|-------|----------|---------|------|
| `alex.smith@example.com` | `password123` | $150.00 | Alex Smith (AlexS) |
| `jane.doe@example.com` | `password123` | $200.00 | Jane Doe (JaneD) |
| `bob.wilson@example.com` | `password123` | $100.00 | Bob Wilson (BobW) |
| `test@example.com` | `test1234` | $500.00 | Test User (TestUser) |

---

## Sample Items Included

1. **Vintage Nikon Camera** - $150 starting bid (Alex)
2. **Designer Leather Handbag** - $800 starting bid (Jane)
3. **Modern Sectional Sofa** - $1,200 starting bid (Alex)
4. **Rare Coin Collection** - $50 starting bid (Bob)
5. **Wireless Headphones** - $200 starting bid (Test User)
6. **Gaming Laptop** - $1,000 starting bid, $1,050 current (Alex)
7. **Vintage Watch Collection** - $300 starting bid (Jane)
8. **Designer Sunglasses** - $100 starting bid (Bob)

---

## Categories Included

### Main Categories
1. Electronics
2. Fashion
3. Home & Garden
4. Sports & Outdoors
5. Books & Media
6. Automotive
7. Health & Beauty
8. Toys & Games
9. Collectibles
10. Others

### Subcategories
- **Electronics**: Smartphones, Laptops, Tablets, Audio, Cameras, Gaming
- **Fashion**: Men's Clothing, Women's Clothing, Shoes, Accessories
- **Home & Garden**: Furniture, Kitchen, Garden, Tools

---

## What Happens When You Run It

1. ✅ Inserts all categories (uses `INSERT IGNORE` to skip duplicates)
2. ✅ Deletes existing test users to avoid conflicts
3. ✅ Inserts 4 new users with proper password hashes
4. ✅ Deletes existing test items from those users
5. ✅ Inserts 8 sample auction items
6. ✅ Shows verification counts and credentials

---

## Regenerate Sample Data

To regenerate the SQL file with fresh hashes:

```bash
cd bidhub-backend
node generate_sample_data.js
```

This will:
- Generate new bcrypt hashes for all passwords
- Generate new random salts
- Output to `sql/insert_sample_data.sql`

---

## Database Requirements

The script requires these tables to exist:
- ✅ `categories`
- ✅ `users`  
- ✅ `items`

If tables don't exist, run the schema creation scripts first:
- `sql/complete_database_recreation.sql` - Complete schema
- `sql/bidhub_schema_step2_database.sql` - Tables only

---

## Troubleshooting

### "Duplicate entry" errors
- The script uses `INSERT IGNORE` for categories
- Users and items are deleted before insertion
- Run the full script multiple times safely

### "Unknown column" errors
- Check that your database schema matches the expected structure
- Run schema verification: `sql/check_database_structure.sql`

### Password not working
- Make sure you're using the exact password: `password123` or `test1234`
- All passwords are lowercase, no spaces
- If regenerating, old hashes won't work with new hashes

---

## Testing the Data

After inserting sample data:

### 1. Verify Users
```sql
SELECT id, email, username, alias, credits 
FROM users 
ORDER BY created_at DESC;
```

### 2. Verify Items
```sql
SELECT id, title, starting_price, status, location 
FROM items 
ORDER BY created_at DESC;
```

### 3. Test Login (Via App)
- Open BidHub Android app
- Use any of the test email/password combinations
- Should login successfully

### 4. Test Login (Via API)
```bash
curl -X POST "https://bidhub-android-app.onrender.com/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"alex.smith@example.com","password":"password123"}'
```

---

## Files

- **`sql/insert_sample_data.sql`** - Ready-to-run SQL file
- **`generate_sample_data.js`** - Script to regenerate SQL with fresh hashes
- **`SAMPLE_DATA_USAGE.md`** - This documentation

---

## Next Steps

After loading sample data:

1. ✅ Test login with all accounts
2. ✅ Browse items in the app
3. ✅ Test bidding functionality
4. ✅ Test credit transactions
5. ✅ Test item posting

---

**Note**: All passwords are hashed using bcrypt with 8 rounds (matching backend configuration). Never commit plain text passwords to version control!

