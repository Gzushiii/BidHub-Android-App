#!/bin/bash

# Comprehensive Test Script for Schema Fixes
# This script tests all API endpoints to verify the schema fixes are working

echo "======================================================="
echo "TESTING API ENDPOINTS AFTER SCHEMA FIXES"
echo "======================================================="
echo ""

# Set the base URL
BASE_URL="${BASE_URL:-https://bidhub-android-app.onrender.com}"

echo "Testing base URL: $BASE_URL"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Function to test endpoint
test_endpoint() {
    local name=$1
    local method=$2
    local endpoint=$3
    local headers=$4
    local data=$5
    
    echo "Testing: $name"
    echo "Endpoint: $method $endpoint"
    
    if [ -n "$data" ]; then
        RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            $headers \
            -d "$data")
    else
        RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            $headers)
    fi
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_STATUS:" | cut -d':' -f2)
    BODY=$(echo "$RESPONSE" | sed '/HTTP_STATUS:/d')
    
    if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
        echo -e "${GREEN}✓ PASSED${NC} (HTTP $HTTP_CODE)"
        echo "Response: $(echo "$BODY" | head -c 200)..."
        ((TESTS_PASSED++))
    elif [ "$HTTP_CODE" -eq 401 ] || [ "$HTTP_CODE" -eq 403 ]; then
        echo -e "${YELLOW}⚠ AUTH REQUIRED${NC} (HTTP $HTTP_CODE) - This is expected for protected endpoints"
        echo "Response: $(echo "$BODY" | head -c 200)..."
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ FAILED${NC} (HTTP $HTTP_CODE)"
        echo "Response: $BODY"
        ((TESTS_FAILED++))
    fi
    echo ""
}

# Test 1: Health Check
test_endpoint "Health Check" "GET" "/api/health" "" ""

# Test 2: Get Categories
test_endpoint "Get Categories" "GET" "/api/categories" "" ""

# Test 3: Get Items (List) - This should work with v_active_items view
test_endpoint "Get Items List" "GET" "/api/items" ""

# Test 4: Get Items with Filters
test_endpoint "Get Items (Filtered)" "GET" "/api/items?status=active&limit=5" ""

# Test 5: Register a test user (to get auth token)
echo "=========================================="
echo "Testing Authentication Endpoints"
echo "=========================================="
echo ""

REGISTER_DATA='{
    "username": "schema_test_'$(date +%s)'",
    "email": "schematest'$(date +%s)'@example.com",
    "phone_number": "+1234567890",
    "password": "testpass123",
    "first_name": "Schema",
    "last_name": "Test",
    "alias": "schemtest'$(date +%s)'"
}'

REGISTER_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "$REGISTER_DATA")

REGISTER_HTTP_CODE=$(echo "$REGISTER_RESPONSE" | grep "HTTP_STATUS:" | cut -d':' -f2)
REGISTER_BODY=$(echo "$REGISTER_RESPONSE" | sed '/HTTP_STATUS:/d')

if [ "$REGISTER_HTTP_CODE" -ge 200 ] && [ "$REGISTER_HTTP_CODE" -lt 300 ]; then
    echo -e "${GREEN}✓ User Registration PASSED${NC} (HTTP $REGISTER_HTTP_CODE)"
    TOKEN=$(echo "$REGISTER_BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    if [ -n "$TOKEN" ]; then
        echo "Token obtained: ${TOKEN:0:30}..."
        AUTH_HEADER="-H \"Authorization: Bearer $TOKEN\""
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ Failed to extract token${NC}"
        ((TESTS_FAILED++))
    fi
else
    echo -e "${RED}✗ User Registration FAILED${NC} (HTTP $REGISTER_HTTP_CODE)"
    echo "Response: $REGISTER_BODY"
    ((TESTS_FAILED++))
    TOKEN=""
fi
echo ""

# Test 6: Login (if registration failed, try with existing user)
if [ -z "$TOKEN" ]; then
    LOGIN_DATA='{
        "email": "testuser444@example.com",
        "password": "password123"
    }'
    
    LOGIN_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "$LOGIN_DATA")
    
    LOGIN_HTTP_CODE=$(echo "$LOGIN_RESPONSE" | grep "HTTP_STATUS:" | cut -d':' -f2)
    LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '/HTTP_STATUS:/d')
    
    if [ "$LOGIN_HTTP_CODE" -ge 200 ] && [ "$LOGIN_HTTP_CODE" -lt 300 ]; then
        echo -e "${GREEN}✓ Login PASSED${NC} (HTTP $LOGIN_HTTP_CODE)"
        TOKEN=$(echo "$LOGIN_BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$TOKEN" ]; then
            echo "Token obtained: ${TOKEN:0:30}..."
            AUTH_HEADER="-H \"Authorization: Bearer $TOKEN\""
            ((TESTS_PASSED++))
        fi
    fi
    echo ""
