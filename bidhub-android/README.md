# BidHub Android App

A Java-only Android application for the BidHub bidding platform, integrated with the Node.js + MySQL backend API.

## Features

- ✅ User Authentication (Login/Register)
- ✅ Browse Items
- ✅ View Item Details
- ✅ Place Bids
- ✅ Buy Now
- ✅ Post Items
- ✅ Credits Management
- ✅ Manual Top-up Flow
- ✅ Transaction History

## Project Structure

```
bidhub-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/cc106/bidhub/
│   │   │   ├── activities/      # All Activity classes
│   │   │   ├── api/             # API client classes
│   │   │   ├── adapters/        # RecyclerView adapters
│   │   │   ├── models/          # Data models (POJOs)
│   │   │   └── utils/           # Utility classes
│   │   ├── res/                 # Resources (layouts, strings, etc.)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## Setup Instructions

### 1. Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 21 (Android 5.0) or higher
- Java 8 or higher
- Backend API running (see `bidhub-backend/`)

### 2. Configuration

1. Open the project in Android Studio
2. Update the API base URL in `app/src/main/java/com/cc106/bidhub/utils/Config.java`:
   ```java
   public static final String API_BASE_URL = "https://your-backend-url.onrender.com/api";
   ```

### 3. Build and Run

1. Sync Gradle files (File → Sync Project with Gradle Files)
2. Connect an Android device or start an emulator
3. Click "Run" button or press Shift+F10
4. The app will install and launch

## API Integration

The app uses REST API calls to communicate with the backend. All API clients are in the `api/` package:

- `AuthApiClient` - Authentication (login/register)
- `ItemApiClient` - Items management
- `BidApiClient` - Bidding functionality
- `CreditsApiClient` - Credits management
- `CategoryApiClient` - Categories
- `TopupApiClient` - Manual top-up flow

## Authentication

The app uses JWT token-based authentication:
- Token is stored in SharedPreferences
- Token is automatically included in API requests via `Authorization: Bearer <token>` header
- Token expires after 7 days (backend setting)

## Key Components

### Activities

- `LoginActivity` - User login
- `RegisterActivity` - User registration
- `MainActivity` - Main dashboard with bottom navigation
- `BrowseItemsActivity` - Browse all items
- `ItemDetailActivity` - View item details and place bids
- `PostItemActivity` - Create and post new items
- `CreditsActivity` - View balance and top-up
- `ProfileActivity` - User profile

### Models

- `User` - User information
- `Item` - Auction item
- `Category` - Item category
- `CreditTransaction` - Credit transaction
- `Topup` - Top-up request

## Dependencies

- AndroidX AppCompat
- Material Design Components
- RecyclerView
- CardView
- Glide (for image loading)
- SwipeRefreshLayout

## Network Security

HTTPS is enforced for all API calls. Network security configuration is in `res/xml/network_security_config.xml`.

## Error Handling

All API calls handle:
- Network errors
- HTTP errors (400, 401, 403, 404, 500)
- JSON parsing errors
- Timeout errors

User-friendly error messages are displayed via Toast notifications.

## Testing

1. Test with backend API running
2. Test on different Android versions (API 21+)
3. Test on different screen sizes
4. Test with slow network conditions

## Troubleshooting

### App won't connect to backend
- Check API base URL in `Config.java`
- Verify backend is running and accessible
- Check network permissions in AndroidManifest.xml

### Login fails
- Verify backend API is responding
- Check email/password format
- Check backend logs for errors

### Items not loading
- Check network connection
- Verify API endpoint is correct
- Check backend API health endpoint

## License

MIT License

