#!/bin/bash

echo "🚀 Testing Render Deployment..."
echo "================================"

# Wait for deployment to complete
echo "⏳ Waiting for deployment to complete..."
sleep 30

# Test health endpoint
echo "🔍 Testing health endpoint..."
curl -s "https://bidhub-backend.onrender.com/api/health" | jq '.' || echo "❌ Health check failed"

echo ""
echo "📱 Next Steps:"
echo "1. Check your Render dashboard: https://render.com"
echo "2. Find your 'bidhub-backend' service"
echo "3. Copy the service URL"
echo "4. Update your Android app BASE_URL to use the Render URL"
echo ""
echo "🔧 Update these files in your Android app:"
echo "   - AuthApiClient.java (line 18)"
echo "   - ItemApiClient.java (line 18)"
echo ""
echo "Change from: http://192.168.18.136:3000/api"
echo "Change to:   https://your-app-name.onrender.com/api"
