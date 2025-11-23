# BidHub Android Frontend Design Document

## Overview

This document outlines the design for a new Java-only Android frontend application that integrates with the existing BidHub Node.js + MySQL backend API.

## Architecture Overview

### Design Principles
- **Simple & Clean**: Minimal dependencies, straightforward structure
- **API-First**: All data operations go through the backend API
- **Java-Only**: Pure Java implementation (no Kotlin)
- **RESTful Integration**: Direct mapping to backend API endpoints
- **JWT Authentication**: Token-based authentication with secure storage

### Architecture Layers

```
┌─────────────────────────────────────┐
│   Presentation Layer (Activities)   │
│   - LoginActivity                  │
│   - RegisterActivity               │
│   - MainActivity (Dashboard)        │
│   - BrowseItemsActivity            │
│   - ItemDetailActivity             │
│   - PostItemActivity               │
│   - CreditsActivity                │
│   - ProfileActivity                │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│   Business Logic Layer             │
│   - ApiClient (Base)               │
│   - AuthApiClient                 │
│   - ItemApiClient                 │
│   - BidApiClient                  │
│   - CreditsApiClient              │
│   - CategoryApiClient             │
│   - TopupApiClient                │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│   Data Layer                       │
│   - SharedPreferences (Token)     │
│   - Models (POJOs)                │
│   - Network (HTTP Client)          │
└─────────────────────────────────────┘
```

## Project Structure

```
bidhub-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cc106/bidhub/
│   │   │   │   ├── activities/
│   │   │   │   │   ├── LoginActivity.java
│   │   │   │   │   ├── RegisterActivity.java
│   │   │   │   │   ├── MainActivity.java
│   │   │   │   │   ├── BrowseItemsActivity.java
│   │   │   │   │   ├── ItemDetailActivity.java
│   │   │   │   │   ├── PostItemActivity.java
│   │   │   │   │   ├── CreditsActivity.java
│   │   │   │   │   └── ProfileActivity.java
│   │   │   │   ├── api/
│   │   │   │   │   ├── ApiClient.java (Base)
│   │   │   │   │   ├── AuthApiClient.java
│   │   │   │   │   ├── ItemApiClient.java
│   │   │   │   │   ├── BidApiClient.java
│   │   │   │   │   ├── CreditsApiClient.java
│   │   │   │   │   ├── CategoryApiClient.java
│   │   │   │   │   └── TopupApiClient.java
│   │   │   │   ├── models/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Item.java
│   │   │   │   │   ├── Bid.java
│   │   │   │   │   ├── Category.java
│   │   │   │   │   ├── CreditTransaction.java
│   │   │   │   │   └── Topup.java
│   │   │   │   ├── adapters/
│   │   │   │   │   ├── ItemAdapter.java
│   │   │   │   │   ├── BidAdapter.java
│   │   │   │   │   └── TransactionAdapter.java
│   │   │   │   ├── utils/
│   │   │   │   │   ├── Config.java
│   │   │   │   │   ├── TokenManager.java
│   │   │   │   │   ├── ImageUtils.java
│   │   │   │   │   └── DateUtils.java
│   │   │   │   └── fragments/
│   │   │   │       ├── HomeFragment.java
│   │   │   │       ├── BrowseFragment.java
│   │   │   │       └── ProfileFragment.java
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_login.xml
│   │   │   │   │   ├── activity_register.xml
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_browse.xml
│   │   │   │   │   ├── activity_item_detail.xml
│   │   │   │   │   ├── item_card.xml
│   │   │   │   │   └── ...
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── dimens.xml
│   │   │   │   └── drawable/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Core Components

### 1. Configuration (`Config.java`)

```java
package com.cc106.bidhub.utils;

public class Config {
    // Backend API Base URL
    // Change this to your Render deployment URL
    public static final String API_BASE_URL = "https://bidhub-backend.onrender.com/api";
    
