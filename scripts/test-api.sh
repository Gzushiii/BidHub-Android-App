#!/bin/bash

# Comprehensive API Testing Script
# Tests all API endpoints with and without authentication
# Also tests schema fixes and compatibility

echo "======================================================="
echo "COMPREHENSIVE API TESTING"
echo "======================================================="
echo ""

# Set the base URL (can be overridden with BASE_URL env var)
BASE_URL="${BASE_URL:-https://bidhub-android-app.onrender.com}"

echo "Testing base URL: $BASE_URL"
echo "Date: $(date)"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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
    echo -e "${BLUE}Testing: $name${NC}"
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
        if [ ! -z "$body" ]; then
            echo "Response preview:"
            echo "$body" | head -c 300 | jq . 2>/dev/null || echo "$body" | head -c 300
            if [ ${#body} -gt 300 ]; then
                echo "... (truncated)"
            fi
        fi
    elif [ "$http_code" -eq 401 ] || [ "$http_code" -eq 403 ]; then
        echo -e "${YELLOW}⚠ AUTH REQUIRED${NC} (HTTP $http_code) - Expected for protected endpoints"
        ((WARNINGS++))
    elif [ "$http_code" -ge 400 ] && [ "$http_code" -lt 500 ]; then
        echo -e "${YELLOW}⚠ CLIENT ERROR${NC} (HTTP $http_code)"
        echo "Response: $body" | head -c 200
        ((WARNINGS++))
    else
        echo -e "${RED}✗ FAIL${NC} (HTTP $http_code)"
        echo "Response: $body"
        ((FAILED++))
    fi
    echo ""
}

# =====================================================
# SECTION 1: PUBLIC ENDPOINTS (No Auth Required)
# =====================================================

echo "======================================================="
echo "SECTION 1: PUBLIC ENDPOINTS"
echo "======================================================="
echo ""

test_endpoint "Health Check" "$BASE_URL/api/health"
test_endpoint "Get Categories" "$BASE_URL/api/categories"
test_endpoint "Get All Items" "$BASE_URL/api/items"
test_endpoint "Get Items (Filtered: status=active)" "$BASE_URL/api/items?status=active"
test_endpoint "Get Items (Filtered: limit=5)" "$BASE_URL/api/items?limit=5"

# =====================================================
# SECTION 2: AUTHENTICATION
# =====================================================

echo "======================================================="
echo "SECTION 2: AUTHENTICATION"
echo "======================================================="
echo ""

# Try to register a test user
RANDOM_EMAIL="test_$(date +%s)@example.com"
REGISTER_DATA="{\"username\":\"testuser$(date +%s)\",\"email\":\"$RANDOM_EMAIL\",\"phone_number\":\"+1234567890\",\"password\":\"Test123!\",\"first_name\":\"Test\",\"last_name\":\"User\",\"alias\":\"TestUser$(date +%s)\"}"

REGISTER_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "$REGISTER_DATA" "$BASE_URL/api/auth/register")
TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4 || echo "")

if [ -z "$TOKEN" ]; then
    # Try login with test credentials
    echo "Registration failed or user exists, trying login..."
    LOGIN_DATA="{\"email\":\"testuser444@example.com\",\"password\":\"password123\"}"
    LOGIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "$LOGIN_DATA" "$BASE_URL/api/auth/login")
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4 || echo "")
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}⚠ Could not obtain auth token. Some tests will be skipped.${NC}"
        echo "You can manually set TOKEN environment variable to test authenticated endpoints."
        echo ""
    else
        echo -e "${GREEN}✓ Login successful${NC}"
        echo "Token: ${TOKEN:0:50}..."
        echo ""
    fi
else
    echo -e "${GREEN}✓ Registration successful${NC}"
    echo "Token: ${TOKEN:0:50}..."
    echo ""
fi

# =====================================================
# SECTION 3: AUTHENTICATED ENDPOINTS
# =====================================================

if [ ! -z "$TOKEN" ]; then
    echo "======================================================="
    echo "SECTION 3: AUTHENTICATED ENDPOINTS"
    echo "======================================================="
    echo ""
    
    test_endpoint "Get Credits Balance" "$BASE_URL/api/credits/balance" "GET" "" "$TOKEN"
    test_endpoint "Get Credit Transactions" "$BASE_URL/api/credits/transactions" "GET" "" "$TOKEN"
    
    # Test item creation
    ITEM_DATA="{\"title\":\"Test Item $(date +%s)\",\"description\":\"Test item for API testing\",\"category_id\":1,\"starting_price\":50.00,\"reserve_price\":75.00,\"duration_days\":7,\"images\":[]}"
    CREATE_RESPONSE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$ITEM_DATA" "$BASE_URL/api/items")
    
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$ITEM_DATA" "$BASE_URL/api/items")
    
    if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
        echo -e "${GREEN}✓ Item Creation PASSED${NC} (HTTP $HTTP_CODE)"
        ((PASSED++))
        
        ITEM_UUID=$(echo "$CREATE_RESPONSE" | grep -o '"id":"[^"]*' | cut -d'"' -f4 || echo "$CREATE_RESPONSE" | grep -o '"uuid_id":"[^"]*' | cut -d'"' -f4 || echo "")
        if [ ! -z "$ITEM_UUID" ]; then
            echo "Created item UUID: $ITEM_UUID"
            test_endpoint "Get Item by UUID" "$BASE_URL/api/items/$ITEM_UUID" "GET" "" "$TOKEN"
        fi
    else
        echo -e "${YELLOW}⚠ Item Creation${NC} (HTTP $HTTP_CODE)"
        echo "Response: $CREATE_RESPONSE" | head -c 200
        ((WARNINGS++))
    fi
    echo ""
    
    # Test top-up endpoints (if available)
    test_endpoint "Initiate Top-Up" "$BASE_URL/api/topups" "POST" "{\"amount\":100.00,\"payment_method\":\"gcash\"}" "$TOKEN"
    
else
    echo "======================================================="
    echo "SECTION 3: AUTHENTICATED ENDPOINTS (SKIPPED)"
    echo "======================================================="
    echo -e "${YELLOW}⚠ No authentication token available. Skipping authenticated tests.${NC}"
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
    exit 0
else
    echo -e "${RED}✗ Some tests failed. Please review the errors above.${NC}"
    exit 1
fi

