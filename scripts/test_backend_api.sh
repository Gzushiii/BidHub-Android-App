#!/bin/bash

# Test the backend API directly
echo "=== TESTING BACKEND API ==="

# Test 1: Check if backend is running
echo "1. Checking backend health..."
curl -s "https://bidhub-android-app.onrender.com/api/health" || echo "Backend not responding"

echo -e "\n2. Testing login to get auth token..."
# Login to get auth token
LOGIN_RESPONSE=$(curl -s -X POST "https://bidhub-android-app.onrender.com/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser444@example.com",
    "password": "password123"
  }')

echo "Login response: $LOGIN_RESPONSE"

# Extract token from response
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Extracted token: $TOKEN"

if [ -n "$TOKEN" ]; then
    echo -e "\n3. Testing credits balance..."
    curl -s -X GET "https://bidhub-android-app.onrender.com/api/credits/balance" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json"
    
    echo -e "\n4. Testing bid placement..."
    curl -s -X POST "https://bidhub-android-app.onrender.com/api/bids/place" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "item_id": 2,
        "amount": 101.00
      }'
else
    echo "Failed to get auth token"
fi
