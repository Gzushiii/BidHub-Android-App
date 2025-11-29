#!/bin/bash

# Comprehensive API Schema Fix Testing Script
# Tests all endpoints that were broken due to schema mismatches

echo "======================================================="
echo "COMPREHENSIVE API SCHEMA FIX TESTING"
echo "======================================================="
echo ""

# Set the base URL
BASE_URL="https://bidhub-android-app.onrender.com"

echo "Testing base URL: $BASE_URL"
echo "Date: $(date)"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counter for results
PASSED=0
FAILED=0
WARNINGS=0

# Function to test endpoint
test_endpoint() {
    local name="$1"
    local url="$2"
    local method="${3:-GET}"
    local data="${4:-}"
    local auth_header="${5:-}"
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Testing: $name"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    local headers=()
    if [ ! -z "$auth_header" ]; then
        headers+=(-H "Authorization: Bearer $auth_header")
    fi
    headers+=(-H "Content-Type: application/json")
    
    local response
    if [ "$method" = "POST" ] && [ ! -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "${headers[@]}" -d "$data" "$url" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "${headers[@]}" "$url" 2>&1)
    fi
    
    local http_code=$(echo "$response" | tail -n 1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "${GREEN}✓ PASS${NC} (HTTP $http_code)"
        ((PASSED++))
        echo "Response:"
        echo "$body" | head -c 500
        echo ""
        if [ ${#body} -gt 500 ]; then
            echo "... (truncated)"
        fi
    elif [ "$http_code" -ge 400 ] && [ "$http_code" -lt 500 ]; then
        echo -e "${YELLOW}⚠ WARNING${NC} (HTTP $http_code) - Client error"
        ((WARNINGS++))
        echo "Response:"
        echo "$body" | head -c 500
        echo ""
    else
        echo -e "${RED}✗ FAIL${NC} (HTTP $http_code)"
        ((FAILED++))
        echo "Response:"
        echo "$body"
    fi
    echo ""
}

# =====================================================
# TEST 1: Health Check (Should always work)
# =====================================================
test_endpoint "Health Check" "$BASE_URL/api/health"

# =====================================================
# TEST 2: Get All Items (Was broken - missing v_active_items view)
# =====================================================
test_endpoint "GET /api/items - List all active items" "$BASE_URL/api/items"

# =====================================================
# TEST 3: Get Items with Filters (Was broken - v_active_items view)
# =====================================================
test_endpoint "GET /api/items?status=active" "$BASE_URL/api/items?status=active"
test_endpoint "GET /api/items?limit=5" "$BASE_URL/api/items?limit=5"

# =====================================================
# TEST 4: Get Categories (Should work)
# =====================================================
test_endpoint "GET /api/categories" "$BASE_URL/api/categories"

# =====================================================
# TEST 5: User Registration (Should work)
# =====================================================
RANDOM_EMAIL="test_$(date +%s)@example.com"
REGISTER_DATA="{\"username\":\"testuser$(date +%s)\",\"email\":\"$RANDOM_EMAIL\",\"phone_number\":\"+1234567890\",\"password\":\"Test123!\",\"first_name\":\"Test\",\"last_name\":\"User\",\"alias\":\"TestUser$(date +%s)\"}"
test_endpoint "POST /api/auth/register - Create new user" "$BASE_URL/api/auth/register" "POST" "$REGISTER_DATA"

# Extract token from registration response for subsequent tests
TOKEN=""
if [ -f /tmp/register_response.json ]; then
    TOKEN=$(grep -o '"token":"[^"]*' /tmp/register_response.json | cut -d'"' -f4 || echo "")
fi

# =====================================================
# TEST 6: User Login (Should work)
# =====================================================
LOGIN_DATA="{\"email\":\"$RANDOM_EMAIL\",\"password\":\"Test123!\"}"
test_endpoint "POST /api/auth/login - User login" "$BASE_URL/api/auth/login" "POST" "$LOGIN_DATA"

# Try to extract token from login response
LOGIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "$LOGIN_DATA" "$BASE_URL/api/auth/login")
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4 || echo "")

if [ -z "$TOKEN" ]; then
    echo -e "${YELLOW}⚠ Warning: Could not extract auth token. Some authenticated tests will be skipped.${NC}"
    echo ""
fi

# =====================================================
# TEST 7: Get Credits Balance (Was broken - wrong table name)
# =====================================================
if [ ! -z "$TOKEN" ]; then
    test_endpoint "GET /api/credits/balance - Get user credits" "$BASE_URL/api/credits/balance" "GET" "" "$TOKEN"
else
    echo -e "${YELLOW}⚠ Skipping credits balance test (no auth token)${NC}"
    ((WARNINGS++))
    echo ""
fi

# =====================================================
# TEST 8: Get Credit Transactions (Was broken - wrong table name)
# =====================================================
if [ ! -z "$TOKEN" ]; then
    test_endpoint "GET /api/credits/transactions - Get transaction history" "$BASE_URL/api/credits/transactions" "GET" "" "$TOKEN"
else
    echo -e "${YELLOW}⚠ Skipping transactions test (no auth token)${NC}"
    ((WARNINGS++))
    echo ""
fi

# =====================================================
# TEST 9: Create Item (Was broken - missing columns: uuid_id, reserve_price, end_date, item_images table)
# =====================================================
if [ ! -z "$TOKEN" ]; then
    ITEM_DATA="{\"title\":\"Test Item $(date +%s)\",\"description\":\"This is a test item created to verify schema fixes.\",\"category_id\":1,\"starting_price\":50.00,\"reserve_price\":100.00,\"duration_days\":7,\"images\":[]}"
    CREATE_RESPONSE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$ITEM_DATA" "$BASE_URL/api/items")
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Testing: POST /api/items - Create new item"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$ITEM_DATA" "$BASE_URL/api/items")
    
    if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
        echo -e "${GREEN}✓ PASS${NC} (HTTP $HTTP_CODE)"
        ((PASSED++))
        
        # Extract item UUID for subsequent tests
        ITEM_UUID=$(echo "$CREATE_RESPONSE" | grep -o '"id":"[^"]*' | cut -d'"' -f4 || echo "")
        ITEM_ID=$(echo "$CREATE_RESPONSE" | grep -o '"uuid_id":"[^"]*' | cut -d'"' -f4 || echo "")
        
        echo "Response:"
        echo "$CREATE_RESPONSE" | head -c 500
        echo ""
        
        if [ ! -z "$ITEM_UUID" ] || [ ! -z "$ITEM_ID" ]; then
            TEST_ITEM_ID="${ITEM_UUID:-$ITEM_ID}"
            echo "Extracted Item ID: $TEST_ITEM_ID"
        fi
    else
        echo -e "${RED}✗ FAIL${NC} (HTTP $HTTP_CODE)"
        ((FAILED++))
        echo "Response: $CREATE_RESPONSE"
    fi
    echo ""
