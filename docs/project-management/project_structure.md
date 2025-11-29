# BidHub Project Structure Documentation

> **Related Documentation:**
> - [Implementation Plan](./Implementation.md) - Complete implementation plan and tech stack
> - [UI/UX Design System](./UI_UX_doc.md) - Design system and user experience specifications

## Root Directory Structure

```
BidHub-Android-App/
├── bidhub/                          # Main Android project
│   ├── app/                        # Android application module
│   ├── build/                      # Build outputs and intermediates
│   ├── gradle/                     # Gradle wrapper and configuration
│   ├── build.gradle.kts           # Project-level build configuration
│   ├── settings.gradle.kts         # Project settings and modules
│   ├── gradle.properties          # Gradle properties and configuration
│   └── local.properties           # Local development properties
├── Docs/                          # Project documentation
│   ├── Implementation.md          # Implementation plan and tech stack
│   ├── project_structure.md       # This file - project organization
│   └── UI_UX_doc.md              # Design system and UX guidelines
├── Project Management/            # Project management documents
│   ├── app-completion-analysis.md
│   ├── current-progress-assessment.md
│   ├── jira-epics.md
│   ├── mvp-epic.md
│   └── project-requirements.md
├── README.md                      # Main project documentation
├── DESIGN_SYSTEM_GUIDE.md         # Design system specifications
└── logo.png                       # Project logo and assets
```

## Detailed Android App Structure

### Main Application Module (`bidhub/app/`)

```
app/
├── build/                         # Build outputs and generated files
│   ├── generated/                 # Auto-generated source files
│   ├── intermediates/             # Intermediate build files
│   ├── outputs/                   # Final build outputs (APK, AAB)
│   └── tmp/                       # Temporary build files
├── src/
│   ├── main/                      # Main source code
│   │   ├── java/com/cc106/bidhub/ # Java source code
│   │   │   ├── activities/        # Android Activities
│   │   │   ├── fragments/         # Android Fragments
│   │   │   ├── adapters/          # RecyclerView and List adapters
│   │   │   ├── bidding/           # Bidding engine components
│   │   │   ├── credits/           # Credit management system
│   │   │   ├── items/             # Item management system
│   │   │   ├── payments/          # Payment gateway integration
│   │   │   ├── redemption/        # Redemption code system
│   │   │   ├── toast/             # Custom toast system
│   │   │   ├── utils/             # Utility classes
│   │   │   ├── database/          # Database helpers and models
│   │   │   ├── network/           # Network and API clients
│   │   │   ├── security/          # Security and encryption
│   │   │   └── services/          # Background services
│   │   ├── res/                   # Android resources
│   │   │   ├── anim/              # Animation definitions
│   │   │   ├── drawable/          # Images, icons, and drawables
│   │   │   ├── layout/            # XML layout files
│   │   │   ├── menu/              # Menu definitions
│   │   │   ├── mipmap/            # App launcher icons
│   │   │   ├── values/            # Strings, colors, dimensions
│   │   │   └── xml/               # Configuration XML files
│   │   └── AndroidManifest.xml    # App manifest
│   ├── test/                      # Unit tests
│   └── androidTest/               # Instrumented tests
├── build.gradle.kts              # Module-level build configuration
└── proguard-rules.pro            # Code obfuscation rules
```

## Package Organization

> **Implementation Reference:** This package structure aligns with the [Implementation Plan](./Implementation.md) stages and supports the [UI/UX Design System](./UI_UX_doc.md) component requirements.

### Core Package Structure (`com.cc106.bidhub`)

