#!/bin/bash

# Test API Endpoints Directly
# This script tests the API endpoints to see if they're working

echo "======================================================="
echo "TESTING API ENDPOINTS DIRECTLY"
echo "======================================================="
echo ""

# Set the base URL
BASE_URL="https://bidhub-android-app.onrender.com"

echo "Testing base URL: $BASE_URL"
echo ""

# Test 1: Health check
echo "1. Testing health endpoint..."
curl -s -w "\nHTTP Status: %{http_code}\n" "$BASE_URL/api/health" || echo "Health check failed"
echo ""

# Test 2: Get all items (this is what the Android app calls)
echo "2. Testing GET /api/items (what Android app calls)..."
curl -s -w "\nHTTP Status: %{http_code}\n" "$BASE_URL/api/items" || echo "Items endpoint failed"
echo ""

# Test 3: Get specific item by UUID
echo "3. Testing GET /api/items/{uuid} for specific item..."
ITEM_UUID="e78fae88-b185-11f0-988a-52511f42de14"
curl -s -w "\nHTTP Status: %{http_code}\n" "$BASE_URL/api/items/$ITEM_UUID" || echo "Specific item endpoint failed"
echo ""

# Test 4: Test with authentication (if we have a token)
echo "4. Testing with authentication..."
echo "Note: This will fail without a valid JWT token, but we can see the response structure"
curl -s -w "\nHTTP Status: %{http_code}\n" -H "Authorization: Bearer test-token" "$BASE_URL/api/items" || echo "Authenticated request failed"
echo ""

echo "======================================================="
echo "API ENDPOINT TESTING COMPLETE"
echo "======================================================="
echo ""
echo "If the items endpoint returns data, the issue is in the Android app."
echo "If the items endpoint returns empty or errors, the issue is in the backend."
