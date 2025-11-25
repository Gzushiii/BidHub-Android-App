# 🏆 BidHub - Android Bidding Application

**Your bid, your win.**

BidHub is a secure, credit-based mobile bidding application for Android that revolutionizes online auctions through a mandatory pre-paid credit system, ensuring genuine bidder commitment and eliminating fraudulent behavior.

---

## 📱 Project Overview

### Vision Statement
To create a secure and trusted mobile bidding environment where all participants are genuinely invested through a mandatory credit system, creating a more reliable and efficient marketplace.

### Core Value Proposition
- **Mandatory Pre-Paid Credits**: Ensures all bidders have sufficient funds before participating
- **Enhanced Security & Privacy**: Alias-based bidding protects user identities
- **Mobile-First Design**: Optimized for smartphone users
- **Localized Payment Methods**: Integration with GCash and Maya payment systems

---

## 🚀 Features & Functionalities

### ✅ Implemented Features

#### 1. User Management & Authentication
- ✅ **User Registration**: Complete account creation with email and phone verification
- ✅ **Secure Login**: Password-based authentication with security features
- ✅ **Profile Management**: User data management and alias generation
- ✅ **Alias System**: Automatic generation of unique bidding aliases for privacy
- ✅ **Account Security**: Security settings and password change functionality

#### 2. Credit System & Payment Integration
- ✅ **Credit Management**: Complete credit balance and transaction system
- ✅ **Credit Packages**: Predefined packages (100, 500, 1000 credits) with pricing
- ✅ **Payment Gateway Framework**: Extensible architecture for GCash and Maya
- ✅ **Redemption Code System**: Secure code generation and manual redemption
- ✅ **Transaction History**: Complete transaction tracking and logging
- ✅ **Test Payment Gateway**: Simulated payment processing for development

#### 3. Item Management & Listing System
- ✅ **Item Creation**: Complete item creation and management system
- ✅ **Image Management**: Image upload, compression, and optimization
- ✅ **Category System**: Hierarchical category management with subcategories
- ✅ **Search & Filtering**: Advanced search and filter capabilities
- ✅ **Item Validation**: Comprehensive validation and security
- ✅ **My Listings**: View and manage posted items
- ✅ **Item Details**: Comprehensive item detail views with image galleries

#### 4. Bidding Engine & Auction System
- ✅ **Bid Placement**: Complete bidding functionality with validation
- ✅ **Auction Management**: Auction lifecycle management
- ✅ **Winner Determination**: Automatic winner selection logic
- ✅ **Credit Integration**: Real-time credit validation and processing
- ✅ **Bid History**: Complete record of all bids with timestamps
- ✅ **Active Bids**: Track ongoing bids
- ✅ **Won Auctions**: View successfully won items
- ✅ **Lost Auctions**: View items that were outbid
- ✅ **Auction Countdown**: Real-time countdown timers

#### 5. Security & Privacy
- ✅ **Password Security**: Secure password hashing and validation
- ✅ **Data Encryption**: End-to-end encryption for sensitive data
- ✅ **Privacy Protection**: User data anonymization through aliases
- ✅ **Access Control**: Role-based permissions and user authorization

#### 6. User Interface & Experience
- ✅ **Material Design 3**: Modern UI components
- ✅ **Fragment-based Navigation**: Home, Browse, Post, Credits, Profile
- ✅ **Custom Toast System**: Enhanced user feedback
- ✅ **Loading States**: Shimmer effects and progress indicators
- ✅ **Image Loading**: Optimized image loading with Glide
- ✅ **Pull-to-Refresh**: Swipe refresh functionality

---

## 🏗️ Project Structure

```
bidhub/
├── app/                           # Main application module
│   ├── src/main/
│   │   ├── java/com/cc106/bidhub/
│   │   │   ├── activities/        # Main app activities
│   │   │   ├── fragments/         # UI fragments
│   │   │   ├── adapters/          # RecyclerView adapters
│   │   │   ├── bidding/           # Bidding engine components
│   │   │   ├── credits/           # Credit management system
│   │   │   ├── items/             # Item management system
│   │   │   ├── payments/          # Payment gateway integration
│   │   │   ├── redemption/        # Redemption code system
│   │   │   ├── toast/             # Custom toast system
│   │   │   ├── notifications/     # Notification management
│   │   │   ├── models/            # Data models
│   │   │   ├── api/               # API client classes
│   │   │   └── utils/             # Utility classes
│   │   ├── res/                   # Android resources
│   │   │   ├── layout/            # XML layout files
│   │   │   ├── drawable/          # Images and icons
│   │   │   ├── values/            # Colors, strings, themes
│   │   │   └── mipmap/            # App launcher icons
│   │   └── AndroidManifest.xml    # App configuration
│   └── build.gradle.kts           # App-level build configuration
├── gradle/                        # Gradle wrapper and version catalog
├── build.gradle.kts               # Project-level build configuration
├── settings.gradle.kts            # Project settings
└── README.md                      # This file
```

---

## 📦 Package Structure

### Core Activities
- `LoginActivity` - User login interface
- `RegisterActivity` - User registration interface
- `MainActivity` - Main application dashboard with bottom navigation
- `WelcomeActivity` - Welcome screen for new users
- `ProfileActivity` - User profile management
- `CreditsActivity` - Credit management and purchase
- `PostActivity` - Item creation and posting
- `BrowseActivity` - Browse and search items
- `BiddingActivity` - Place bids on items
- `ItemDetailActivity` - Detailed item view

