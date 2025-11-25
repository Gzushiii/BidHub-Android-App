#!/bin/bash

# Quick Backend Status Check
# This script tests if the backend is online and responsive

echo "========================================="
echo "Backend Status Check"
echo "========================================="
echo ""

BASE_URL="https://bidhub-android-app.onrender.com"

echo "1. Testing Health Endpoint..."
echo "----------------------------------------"
time curl -s "${BASE_URL}/api/health" \
  -w "\nResponse Code: %{http_code}\nTime: %{time_total}s\n" \
  --max-time 30

echo ""
echo "2. Testing Auth Login Endpoint..."
echo "----------------------------------------"
time curl -s "${BASE_URL}/api/auth/login" \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test"}' \
  -w "\nResponse Code: %{http_code}\nTime: %{time_total}s\n" \
  --max-time 30

echo ""
echo "3. Testing with Real User Account..."
echo "----------------------------------------"
read -p "Enter email: " email
read -sp "Enter password: " password
echo ""
time curl -s "${BASE_URL}/api/auth/login" \
  -X POST \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${email}\",\"password\":\"${password}\"}" \
  -w "\nResponse Code: %{http_code}\nTime: %{time_total}s\n" \
  --max-time 60 | jq . 2>/dev/null || cat

echo ""
echo "========================================="
echo "Check Complete"
echo "========================================="
echo ""

# Interpret results
echo "Expected Results:"
echo "- First request: 20-30 seconds (cold start on free tier)"
echo "- Second request: < 2 seconds (warmed up)"
echo "- Response code 401 = invalid credentials (backend working)"
echo "- Response code 500 = backend error"
echo "- No response = backend down or network issue"

