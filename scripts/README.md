# Scripts Directory

Utility scripts for testing, setup, and maintenance.

## 📁 File Organization

### API Testing
- **`test-api.sh`** - Comprehensive API testing script that tests:
  - Public endpoints (health, categories, items)
  - Authentication (register, login)
  - Authenticated endpoints (credits, transactions, item creation, bidding)
  - Schema compatibility
  - Provides colored output and test summary

### Database Testing
- **`test-database.js`** - Comprehensive database connection test that:
  - Tests database connectivity
  - Lists all tables
  - Tests core tables (users, categories, items, bids, credit_transactions)
  - Checks for stored procedures
  - Provides detailed error messages

### Backend Status & Deployment
- **`check_backend_status.sh`** - Quick backend health check:
  - Tests health endpoint
  - Tests login endpoint
  - Provides timing information
  - Useful for monitoring Render deployments

- **`test-render-deployment.sh`** - Tests Render deployment:
  - Waits for deployment
  - Tests health endpoint
  - Provides next steps for configuration

### Data Generation
- **`generate_sample_data.js`** - Generates sample data with proper bcrypt password hashes:
  - Creates sample users with hashed passwords
  - Creates sample categories
  - Creates sample items
  - Outputs SQL file ready to import

## 🚀 Usage

### Testing API Endpoints

```bash
# Test all API endpoints
./scripts/test-api.sh

# Test with custom base URL
BASE_URL="http://localhost:3000" ./scripts/test-api.sh
```

### Testing Database Connection

```bash
# From project root (requires Node.js and .env file)
node scripts/test-database.js

# Or from bidhub-backend directory
cd bidhub-backend
node ../scripts/test-database.js
```

### Checking Backend Status

```bash
# Quick health check
./scripts/check_backend_status.sh

# Test Render deployment
./scripts/test-render-deployment.sh
```

### Generating Sample Data

```bash
# From bidhub-backend directory (requires bcryptjs)
cd bidhub-backend
node ../scripts/generate_sample_data.js

# Then import the generated SQL file
mysql -u username -p defaultdb < sql/insert_sample_data.sql
```

## 📝 Script Details

### test-api.sh

Comprehensive API testing that covers:
- ✅ Public endpoints (no auth required)
- ✅ User registration and login
- ✅ Authenticated endpoints
- ✅ Item creation and retrieval
- ✅ Credit balance and transactions
- ✅ Top-up endpoints
- ✅ Bidding functionality

**Output:**
- Colored output (green for pass, yellow for warnings, red for failures)
- Test summary with pass/fail counts
- Detailed error messages

### test-database.js

Database connection and schema verification:
- ✅ Connection test with SSL support
- ✅ Table listing
- ✅ Core table verification
- ✅ Stored procedure detection
- ✅ Detailed error reporting

**Requirements:**
- Node.js
- `.env` file with database credentials
- `mysql2` package installed

### check_backend_status.sh

Quick backend health monitoring:
- Tests health endpoint
- Tests authentication
- Shows response times
- Useful for monitoring free-tier Render deployments (cold starts)

### generate_sample_data.js

Sample data generation with security:
- Generates bcrypt password hashes (8 rounds)
- Creates realistic test data
- Outputs ready-to-import SQL
- Includes user credentials in output

**Default Passwords:**
- All users (except test@example.com): `password123`
- test@example.com: `test1234`

## 🔧 Troubleshooting

### API Tests Failing

1. **Check backend is running:**
   ```bash
   ./scripts/check_backend_status.sh
   ```

2. **Verify base URL:**
   - Default: `https://bidhub-android-app.onrender.com`
   - Override with: `BASE_URL="your-url" ./scripts/test-api.sh`

3. **Check authentication:**
   - Tests will auto-register a test user
   - Or use existing credentials if registration fails

### Database Tests Failing

1. **Check .env file:**
   ```bash
   # Verify these are set:
   DB_HOST=your-host
   DB_PORT=3306
   DB_USER=your-user
   DB_PASSWORD=your-password
   DB_NAME=defaultdb
   DB_SSL=true  # for cloud databases
   ```

2. **Verify database is accessible:**
   - Check network connectivity
   - Verify credentials
   - Check firewall rules

3. **Install dependencies:**
   ```bash
   cd bidhub-backend
   npm install mysql2
   ```

## 📋 Cleanup Summary

This directory has been cleaned up to remove redundant files:
- **8 redundant test scripts** merged into comprehensive scripts
- **3 duplicate database connection tests** merged into `test-database.js`
- **5 duplicate API test scripts** merged into `test-api.sh`

The remaining scripts are:
- **Essential testing scripts** for API and database
- **Utility scripts** for status checking and data generation
- **Comprehensive coverage** of all testing needs

## ⚠️ Important Notes

- Most scripts are bash scripts - use Git Bash or WSL on Windows
- Database scripts require Node.js and proper `.env` configuration
- API scripts require `curl` and optionally `jq` for JSON formatting
- Always test on development/staging before production
- Sample data scripts include default passwords - change them in production

## 📚 Related Documentation

- API documentation: `../docs/api/`
- Database documentation: `../docs/database/`
- Testing documentation: `../docs/testing/`
- Deployment documentation: `../docs/deployment/`