    // API Endpoints
    public static final String ENDPOINT_AUTH_REGISTER = "/auth/register";
    public static final String ENDPOINT_AUTH_LOGIN = "/auth/login";
    public static final String ENDPOINT_ITEMS = "/items";
    public static final String ENDPOINT_BIDS = "/bids/place";
    public static final String ENDPOINT_CREDITS_BALANCE = "/credits/balance";
    public static final String ENDPOINT_CREDITS_TRANSACTIONS = "/credits/transactions";
    public static final String ENDPOINT_CREDITS_PURCHASE = "/credits/purchase";
    public static final String ENDPOINT_CATEGORIES = "/categories";
    public static final String ENDPOINT_TOPUPS = "/topups";
    
    // SharedPreferences Keys
    public static final String PREFS_NAME = "BidHubPrefs";
    public static final String KEY_AUTH_TOKEN = "auth_token";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_EMAIL = "user_email";
    
    // Request Timeouts
    public static final int CONNECT_TIMEOUT = 30000; // 30 seconds
    public static final int READ_TIMEOUT = 30000; // 30 seconds
}
```

### 2. Base API Client (`ApiClient.java`)

```java
package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.utils.Config;
import com.cc106.bidhub.utils.TokenManager;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiClient {
    protected Context context;
    protected String baseUrl;
    
    public ApiClient(Context context) {
        this.context = context;
        this.baseUrl = Config.API_BASE_URL;
    }
    
    protected HttpURLConnection createConnection(String endpoint, String method) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(Config.CONNECT_TIMEOUT);
        conn.setReadTimeout(Config.READ_TIMEOUT);
        
        // Add auth token if available
        String token = TokenManager.getToken(context);
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        
        return conn;
    }
    
    protected String sendRequest(HttpURLConnection conn, JSONObject body) throws Exception {
        if (body != null) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.flush();
            os.close();
        }
        
        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        if (responseCode >= 200 && responseCode < 300) {
            return response.toString();
        } else {
            throw new ApiException(responseCode, response.toString());
        }
    }
    
    public static class ApiException extends Exception {
        private int statusCode;
        private String response;
        
        public ApiException(int statusCode, String response) {
            super("API Error: " + statusCode);
            this.statusCode = statusCode;
            this.response = response;
        }
        
        public int getStatusCode() { return statusCode; }
        public String getResponse() { return response; }
    }
}
```

### 3. Token Manager (`TokenManager.java`)

```java
package com.cc106.bidhub.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static void saveToken(Context context, String token) {
        getPrefs(context).edit().putString(Config.KEY_AUTH_TOKEN, token).apply();
    }
    
    public static String getToken(Context context) {
        return getPrefs(context).getString(Config.KEY_AUTH_TOKEN, null);
    }
    
    public static void clearToken(Context context) {
        getPrefs(context).edit().remove(Config.KEY_AUTH_TOKEN).apply();
    }
    
    public static boolean isLoggedIn(Context context) {
        return getToken(context) != null;
    }
}
```

### 4. Auth API Client (`AuthApiClient.java`)

```java
package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.models.User;
import com.cc106.bidhub.utils.Config;
import com.cc106.bidhub.utils.TokenManager;
import org.json.JSONObject;
import java.net.HttpURLConnection;

public class AuthApiClient extends ApiClient {
    
    public AuthApiClient(Context context) {
        super(context);
    }
    
    public User register(String username, String email, String phoneNumber, 
                        String password, String firstName, String lastName, 
                        String alias) throws Exception {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("email", email);
        body.put("phone_number", phoneNumber);
        body.put("password", password);
        body.put("first_name", firstName);
        body.put("last_name", lastName);
        body.put("alias", alias);
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_AUTH_REGISTER, "POST");
        String response = sendRequest(conn, body);
        
        JSONObject json = new JSONObject(response);
        String token = json.getString("token");
        TokenManager.saveToken(context, token);
        