### Key Components

#### Bidding System (`bidding/`)
- `BiddingEngine` - Core bidding logic and validation
- `AuctionManager` - Auction lifecycle management
- `Bid` - Bid data model
- `AuctionResult` - Auction result processing
- `BidStatus` - Bid status enumeration

#### Credit System (`credits/`)
- `CreditManager` - Credit balance and transaction management
- `SimpleCreditManager` - Simplified credit operations
- `PaymentGateway` - Payment gateway interface
- `TestPaymentGateway` - Test payment implementation
- `CreditPackage` - Credit package definitions
- `CreditTransaction` - Transaction tracking

#### Item Management (`items/`)
- `ItemManager` - Item CRUD operations
- `CategoryManager` - Category management
- `Item` - Item data model
- `Category` - Category data model
- `FilterCriteria` - Search and filter logic

#### Redemption System (`redemption/`)
- `RedemptionCodeManager` - Redemption code generation and validation
- Secure code distribution and tracking

#### Payment Integration (`payments/`)
- Payment gateway implementations
- GCash and Maya integration templates

#### Utilities
- `DatabaseHelper` - SQLite database management
- `PasswordHasher` - Secure password hashing
- `AliasGenerator` - User alias generation
- `ValidationUtils` - Input validation
- `UIUtils` - UI helper functions
- `ToastHelper` - Custom toast notifications

---

## 🗄️ Database Schema

The application uses SQLite with the following main tables:

- **users** - Stores user information including email, alias, password hash, and credits
- **items** - Item listings with details, images, and auction information
- **bids** - Bid records with timestamps and bidder information
- **categories** - Hierarchical category structure
- **credit_transactions** - Credit purchase and usage history
- **redemption_codes** - Redemption code tracking

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 21 (Android 5.0) or higher
- Java 11
- Gradle 7.0+

### Installation Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd BidHub-Android-App/bidhub
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Open the `bidhub` folder as a project
   - Wait for Gradle sync to complete

3. **Build and Run**
   - Connect Android device or start emulator
   - Click "Run" button in Android Studio
   - App will install and launch on device

### Development Setup
1. **Enable Developer Options** on your Android device
2. **Enable USB Debugging**
3. **Connect device** via USB cable
4. **Trust computer** when prompted on device

---

## 🛠️ Dependencies

### Core Dependencies
- **AndroidX Libraries**: AppCompat, Material Design, Activity, ConstraintLayout
- **Material Design 3**: Modern UI components
- **OkHttp**: HTTP client for API communication
- **Glide**: Image loading and caching
- **Shimmer**: Skeleton loading effects
- **SwipeRefreshLayout**: Pull-to-refresh functionality

### Key Libraries
```kotlin
implementation("com.google.android.material:material:1.11.0")
implementation("com.github.bumptech.glide:glide:4.16.0")
implementation("com.facebook.shimmer:shimmer:0.5.0")
implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
```

---

## 🎨 Architecture

### Design Patterns
- **MVP (Model-View-Presenter)**: Separation of concerns
- **Singleton Pattern**: Managers and utilities
- **Factory Pattern**: Object creation
- **Observer Pattern**: Event handling

### Key Principles
- **Separation of Concerns**: Clear separation between UI, business logic, and data
- **Single Responsibility**: Each class has one clear purpose
- **Dependency Injection**: Loose coupling between components
- **Error Handling**: Comprehensive error handling and user feedback

---

## 🧪 Testing

### Test Structure
- Unit tests for business logic
- Integration tests for API communication
- UI tests for user flows

### Running Tests
```bash
./gradlew test              # Run unit tests
./gradlew connectedAndroidTest  # Run instrumented tests
```

---

## 📊 Project Status

- **Overall Completion**: ~70%
- **Core Features**: 85% complete
- **UI/UX**: 80% complete
- **Database**: 90% complete
- **Payment Integration**: 60% complete (Test gateway implemented)
- **Notifications**: 30% complete

---

## 🛠️ Development Guidelines

### Code Style
- Follow Java naming conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Maintain consistent indentation (4 spaces)

### Git Workflow
- Create feature branches for new features
- Use descriptive commit messages
- Test thoroughly before merging
- Keep commits atomic and focused

### Best Practices
- Always validate user input
- Handle errors gracefully with user-friendly messages
- Use async operations for network calls
- Implement proper loading states
- Follow Material Design guidelines

---

## 📝 Key Features Documentation

### Credit System
See `credits/MVP_README.md` for detailed credit management documentation.

### Redemption Codes
See `redemption/README.md` for redemption code system documentation.

### Toast System
See `toast/README.md` for custom toast implementation details.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Make your changes
4. Test thoroughly
5. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
6. Push to the branch (`git push origin feature/AmazingFeature`)
7. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 📞 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the project documentation

---

## 🔗 Related Documentation

- [Main Project README](../README.md) - Comprehensive project overview
- [API Documentation](../docs/) - Backend API specifications
- [Database Schema](../sql/) - Database structure and migrations

---

*Last updated: January 2025*