```
com.cc106.bidhub/
├── activities/                    # Main app screens
│   ├── AuthActivity.java         # Login/Registration
│   ├── MainActivity.java         # Main dashboard
│   ├── ItemListActivity.java     # Item browsing
│   ├── ItemDetailActivity.java   # Item details and bidding
│   ├── ProfileActivity.java      # User profile management
│   ├── CreditActivity.java       # Credit management
│   └── SettingsActivity.java     # App settings
├── fragments/                     # Reusable UI components
│   ├── HomeFragment.java         # Home dashboard
│   ├── SearchFragment.java       # Search interface
│   ├── BiddingFragment.java      # Bidding interface
│   ├── ProfileFragment.java      # Profile management
│   └── SettingsFragment.java     # Settings interface
├── adapters/                      # Data binding adapters
│   ├── ItemAdapter.java          # Item list adapter
│   ├── BidAdapter.java           # Bid history adapter
│   ├── CategoryAdapter.java      # Category selection
│   └── TransactionAdapter.java   # Transaction history
├── bidding/                       # Bidding engine
│   ├── BiddingEngine.java        # Core bidding logic
│   ├── AuctionManager.java       # Auction management
│   ├── BidValidator.java         # Bid validation
│   └── WinnerDeterminer.java     # Winner selection logic
├── credits/                       # Credit system
│   ├── CreditManager.java        # Credit balance management
│   ├── CreditTransaction.java    # Transaction processing
│   ├── CreditPackage.java        # Credit package definitions
│   └── CreditValidator.java      # Credit validation
├── items/                         # Item management
│   ├── ItemManager.java          # Item CRUD operations
│   ├── ItemValidator.java        # Item validation
│   ├── ImageManager.java         # Image handling
│   └── CategoryManager.java      # Category management
├── payments/                      # Payment integration
│   ├── PaymentGateway.java       # Payment interface
│   ├── GCashIntegration.java     # GCash payment
│   ├── MayaIntegration.java      # Maya payment
│   └── PaymentValidator.java     # Payment validation
├── redemption/                    # Redemption system
│   ├── RedemptionManager.java    # Code generation
│   ├── CodeValidator.java        # Code validation
│   └── RedemptionHistory.java    # Redemption tracking
├── database/                      # Database layer
│   ├── DatabaseHelper.java       # SQLite database helper
│   ├── MySQLDatabaseHelper.java  # MySQL database helper
│   ├── UserDAO.java              # User data access
│   ├── ItemDAO.java              # Item data access
│   ├── BidDAO.java               # Bid data access
│   └── TransactionDAO.java       # Transaction data access
├── network/                       # Network layer
│   ├── ApiClient.java            # HTTP client
│   ├── ApiService.java           # API service interface
│   ├── NetworkManager.java       # Network state management
│   └── ResponseHandler.java      # API response handling
├── security/                      # Security features
│   ├── EncryptionManager.java    # Data encryption
│   ├── PasswordManager.java      # Password handling
│   ├── TokenManager.java         # JWT token management
│   └── SecurityValidator.java    # Security validation
├── services/                      # Background services
│   ├── NotificationService.java  # Push notifications
│   ├── SyncService.java          # Data synchronization
│   └── BackgroundTaskService.java # Background tasks
├── utils/                         # Utility classes
│   ├── Constants.java            # App constants
│   ├── Helper.java               # General helper methods
│   ├── DateUtils.java            # Date/time utilities
│   ├── ImageUtils.java           # Image processing
│   └── ValidationUtils.java      # Input validation
└── models/                        # Data models
    ├── User.java                 # User model
    ├── Item.java                 # Item model
    ├── Bid.java                  # Bid model
    ├── Transaction.java          # Transaction model
    └── Category.java             # Category model
```

## Resource Organization

### Layout Files (`res/layout/`)

```
layout/
├── activity_auth.xml             # Login/Registration screen
├── activity_main.xml             # Main dashboard
├── activity_item_list.xml        # Item browsing
├── activity_item_detail.xml      # Item details
├── activity_profile.xml          # User profile
├── activity_credit.xml           # Credit management
├── activity_settings.xml         # App settings
├── fragment_home.xml             # Home fragment
├── fragment_search.xml           # Search fragment
├── fragment_bidding.xml          # Bidding fragment
├── fragment_profile.xml          # Profile fragment
├── item_card.xml                 # Item card layout
├── bid_item.xml                  # Bid item layout
├── transaction_item.xml          # Transaction item layout
└── dialog_*.xml                  # Dialog layouts
```

### Drawable Resources (`res/drawable/`)

```
drawable/
├── ic_*.xml                      # Vector icons
├── button_*.xml                  # Button backgrounds
├── input_field_*.xml             # Input field backgrounds
├── card_*.xml                    # Card backgrounds
├── background_*.xml              # Screen backgrounds
└── logo.png                      # App logo
```

### Values Resources (`res/values/`)

```
values/
├── strings.xml                   # String resources
├── colors.xml                    # Color definitions
├── dimensions.xml                # Size and spacing values
├── styles.xml                    # UI styles and themes
└── themes.xml                    # App themes
```

## Configuration Files

### Build Configuration

```
build.gradle.kts (Project level)
├── Dependencies management
├── Plugin configuration
├── Repository definitions
└── Global build settings

build.gradle.kts (App level)
├── Android configuration
├── Dependencies
├── Build variants
├── Signing configuration
└── ProGuard rules
```

### Gradle Configuration

```
gradle/
├── wrapper/
│   ├── gradle-wrapper.jar        # Gradle wrapper
│   └── gradle-wrapper.properties # Wrapper configuration
└── libs.versions.toml            # Version catalog
```

## Database Schema

### MySQL Database Structure

```
bidhub_db/
├── users                         # User accounts
├── categories                    # Item categories
├── items                         # Auction items
├── bids                          # Bid records
├── credit_transactions           # Credit transactions
├── redemption_codes              # Redemption codes
└── audit_logs                    # System audit logs
```

## Environment Configuration

### Development Environment

```
local.properties
├── Database connection settings
├── API endpoints
├── Debug configurations
└── Development keys
```

### Production Environment

```
production.properties
├── Production database settings
├── Live API endpoints
├── Release configurations
└── Production keys
```

## Asset Organization

### Images and Media

```
assets/
├── images/                       # App images
│   ├── logos/                    # Logo variations
│   ├── icons/                    # App icons
│   └── placeholders/             # Placeholder images
├── fonts/                        # Custom fonts
└── data/                         # Static data files
```

## Testing Structure

### Unit Tests (`src/test/`)