        JSONObject userJson = json.getJSONObject("user");
        return parseUser(userJson);
    }
    
    public User login(String email, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_AUTH_LOGIN, "POST");
        String response = sendRequest(conn, body);
        
        JSONObject json = new JSONObject(response);
        String token = json.getString("token");
        TokenManager.saveToken(context, token);
        
        JSONObject userJson = json.getJSONObject("user");
        return parseUser(userJson);
    }
    
    public void logout() {
        TokenManager.clearToken(context);
    }
    
    private User parseUser(JSONObject json) throws Exception {
        User user = new User();
        user.setId(json.getInt("id"));
        user.setUsername(json.getString("username"));
        user.setEmail(json.getString("email"));
        user.setFirstName(json.getString("first_name"));
        user.setLastName(json.getString("last_name"));
        user.setAlias(json.getString("alias"));
        user.setCredits(json.getDouble("credits"));
        return user;
    }
}
```

### 5. Item API Client (`ItemApiClient.java`)

```java
package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.models.Item;
import com.cc106.bidhub.utils.Config;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class ItemApiClient extends ApiClient {
    
    public ItemApiClient(Context context) {
        super(context);
    }
    
    public List<Item> getItems(String status, Integer categoryId, String search, 
                              Integer minPrice, Integer maxPrice, 
                              Integer limit, Integer offset) throws Exception {
        StringBuilder endpoint = new StringBuilder(Config.ENDPOINT_ITEMS);
        List<String> params = new ArrayList<>();
        
        if (status != null) params.add("status=" + status);
        if (categoryId != null) params.add("category_id=" + categoryId);
        if (search != null) params.add("search=" + search);
        if (minPrice != null) params.add("min_price=" + minPrice);
        if (maxPrice != null) params.add("max_price=" + maxPrice);
        if (limit != null) params.add("limit=" + limit);
        if (offset != null) params.add("offset=" + offset);
        
        if (!params.isEmpty()) {
            endpoint.append("?").append(String.join("&", params));
        }
        
        HttpURLConnection conn = createConnection(endpoint.toString(), "GET");
        String response = sendRequest(conn, null);
        
        JSONObject json = new JSONObject(response);
        JSONArray itemsArray = json.getJSONArray("items");
        List<Item> items = new ArrayList<>();
        
        for (int i = 0; i < itemsArray.length(); i++) {
            items.add(parseItem(itemsArray.getJSONObject(i)));
        }
        
        return items;
    }
    
    public Item getItemById(String itemId) throws Exception {
        HttpURLConnection conn = createConnection(Config.ENDPOINT_ITEMS + "/" + itemId, "GET");
        String response = sendRequest(conn, null);
        return parseItem(new JSONObject(response));
    }
    
    public Item createItem(String title, String description, Integer categoryId, 
                          Double startingPrice, Double reservePrice, 
                          Integer durationDays, List<String> images) throws Exception {
        JSONObject body = new JSONObject();
        body.put("title", title);
        body.put("description", description);
        body.put("category_id", categoryId);
        body.put("starting_price", startingPrice);
        if (reservePrice != null) body.put("reserve_price", reservePrice);
        if (durationDays != null) body.put("duration_days", durationDays);
        if (images != null && !images.isEmpty()) {
            JSONArray imagesArray = new JSONArray();
            for (String image : images) {
                imagesArray.put(image);
            }
            body.put("images", imagesArray);
        }
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_ITEMS, "POST");
        String response = sendRequest(conn, body);
        
        JSONObject json = new JSONObject(response);
        return parseItem(json.getJSONObject("item"));
    }
    
    public void publishItem(String itemId, Integer durationDays) throws Exception {
        JSONObject body = new JSONObject();
        if (durationDays != null) body.put("duration_days", durationDays);
        
        HttpURLConnection conn = createConnection(
            Config.ENDPOINT_ITEMS + "/" + itemId + "/publish", "POST");
        sendRequest(conn, body);
    }
    
    public void buyNow(String itemId, Double amount) throws Exception {
        JSONObject body = new JSONObject();
        if (amount != null) body.put("amount", amount);
        
        HttpURLConnection conn = createConnection(
            Config.ENDPOINT_ITEMS + "/" + itemId + "/buy-now", "POST");
        sendRequest(conn, body);
    }
    
    private Item parseItem(JSONObject json) throws Exception {
        Item item = new Item();
        if (json.has("id")) item.setId(json.getString("id"));
        if (json.has("uuid_id")) item.setUuidId(json.getString("uuid_id"));
        if (json.has("title")) item.setTitle(json.getString("title"));
        if (json.has("description")) item.setDescription(json.getString("description"));
        if (json.has("category_id")) item.setCategoryId(json.getInt("category_id"));
        if (json.has("starting_bid")) item.setStartingBid(json.getDouble("starting_bid"));
        if (json.has("current_bid")) item.setCurrentBid(json.getDouble("current_bid"));
        if (json.has("buy_now_price")) item.setBuyNowPrice(json.getDouble("buy_now_price"));
        if (json.has("status")) item.setStatus(json.getString("status"));
        if (json.has("end_date")) item.setEndDate(json.getString("end_date"));
        if (json.has("images")) {
            JSONArray imagesArray = json.getJSONArray("images");
            List<String> images = new ArrayList<>();
            for (int i = 0; i < imagesArray.length(); i++) {
                images.add(imagesArray.getString(i));
            }
            item.setImages(images);
        }
        return item;
    }
}
```

### 6. Bid API Client (`BidApiClient.java`)

```java
package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.utils.Config;
import org.json.JSONObject;
import java.net.HttpURLConnection;

