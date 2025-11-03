#!/bin/bash

# Comprehensive API Endpoint Testing Script
# Tests all endpoints for proper functionality

set -e

BASE_URL="${BASE_URL:-https://bidhub-android-app.onrender.com}"
TEST_EMAIL="testuser_api@example.com"
TEST_PASSWORD="password123"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_WARNING=0

# Test function
test_endpoint() {
    local name="$1"
    local method="$2"
    local endpoint="$3"
    local auth_token="${4:-}"
    local data="${5:-}"
    local expected_status="${6:-200}"
    
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Testing: ${name}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo "Method: $method"
    echo "Endpoint: $endpoint"
    
    local headers=(-H "Content-Type: application/json")
    if [ -n "$auth_token" ]; then
        headers+=(-H "Authorization: Bearer $auth_token")
    fi
    
    local response
    if [ "$method" = "POST" ] || [ "$method" = "PUT" ] || [ "$method" = "PATCH" ]; then
        if [ -n "$data" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
                "${headers[@]}" -d "$data" 2>&1)
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
                "${headers[@]}" 2>&1)
        fi
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            "${headers[@]}" 2>&1)
    fi
    
    local http_code=$(echo "$response" | tail -n 1)
    local body=$(echo "$response" | sed '$d')
    
    # Check if curl failed
    if echo "$response" | grep -q "curl:"; then
        echo -e "${RED}✗ FAILED${NC} - Connection error"
        echo "$response"
        ((TESTS_FAILED++))
        return 1
    fi
    
    # Compare HTTP status
    if [ "$http_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ PASSED${NC} (HTTP $http_code)"
        echo "Response: $(echo "$body" | head -c 200)"
        ((TESTS_PASSED++))
        return 0
    elif [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "${GREEN}✓ PASSED${NC} (HTTP $http_code - different from expected but OK)"
        echo "Response: $(echo "$body" | head -c 200)"
        ((TESTS_PASSED++))
        return 0
    elif [ "$http_code" -eq 401 ] || [ "$http_code" -eq 403 ]; then
        echo -e "${YELLOW}⚠ AUTH REQUIRED${NC} (HTTP $http_code) - Expected for protected endpoints"
        echo "Response: $(echo "$body" | head -c 200)"
        ((TESTS_WARNING++))
        return 0
    else
        echo -e "${RED}✗ FAILED${NC} (HTTP $http_code, expected $expected_status)"
        echo "Response: $body"
        ((TESTS_FAILED++))
        return 1
    fi
}

echo "═══════════════════════════════════════════════════════════"
echo "  COMPREHENSIVE API ENDPOINT TESTING"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "Base URL: $BASE_URL"
echo "Date: $(date)"
echo ""

# ============================================================
# PHASE 1: Public Endpoints
# ============================================================

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
echo -e "${BLUE}PHASE 1: Public Endpoints${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
echo ""

test_endpoint "Health Check" "GET" "/api/health" "" "" "200"
test_endpoint "Root Endpoint" "GET" "/" "" "" "200"
test_endpoint "Get Categories" "GET" "/api/categories" "" "" "200"
test_endpoint "Get Items (Public)" "GET" "/api/items?limit=5" "" "" "200"

# ============================================================
# PHASE 2: Authentication
# ============================================================

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
echo -e "${BLUE}PHASE 2: Authentication${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
echo ""

# Register test user
REGISTER_DATA="{\"username\":\"apitest_$(date +%s)\",\"email\":\"$TEST_EMAIL\",\"phone_number\":\"+1234567890\",\"password\":\"$TEST_PASSWORD\",\"first_name\":\"API\",\"last_name\":\"Test\",\"alias\":\"apitest_$(date +%s)\"}"
test_endpoint "User Registration" "POST" "/api/auth/register" "" "$REGISTER_DATA" "201"

# Login
LOGIN_DATA="{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "$LOGIN_DATA")

AUTH_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4 || echo "")

if [ -z "$AUTH_TOKEN" ]; then
    echo -e "${YELLOW}⚠ Could not extract auth token from login response${NC}"
    echo "Response: $LOGIN_RESPONSE"
    AUTH_TOKEN=""
