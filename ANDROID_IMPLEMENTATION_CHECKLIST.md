# Android Frontend Implementation Checklist

## Quick Reference

### Backend API Base URL
```
https://bidhub-backend.onrender.com/api
```

### Authentication
- **Method**: JWT Bearer Token
- **Header**: `Authorization: Bearer <token>`
- **Token Storage**: SharedPreferences
- **Token Expiry**: 7 days

### API Endpoints Summary

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/auth/register` | POST | No | Register new user |
| `/auth/login` | POST | No | User login |
| `/items` | GET | No | List items (with filters) |
| `/items/:id` | GET | No | Get item details |
| `/items` | POST | Yes | Create new item |
| `/items/:id/publish` | POST | Yes | Publish draft item |
| `/items/:id/buy-now` | POST | Yes | Buy item immediately |
| `/bids/place` | POST | Yes | Place a bid |
| `/credits/balance` | GET | Yes | Get credit balance |
| `/credits/transactions` | GET | Yes | Get transaction history |
| `/credits/purchase` | POST | Yes | Purchase credits (legacy) |
| `/categories` | GET | No | Get all categories |
| `/topups` | POST | Yes | Initiate top-up |
| `/topups/:id/submit` | POST | Yes | Submit receipt |
| `/topups/:id` | GET | Yes | Get top-up status |
| `/topups` | GET | Yes | List user's top-ups |

## Implementation Phases

### Phase 1: Foundation (Week 1)
- [ ] Create Android project structure
- [ ] Set up Gradle dependencies
- [ ] Create `Config.java` with API base URL
- [ ] Create `TokenManager.java` for token storage
- [ ] Create base `ApiClient.java` with HTTP connection logic
- [ ] Create `ApiException.java` for error handling
- [ ] Test base API client with health endpoint

### Phase 2: Authentication (Week 1-2)
- [ ] Create `User.java` model
- [ ] Create `AuthApiClient.java`
- [ ] Create `LoginActivity.java` with UI
- [ ] Create `RegisterActivity.java` with UI
- [ ] Implement login flow
- [ ] Implement registration flow
- [ ] Add token validation on app start
- [ ] Test authentication with backend

### Phase 3: Item Browsing (Week 2)
- [ ] Create `Item.java` model
- [ ] Create `Category.java` model
- [ ] Create `ItemApiClient.java`
- [ ] Create `CategoryApiClient.java`
- [ ] Create `ItemAdapter.java` for RecyclerView
- [ ] Create `BrowseItemsActivity.java` with RecyclerView
- [ ] Create `ItemDetailActivity.java`
- [ ] Implement item list display
- [ ] Implement item detail view
- [ ] Add image loading (Glide)
- [ ] Test item browsing

### Phase 4: Item Posting (Week 2-3)
- [ ] Create `PostItemActivity.java`
- [ ] Add category selection
- [ ] Add image selection (optional)
- [ ] Implement item creation
- [ ] Implement draft saving
- [ ] Implement item publishing
- [ ] Add validation
- [ ] Test item posting flow

### Phase 5: Bidding (Week 3)
- [ ] Create `BidApiClient.java`
- [ ] Add bid button to `ItemDetailActivity`
- [ ] Implement bid placement
- [ ] Add bid validation (amount > current bid)
- [ ] Show bid success/error messages
- [ ] Refresh item details after bid
- [ ] Test bidding flow

### Phase 6: Credits & Top-up (Week 3-4)
- [ ] Create `CreditTransaction.java` model
- [ ] Create `Topup.java` model
- [ ] Create `CreditsApiClient.java`
- [ ] Create `TopupApiClient.java`
- [ ] Create `CreditsActivity.java`
- [ ] Display credit balance
- [ ] Display transaction history
- [ ] Implement top-up initiation
- [ ] Implement receipt submission
- [ ] Add top-up status tracking
- [ ] Test credits and top-up flow

### Phase 7: Main Dashboard (Week 4)
- [ ] Create `MainActivity.java` with bottom navigation
- [ ] Create `HomeFragment.java`
- [ ] Create `BrowseFragment.java`
- [ ] Create `ProfileFragment.java`
- [ ] Implement navigation between screens
- [ ] Add user profile display
- [ ] Test main navigation

### Phase 8: Polish & Testing (Week 4-5)
- [ ] Add loading indicators
- [ ] Add error handling and user-friendly messages
- [ ] Add pull-to-refresh
- [ ] Add pagination for item lists
- [ ] Optimize image loading
- [ ] Test on different screen sizes
- [ ] Test with slow network
- [ ] Fix bugs and edge cases

## Code Structure Checklist

### Utils Package
- [ ] `Config.java` - API configuration
- [ ] `TokenManager.java` - Token storage
- [ ] `ImageUtils.java` - Image helper functions
- [ ] `DateUtils.java` - Date formatting

### API Package
- [ ] `ApiClient.java` - Base API client
- [ ] `AuthApiClient.java` - Authentication
- [ ] `ItemApiClient.java` - Items
- [ ] `BidApiClient.java` - Bidding
- [ ] `CreditsApiClient.java` - Credits
- [ ] `CategoryApiClient.java` - Categories
- [ ] `TopupApiClient.java` - Top-ups

### Models Package
- [ ] `User.java`
- [ ] `Item.java`
- [ ] `Bid.java`
- [ ] `Category.java`
- [ ] `CreditTransaction.java`
- [ ] `Topup.java`

### Activities Package
- [ ] `LoginActivity.java`
- [ ] `RegisterActivity.java`
- [ ] `MainActivity.java`
- [ ] `BrowseItemsActivity.java`
- [ ] `ItemDetailActivity.java`
- [ ] `PostItemActivity.java`
- [ ] `CreditsActivity.java`
- [ ] `ProfileActivity.java`

### Adapters Package
- [ ] `ItemAdapter.java`
- [ ] `BidAdapter.java`
- [ ] `TransactionAdapter.java`

## Request/Response Examples

### Register Request
```json
POST /api/auth/register
{
  "username": "johndoe",
  "email": "john@example.com",
  "phone_number": "+639123456789",
  "password": "SecurePass123!",
  "first_name": "John",
  "last_name": "Doe",
  "alias": "johndoe123"
}
```

### Register Response
```json
{
  "message": "User registered successfully",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "alias": "johndoe123",
    "credits": 100.00
  }
}
```

### Get Items Request
```
GET /api/items?status=active&limit=20&offset=0
```

### Get Items Response
```json
{
  "items": [
    {
      "id": "uuid-here",
      "title": "Vintage Watch",
      "description": "Beautiful vintage watch",
      "category_id": 1,
      "starting_bid": 100.00,
      "current_bid": 150.00,
      "status": "active",
      "end_date": "2025-12-01T00:00:00Z"
    }
  ],
  "count": 1,
  "total": 1,
  "limit": 20,
  "offset": 0
}
```

### Place Bid Request
```json
POST /api/bids/place
Authorization: Bearer <token>
{
  "item_id": "uuid-here",
  "amount": 200.00
}
```

### Place Bid Response
```json
{
  "message": "Bid placed successfully",
  "bid_amount": 200.00,
  "item_id": "uuid-here"
}
```

## Error Handling

### Common Error Responses

**401 Unauthorized** (Invalid/Expired Token)
```json
{
  "error": "Invalid or expired token"
}
```

**400 Bad Request** (Validation Error)
```json
{
  "error": "Validation failed",
  "details": ["Email is required", "Password must be at least 8 characters"]
}
```

**404 Not Found** (Item Not Found)
```json
{
  "error": "item_not_found",
  "message": "Item not found"
}
```

**400 Bad Request** (Insufficient Credits)
```json
{
  "error": "insufficient_credits",
  "message": "Insufficient credits. Required: ₱200, Available: ₱100"
}
```

## Testing Checklist

### Authentication
- [ ] Register with valid data
- [ ] Register with duplicate email (should fail)
- [ ] Login with valid credentials
- [ ] Login with invalid credentials (should fail)
- [ ] Token persists after app restart
- [ ] Logout clears token

### Items
- [ ] List items (no auth)
- [ ] Filter items by category
- [ ] Search items
- [ ] View item details
- [ ] Create item (with auth)
- [ ] Publish draft item
- [ ] Buy now (if available)

### Bidding
- [ ] Place bid (valid amount)
- [ ] Place bid (amount too low - should fail)
- [ ] Place bid (insufficient credits - should fail)
- [ ] Place bid on own item (should fail)

### Credits
- [ ] View balance
- [ ] View transaction history
- [ ] Initiate top-up
- [ ] Submit receipt
- [ ] Check top-up status

## Common Issues & Solutions

### Issue: Network timeout
**Solution**: Increase timeout values in `Config.java` or handle gracefully

### Issue: Token expired
**Solution**: Check for 401 response, clear token, redirect to login

### Issue: JSON parsing error
**Solution**: Validate JSON structure, handle missing fields gracefully

### Issue: Image loading slow
**Solution**: Use Glide with caching, show placeholder images

### Issue: API returns 404
**Solution**: Check API base URL, verify endpoint paths match backend

## Deployment Checklist

- [ ] Update `Config.API_BASE_URL` to production URL
- [ ] Test all endpoints with production backend
- [ ] Enable ProGuard (if using)
- [ ] Test on multiple Android versions
- [ ] Test on different screen sizes
- [ ] Test with slow network conditions
- [ ] Verify HTTPS is enforced
- [ ] Remove debug logging

## Resources

- **Backend API Docs**: See `INTEGRATION_COMPLETE.md`
- **Design Document**: See `ANDROID_FRONTEND_DESIGN.md`
- **Backend Repository**: `bidhub-backend/`
- **API Base URL**: Configure in `Config.java`