public class BidApiClient extends ApiClient {
    
    public BidApiClient(Context context) {
        super(context);
    }
    
    public void placeBid(String itemId, Double amount) throws Exception {
        JSONObject body = new JSONObject();
        body.put("item_id", itemId);
        body.put("amount", amount);
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_BIDS, "POST");
        sendRequest(conn, body);
    }
}
```

### 7. Credits API Client (`CreditsApiClient.java`)

```java
package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.models.CreditTransaction;
import com.cc106.bidhub.utils.Config;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class CreditsApiClient extends ApiClient {
    
    public CreditsApiClient(Context context) {
        super(context);
    }
    
    public Double getBalance() throws Exception {
        HttpURLConnection conn = createConnection(Config.ENDPOINT_CREDITS_BALANCE, "GET");
        String response = sendRequest(conn, null);
        JSONObject json = new JSONObject(response);
        return json.getDouble("credits");
    }
    
    public List<CreditTransaction> getTransactions(String type, String status, 
                                                   Integer limit, Integer offset) throws Exception {
        StringBuilder endpoint = new StringBuilder(Config.ENDPOINT_CREDITS_TRANSACTIONS);
        List<String> params = new ArrayList<>();
        
        if (type != null) params.add("type=" + type);
        if (status != null) params.add("status=" + status);
        if (limit != null) params.add("limit=" + limit);
        if (offset != null) params.add("offset=" + offset);
        
        if (!params.isEmpty()) {
            endpoint.append("?").append(String.join("&", params));
        }
        
        HttpURLConnection conn = createConnection(endpoint.toString(), "GET");
        String response = sendRequest(conn, null);
        
        JSONObject json = new JSONObject(response);
        JSONArray transactionsArray = json.getJSONArray("transactions");
        List<CreditTransaction> transactions = new ArrayList<>();
        
        for (int i = 0; i < transactionsArray.length(); i++) {
            transactions.add(parseTransaction(transactionsArray.getJSONObject(i)));
        }
        
        return transactions;
    }
    
    public void purchaseCredits(Double amount, String paymentMethod, String transactionId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("amount", amount);
        body.put("payment_method", paymentMethod);
        body.put("transaction_id", transactionId);
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_CREDITS_PURCHASE, "POST");
        sendRequest(conn, body);
    }
    
    private CreditTransaction parseTransaction(JSONObject json) throws Exception {
        CreditTransaction transaction = new CreditTransaction();
        if (json.has("id")) transaction.setId(json.getInt("id"));
        if (json.has("type")) transaction.setType(json.getString("type"));
        if (json.has("amount")) transaction.setAmount(json.getDouble("amount"));
        if (json.has("status")) transaction.setStatus(json.getString("status"));
        if (json.has("created_at")) transaction.setCreatedAt(json.getString("created_at"));
        return transaction;
    }
}
```

### 8. Category API Client (`CategoryApiClient.java`)

```java
package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.models.Category;
import com.cc106.bidhub.utils.Config;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class CategoryApiClient extends ApiClient {
    
    public CategoryApiClient(Context context) {
        super(context);
    }
    
    public List<Category> getCategories() throws Exception {
        HttpURLConnection conn = createConnection(Config.ENDPOINT_CATEGORIES, "GET");
        String response = sendRequest(conn, null);
        
        JSONObject json = new JSONObject(response);
        JSONArray categoriesArray = json.getJSONArray("categories");
        List<Category> categories = new ArrayList<>();
        
        for (int i = 0; i < categoriesArray.length(); i++) {
            categories.add(parseCategory(categoriesArray.getJSONObject(i)));
        }
        
        return categories;
    }
    
    private Category parseCategory(JSONObject json) throws Exception {
        Category category = new Category();
        if (json.has("id")) category.setId(json.getInt("id"));
        if (json.has("name")) category.setName(json.getString("name"));
        if (json.has("description")) category.setDescription(json.getString("description"));
        if (json.has("parent_id")) category.setParentId(json.getInt("parent_id"));
        if (json.has("subcategories")) {
            JSONArray subcategoriesArray = json.getJSONArray("subcategories");
            List<Category> subcategories = new ArrayList<>();
            for (int i = 0; i < subcategoriesArray.length(); i++) {
                subcategories.add(parseCategory(subcategoriesArray.getJSONObject(i)));
            }
            category.setSubcategories(subcategories);
        }
        return category;
    }
}
```

### 9. Topup API Client (`TopupApiClient.java`)

```java
package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.models.Topup;
import com.cc106.bidhub.utils.Config;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class TopupApiClient extends ApiClient {
    
    public TopupApiClient(Context context) {
        super(context);
    }
    
    public Topup initiateTopup(Double amount, String paymentMethod) throws Exception {
        JSONObject body = new JSONObject();
        body.put("amount", amount);
        body.put("payment_method", paymentMethod);
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_TOPUPS, "POST");
        String response = sendRequest(conn, body);
        
        return parseTopup(new JSONObject(response));
    }
    
    public void submitReceipt(Integer topupId, String receiptRef) throws Exception {
        JSONObject body = new JSONObject();
        body.put("user_receipt_ref", receiptRef);
        
        HttpURLConnection conn = createConnection(
            Config.ENDPOINT_TOPUPS + "/" + topupId + "/submit", "POST");
        sendRequest(conn, body);
    }
    
    public Topup getTopup(Integer topupId) throws Exception {
        HttpURLConnection conn = createConnection(
            Config.ENDPOINT_TOPUPS + "/" + topupId, "GET");
        String response = sendRequest(conn, null);
        return parseTopup(new JSONObject(response));
    }
    
    public List<Topup> getTopups(String status, Integer limit, Integer offset) throws Exception {
        StringBuilder endpoint = new StringBuilder(Config.ENDPOINT_TOPUPS);
        List<String> params = new ArrayList<>();
        
        if (status != null) params.add("status=" + status);
        if (limit != null) params.add("limit=" + limit);
        if (offset != null) params.add("offset=" + offset);
        
        if (!params.isEmpty()) {
            endpoint.append("?").append(String.join("&", params));
        }
        
        HttpURLConnection conn = createConnection(endpoint.toString(), "GET");
        String response = sendRequest(conn, null);
        
        JSONObject json = new JSONObject(response);
        JSONArray topupsArray = json.getJSONArray("topups");
        List<Topup> topups = new ArrayList<>();
        
        for (int i = 0; i < topupsArray.length(); i++) {
            topups.add(parseTopup(topupsArray.getJSONObject(i)));
        }
        
        return topups;
    }
    
    private Topup parseTopup(JSONObject json) throws Exception {
        Topup topup = new Topup();
        if (json.has("id")) topup.setId(json.getInt("id"));
        if (json.has("amount")) topup.setAmount(json.getDouble("amount"));
        if (json.has("generated_ref")) topup.setGeneratedRef(json.getString("generated_ref"));
        if (json.has("payment_method")) topup.setPaymentMethod(json.getString("payment_method"));
        if (json.has("status")) topup.setStatus(json.getString("status"));
        if (json.has("instructions")) topup.setInstructions(json.getString("instructions"));
        return topup;
    }
}
```

## Data Models

### User Model

```java
package com.cc106.bidhub.models;

