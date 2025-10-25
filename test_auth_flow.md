# Authentication Flow Test Guide

## **🔍 Comprehensive Authentication Debugging**

This guide will help identify the exact failure point in the authentication flow.

### **📱 Android App Testing Steps**

1. **Open Android Studio** and run the app
2. **Open Logcat** and filter by these tags:
   - `SharedPreferencesHelper`
   - `BidApiClient`
   - `ItemDetailActivity`
   - `AuthApiClient`

3. **Test Login Flow:**
   - Login with valid credentials
   - Check logs for:
     ```
     SharedPreferencesHelper: === SAVE AUTH TOKEN (ALIAS) ===
     SharedPreferencesHelper: Token: [first 20 chars]...
     SharedPreferencesHelper: Token saved to SharedPreferences
     ```

4. **Test Token Retrieval:**
   - Navigate to an item detail page
   - Check logs for:
     ```
     SharedPreferencesHelper: === RETRIEVING AUTH TOKEN ===
     SharedPreferencesHelper: Retrieved token: [first 20 chars]...
     ```

5. **Test Bid Placement:**
   - Attempt to place a bid
   - Check logs for:
     ```
     BidApiClient: === BID API CLIENT DEBUG ===
     BidApiClient: Auth token: [first 20 chars]...
     BidApiClient: Full URL: https://bidhub-android-app.onrender.com/api/bids/place
     BidApiClient: Request body: {"item_id":"1","amount":300}
     BidApiClient: Response code: [status code]
     ```

6. **Test Buy Now:**
   - Attempt to use "Buy Now"
   - Check logs for:
     ```
     ItemDetailActivity: === BUY NOW DEBUG ===
     ItemDetailActivity: Auth token: [first 20 chars]...
     ItemDetailActivity: Buy now URL: https://bidhub-android-app.onrender.com/api/items/1/buy-now
     ItemDetailActivity: Response code: [status code]
     ```

### **🖥️ Backend Logs (Render)**

Check Render logs for:

1. **Authentication Middleware:**
   ```
   === AUTHENTICATION MIDDLEWARE DEBUG ===
   Authorization header: Bearer [token]
   Extracted token: [first 20 chars]...
   JWT_SECRET exists: true
   JWT verification successful, user: {id: 1, email: "...", ...}
   ```

2. **Bid Placement:**
   ```
   === BID PLACEMENT REQUEST RECEIVED ===
   Headers: {authorization: "Bearer [token]", ...}
   Request body: {item_id: "1", amount: 300}
   User from JWT: {id: 1, email: "...", ...}
   ```

3. **Buy Now:**
   ```
   === BUY NOW REQUEST RECEIVED ===
   Headers: {authorization: "Bearer [token]", ...}
   Request body: {amount: 500}
   User from JWT: {id: 1, email: "...", ...}
   ```

### **🚨 Expected Failure Scenarios**

#### **Scenario A: Token Not Stored**
- **Android Logs:** `SharedPreferencesHelper: Token: NULL`
- **Issue:** Login not saving token properly
- **Fix:** Check AuthApiClient login response parsing

#### **Scenario B: Token Not Retrieved**
- **Android Logs:** `SharedPreferencesHelper: Retrieved token: NULL`
- **Issue:** Token not persisted or cleared
- **Fix:** Check SharedPreferences storage/retrieval

#### **Scenario C: Token Not Sent**
- **Android Logs:** `BidApiClient: Auth token: NULL`
- **Issue:** Token not retrieved before API call
- **Fix:** Check token retrieval timing

#### **Scenario D: Backend Not Receiving Token**
- **Backend Logs:** `Authorization header: undefined`
- **Issue:** Network request not including Authorization header
- **Fix:** Check HttpURLConnection header setting

#### **Scenario E: JWT Verification Failed**
- **Backend Logs:** `JWT verification failed: [error message]`
- **Issue:** Token expired, invalid, or wrong secret
- **Fix:** Check JWT secret consistency

### **✅ Success Indicators**

- **Android:** Token stored and retrieved successfully
- **Android:** API calls include valid Authorization header
- **Backend:** Requests received with valid JWT
- **Backend:** JWT verification successful
- **Backend:** API endpoints process requests normally

### **🔧 Quick Fixes Based on Logs**

1. **If token is NULL in Android:** Check login response parsing
2. **If token not sent:** Check HttpURLConnection header setting
3. **If backend gets no token:** Check network request format
4. **If JWT verification fails:** Check JWT secret consistency
5. **If requests never reach backend:** Check network connectivity

### **📞 Next Steps**

After running these tests, share the logs to identify the exact failure point and implement the appropriate fix.