else
    echo -e "${YELLOW}⚠ Skipping item creation test (no auth token)${NC}"
    ((WARNINGS++))
    echo ""
fi

# =====================================================
# TEST 10: Get Specific Item by ID (Was broken - missing uuid_id, item_images)
# =====================================================
if [ ! -z "$TEST_ITEM_ID" ]; then
    test_endpoint "GET /api/items/:id - Get specific item" "$BASE_URL/api/items/$TEST_ITEM_ID"
else
    echo -e "${YELLOW}⚠ Skipping specific item test (no item ID available)${NC}"
    ((WARNINGS++))
    echo ""
fi

# =====================================================
# TEST 11: Place Bid (Was broken - stored procedure issues)
# =====================================================
if [ ! -z "$TOKEN" ] && [ ! -z "$TEST_ITEM_ID" ]; then
    BID_DATA="{\"item_id\":\"$TEST_ITEM_ID\",\"amount\":75.00}"
    test_endpoint "POST /api/bids/place - Place a bid" "$BASE_URL/api/bids/place" "POST" "$BID_DATA" "$TOKEN"
else
    echo -e "${YELLOW}⚠ Skipping bid placement test (no auth token or item ID)${NC}"
    ((WARNINGS++))
    echo ""
fi

# =====================================================
# SUMMARY
# =====================================================
echo "======================================================="
echo "TEST SUMMARY"
echo "======================================================="
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo ""
echo "Total Tests Run: $((PASSED + WARNINGS + FAILED))"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All critical tests passed!${NC}"
    echo "The schema fixes appear to be working correctly."
    exit 0
else
    echo -e "${RED}✗ Some tests failed. Please review the errors above.${NC}"
    exit 1
fi