public class User {
    private int id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String alias;
    private double credits;
    
    // Getters and setters
    // ...
}
```

### Item Model

```java
package com.cc106.bidhub.models;

import java.util.List;

public class Item {
    private String id;
    private String uuidId;
    private String title;
    private String description;
    private int categoryId;
    private double startingBid;
    private double currentBid;
    private Double buyNowPrice;
    private String status;
    private String endDate;
    private List<String> images;
    
    // Getters and setters
    // ...
}
```

## Activity Flow

### 1. Login/Register Flow
```
LoginActivity → AuthApiClient.login() → Save Token → Navigate to MainActivity
RegisterActivity → AuthApiClient.register() → Save Token → Navigate to MainActivity
```

### 2. Browse Items Flow
```
MainActivity → BrowseItemsActivity → ItemApiClient.getItems() → Display Items
→ ItemDetailActivity → ItemApiClient.getItemById() → Display Details
```

### 3. Post Item Flow
```
MainActivity → PostItemActivity → ItemApiClient.createItem() → 
ItemApiClient.publishItem() → Success
```

### 4. Bidding Flow
```
ItemDetailActivity → BidApiClient.placeBid() → Refresh Item Details
```

### 5. Credits Flow
```
MainActivity → CreditsActivity → CreditsApiClient.getBalance() → 
TopupApiClient.initiateTopup() → Submit Receipt → Wait for Confirmation
```

## Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Core Android
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // RecyclerView for lists
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    
    // CardView for item cards
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Image loading (optional, for loading item images)
    implementation("com.github.bumptech.glide:glide:4.15.1")
    
    // JSON parsing (optional, can use org.json)
    // implementation("com.google.code.gson:gson:2.10.1")
}
```