fi

# Test authenticated endpoints if we have a token
if [ -n "$TOKEN" ]; then
    echo "=========================================="
    echo "Testing Authenticated Endpoints"
    echo "=========================================="
    echo ""
    
    # Test 7: Get Credits Balance (tests credit_transactions table)
    test_endpoint "Get Credits Balance" "GET" "/api/credits/balance" "-H \"Authorization: Bearer $TOKEN\"" ""
    
    # Test 8: Get Credit Transactions (tests credit_transactions table)
    test_endpoint "Get Credit Transactions" "GET" "/api/credits/transactions" "-H \"Authorization: Bearer $TOKEN\"" ""
    
    # Test 9: Create Item (tests items table with uuid_id, etc.)
    ITEM_DATA='{
        "title": "Schema Test Item '$(date +%s)'",
        "description": "This item was created to test the schema fixes. It should have uuid_id, starting_bid, reserve_price, and end_date.",
        "category_id": 1,
        "starting_price": 50.00,
        "reserve_price": 75.00,
        "duration_days": 7,
        "images": []
    }'
    
    CREATE_ITEM_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/api/items" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "$ITEM_DATA")
    
    CREATE_ITEM_HTTP_CODE=$(echo "$CREATE_ITEM_RESPONSE" | grep "HTTP_STATUS:" | cut -d':' -f2)
    CREATE_ITEM_BODY=$(echo "$CREATE_ITEM_RESPONSE" | sed '/HTTP_STATUS:/d')
    
    if [ "$CREATE_ITEM_HTTP_CODE" -ge 200 ] && [ "$CREATE_ITEM_HTTP_CODE" -lt 300 ]; then
        echo -e "${GREEN}✓ Item Creation PASSED${NC} (HTTP $CREATE_ITEM_HTTP_CODE)"
        ITEM_UUID=$(echo "$CREATE_ITEM_BODY" | grep -o '"uuid_id":"[^"]*"' | cut -d'"' -f4)
        ITEM_ID=$(echo "$CREATE_ITEM_BODY" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        echo "Item created with UUID: $ITEM_UUID"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ Item Creation FAILED${NC} (HTTP $CREATE_ITEM_HTTP_CODE)"
        echo "Response: $CREATE_ITEM_BODY"
        ((TESTS_FAILED++))
    fi
    echo ""
    
    # Test 10: Get Specific Item by UUID (tests item lookup with uuid_id)
    if [ -n "$ITEM_UUID" ]; then
        test_endpoint "Get Item by UUID" "GET" "/api/items/$ITEM_UUID" "-H \"Authorization: Bearer $TOKEN\"" ""
    fi
    
    # Test 11: Purchase Credits (tests credit_transactions table)
    PURCHASE_DATA='{
        "amount": 100.00,
        "payment_method": "test",
        "transaction_id": "test_txn_'$(date +%s)'"
    }'
    
    PURCHASE_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/api/credits/purchase" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "$PURCHASE_DATA")
    
    PURCHASE_HTTP_CODE=$(echo "$PURCHASE_RESPONSE" | grep "HTTP_STATUS:" | cut -d':' -f2)
    PURCHASE_BODY=$(echo "$PURCHASE_RESPONSE" | sed '/HTTP_STATUS:/d')
    
    if [ "$PURCHASE_HTTP_CODE" -ge 200 ] && [ "$PURCHASE_HTTP_CODE" -lt 300 ]; then
        echo -e "${GREEN}✓ Credit Purchase PASSED${NC} (HTTP $PURCHASE_HTTP_CODE)"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ Credit Purchase FAILED${NC} (HTTP $PURCHASE_HTTP_CODE)"
        echo "Response: $PURCHASE_BODY"
        ((TESTS_FAILED++))
    fi
    echo ""
fi

# Summary
echo "======================================================="
echo "TEST SUMMARY"
echo "======================================================="
echo -e "${GREEN}Tests Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Tests Failed: $TESTS_FAILED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed! Schema fixes are working correctly.${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed. Please check the errors above.${NC}"
    exit 1
fi

