#!/bin/bash

# Test API Endpoints with Authentication
# This script tests the API endpoints with proper authentication

echo "======================================================="
echo "TESTING API ENDPOINTS WITH AUTHENTICATION"
echo "======================================================="
echo ""

# Set the base URL
BASE_URL="https://bidhub-android-app.onrender.com"

echo "Testing base URL: $BASE_URL"
echo ""

# Test 1: Login to get a token
echo "1. Testing login to get authentication token..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "miliwate@gmail.com",
    "password": "Admin123"
  }')

echo "Login response: $LOGIN_RESPONSE"

# Extract token from response
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "Failed to get authentication token"
    exit 1
fi

echo "Token extracted: ${TOKEN:0:50}..."
echo ""

# Test 2: Get items with authentication
echo "2. Testing GET /api/items with authentication..."
ITEMS_RESPONSE=$(curl -s -w "\nHTTP Status: %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  "$BASE_URL/api/items")

echo "Items response: $ITEMS_RESPONSE"
echo ""

# Test 3: Get specific item with authentication
echo "3. Testing GET /api/items/{uuid} with authentication..."
ITEM_UUID="e78fae88-b185-11f0-988a-52511f42de14"
ITEM_RESPONSE=$(curl -s -w "\nHTTP Status: %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  "$BASE_URL/api/items/$ITEM_UUID")

echo "Specific item response: $ITEM_RESPONSE"
echo ""

echo "======================================================="
echo "AUTHENTICATED API TESTING COMPLETE"
echo "======================================================="
echo ""
echo "If the authenticated requests work, the issue is in the Android app's"
echo "authentication token handling or API call implementation."