## Network Security Configuration

Create `res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">bidhub-backend.onrender.com</domain>
    </domain-config>
</network-security-config>
```

Update `AndroidManifest.xml`:

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

## Error Handling

All API clients should handle:
- Network errors (no internet)
- HTTP errors (400, 401, 403, 404, 500)
- JSON parsing errors
- Timeout errors

Use try-catch blocks in Activities and show user-friendly error messages.

## Authentication Flow

1. User logs in/registers
2. Token saved to SharedPreferences
3. Token included in all subsequent API requests via `Authorization: Bearer <token>` header
4. If token expires (401), redirect to LoginActivity
5. Logout clears token

## Key Features to Implement

### MVP Features (Phase 1)
1. ✅ User Registration & Login
2. ✅ Browse Items (List & Detail)
3. ✅ Post Items (Create & Publish)
4. ✅ Place Bids
5. ✅ View Credits Balance
6. ✅ Manual Top-up Flow
7. ✅ View Categories

### Future Features (Phase 2)
- Real-time bid updates (WebSocket)
- Push notifications
- Image upload
- Search & filters
- User profile management

## Testing Strategy

1. **Unit Tests**: Test API clients with mock responses
2. **Integration Tests**: Test API integration with test backend
3. **UI Tests**: Test activity flows and user interactions

## Security Considerations

1. **Token Storage**: Use SharedPreferences (consider encryption for production)
2. **HTTPS Only**: All API calls must use HTTPS
3. **Input Validation**: Validate all user inputs before sending to API
4. **Error Messages**: Don't expose sensitive error details to users

## Performance Considerations

1. **Image Loading**: Use Glide or similar library for efficient image loading
2. **Pagination**: Implement pagination for item lists
3. **Caching**: Cache categories and user data locally
4. **Background Threads**: All API calls should be on background threads

## Next Steps

1. Create Android project structure
2. Implement base API client
3. Implement authentication flow
4. Implement item browsing
5. Implement bidding functionality
6. Implement credits and top-up
7. Add UI polish and error handling
8. Test with backend API