```
test/java/com/cc106/bidhub/
├── activities/                   # Activity tests
├── fragments/                    # Fragment tests
├── utils/                        # Utility tests
├── database/                     # Database tests
├── network/                      # Network tests
└── services/                     # Service tests
```

### Instrumented Tests (`src/androidTest/`)

```
androidTest/java/com/cc106/bidhub/
├── ui/                          # UI tests
├── integration/                 # Integration tests
└── performance/                 # Performance tests
```

## Documentation Structure

### Technical Documentation

```
Docs/
├── Implementation.md            # Implementation plan
├── project_structure.md         # This file
├── UI_UX_doc.md                # Design system
├── API_documentation.md         # API reference
├── Database_schema.md           # Database documentation
└── Deployment_guide.md          # Deployment instructions
```

## File Naming Conventions

### Java/Kotlin Files
- **Activities**: `[Feature]Activity.java` (e.g., `AuthActivity.java`)
- **Fragments**: `[Feature]Fragment.java` (e.g., `HomeFragment.java`)
- **Adapters**: `[Data]Adapter.java` (e.g., `ItemAdapter.java`)
- **Managers**: `[Feature]Manager.java` (e.g., `CreditManager.java`)
- **Models**: `[Entity].java` (e.g., `User.java`)
- **Utils**: `[Purpose]Utils.java` (e.g., `DateUtils.java`)

### XML Files
- **Layouts**: `activity_[name].xml`, `fragment_[name].xml`, `item_[name].xml`
- **Drawables**: `ic_[name].xml`, `button_[name].xml`, `background_[name].xml`
- **Menus**: `menu_[name].xml`
- **Values**: `strings.xml`, `colors.xml`, `dimensions.xml`

### Resource Naming
- **IDs**: `[type]_[name]` (e.g., `btn_submit`, `et_email`)
- **Strings**: `[feature]_[description]` (e.g., `auth_login_title`)
- **Colors**: `[purpose]_[color]` (e.g., `primary_blue`, `error_red`)
- **Dimensions**: `[type]_[size]` (e.g., `margin_small`, `text_large`)

## Build and Deployment Structure

### Build Variants

```
buildTypes/
├── debug                        # Development build
│   ├── Debug signing
│   ├── Debug API endpoints
│   └── Debug logging enabled
├── release                      # Production build
│   ├── Release signing
│   ├── Production API endpoints
│   └── Optimized and obfuscated
└── staging                      # Staging build
    ├── Staging signing
    ├── Staging API endpoints
    └── Debug logging enabled
```

### Deployment Pipeline

```
CI/CD Pipeline/
├── Source Control (Git)
├── Build Trigger
├── Code Quality Checks
├── Unit Tests
├── Integration Tests
├── Build Generation
├── Security Scan
├── Deployment to Staging
├── User Acceptance Testing
└── Production Deployment
```

## Module Dependencies

### Internal Dependencies

```
app module dependencies:
├── Core Android libraries
├── UI/UX libraries (Material Design)
├── Network libraries (Retrofit, OkHttp)
├── Database libraries (Room, SQLite)
├── Image processing libraries (Glide, Picasso)
├── Security libraries (JWT, Encryption)
└── Testing libraries (JUnit, Espresso)
```

### External Dependencies

```
External integrations:
├── Payment gateways (GCash, Maya)
├── Push notifications (Firebase)
├── Image storage (AWS S3, Google Cloud)
├── Analytics (Firebase Analytics)
└── Crash reporting (Firebase Crashlytics)
```

## Security Considerations

### Code Organization for Security

```
security/
├── EncryptionManager.java        # Data encryption
├── PasswordManager.java          # Password security
├── TokenManager.java             # JWT token handling
├── SecurityValidator.java        # Input validation
├── BiometricManager.java         # Biometric authentication
└── SecurityConfig.java           # Security configuration
```

### Secure Storage

```
secure_storage/
├── EncryptedSharedPreferences    # Encrypted preferences
├── KeyStore integration          # Android KeyStore
├── Secure file storage           # Encrypted file storage
└── Secure network communication  # TLS/SSL
```

## Performance Optimization

### Code Organization for Performance

```
performance/
├── ImageOptimization.java        # Image compression
├── CacheManager.java             # Caching strategies
├── MemoryManager.java            # Memory optimization
├── NetworkOptimization.java      # Network efficiency
└── DatabaseOptimization.java     # Query optimization
```

## Documentation Integration

This project structure is designed to support the complete implementation of the BidHub Mobile Bidding Platform:

- **Implementation Plan**: The package organization supports all features outlined in [Implementation.md](./Implementation.md)
- **UI/UX Design**: The structure accommodates all components and layouts specified in [UI_UX_doc.md](./UI_UX_doc.md)
- **Cross-References**: All three documents work together to provide a complete development guide

## Development Workflow

1. Follow the [Implementation Plan](./Implementation.md) for development stages and feature priorities
2. Use this project structure for organizing code and resources
3. Implement UI components according to the [UI/UX Design System](./UI_UX_doc.md)
4. Maintain consistency across all three documentation areas

---

This project structure provides a comprehensive, scalable, and maintainable foundation for the BidHub Mobile Bidding Platform, ensuring proper organization, security, and performance while following Android development best practices.