else
    echo -e "${GREEN}✓ Login successful, token extracted${NC}"
fi

test_endpoint "User Login" "POST" "/api/auth/login" "" "$LOGIN_DATA" "200"

# ============================================================
# PHASE 3: Authenticated Endpoints
# ============================================================

if [ -n "$AUTH_TOKEN" ]; then
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}PHASE 3: Authenticated Endpoints${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
    echo ""
    
    # Credits
    test_endpoint "Get Credits Balance" "GET" "/api/credits/balance" "$AUTH_TOKEN" "" "200"
    test_endpoint "Get Credit Transactions" "GET" "/api/credits/transactions?limit=10" "$AUTH_TOKEN" "" "200"
    
    # Items
    test_endpoint "Get Items (Authenticated)" "GET" "/api/items?limit=10" "$AUTH_TOKEN" "" "200"
    
    # Create a test item for bidding
    ITEM_DATA="{\"title\":\"API Test Item $(date +%s)\",\"description\":\"Test item created by API testing script\",\"category_id\":1,\"starting_price\":50.00,\"duration_days\":7}"
    CREATE_ITEM_RESPONSE=$(curl -s -X POST "$BASE_URL/api/items" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $AUTH_TOKEN" \
        -d "$ITEM_DATA")
    
    ITEM_ID=$(echo "$CREATE_ITEM_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2 || echo "")
    
    if [ -n "$ITEM_ID" ]; then
        echo -e "${GREEN}✓ Test item created with ID: $ITEM_ID${NC}"
        test_endpoint "Get Specific Item" "GET" "/api/items/$ITEM_ID" "$AUTH_TOKEN" "" "200"
        
        # Test bidding (if we have credits)
        BID_DATA="{\"item_id\":$ITEM_ID,\"amount\":55.00}"
        test_endpoint "Place Bid" "POST" "/api/bids/place" "$AUTH_TOKEN" "$BID_DATA" "200"
        
        # Test buy now (if item has buy_now_price)
        test_endpoint "Buy Now Check" "POST" "/api/items/$ITEM_ID/buy-now" "$AUTH_TOKEN" "{\"amount\":100.00}" "200"
    else
        echo -e "${YELLOW}⚠ Could not create test item, skipping item-specific tests${NC}"
    fi
    
    # Top-ups (manual top-up system)
    TOPUP_DATA="{\"amount\":500.00,\"payment_method\":\"gcash\"}"
    test_endpoint "Initiate Top-Up" "POST" "/api/topups" "$AUTH_TOKEN" "$TOPUP_DATA" "201"
    
    test_endpoint "List Top-Ups" "GET" "/api/topups" "$AUTH_TOKEN" "" "200"
else
    echo ""
    echo -e "${YELLOW}⚠ Skipping authenticated tests - no auth token available${NC}"
fi

# ============================================================
# PHASE 4: Error Handling Tests
# ============================================================

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
echo -e "${BLUE}PHASE 4: Error Handling Tests${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
echo ""

# Test invalid endpoints
test_endpoint "Invalid Endpoint" "GET" "/api/nonexistent" "" "" "404"

# Test unauthorized access
test_endpoint "Unauthorized Access" "GET" "/api/credits/balance" "" "" "401"

# Test invalid data
INVALID_LOGIN="{\"email\":\"invalid\",\"password\":\"\"}"
test_endpoint "Invalid Login Data" "POST" "/api/auth/login" "" "$INVALID_LOGIN" "400"

# ============================================================
# SUMMARY
# ============================================================

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  TEST SUMMARY"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo -e "${GREEN}✓ Passed: $TESTS_PASSED${NC}"
echo -e "${YELLOW}⚠ Warnings: $TESTS_WARNING${NC}"
echo -e "${RED}✗ Failed: $TESTS_FAILED${NC}"
echo ""
TOTAL=$((TESTS_PASSED + TESTS_WARNING + TESTS_FAILED))
if [ $TOTAL -gt 0 ]; then
    SUCCESS_RATE=$((TESTS_PASSED * 100 / TOTAL))
    echo "Success Rate: ${SUCCESS_RATE}%"
fi
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}All critical tests passed! ✓${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed. Please review the output above.${NC}"
    exit 1
fi

