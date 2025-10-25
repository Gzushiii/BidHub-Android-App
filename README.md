# 🏆 BidHub - Mobile Bidding Platform

**Your bid, your win.**

BidHub is a secure, credit-based mobile bidding application for Android that revolutionizes online auctions through a mandatory pre-paid credit system, ensuring genuine bidder commitment and eliminating fraudulent behavior.

---

## 📱 **Project Overview**

### **Vision Statement**
To create a secure and trusted mobile bidding environment where all participants are genuinely invested through a mandatory credit system, creating a more reliable and efficient marketplace.

### **Core Value Proposition**
- **Mandatory Pre-Paid Credits**: Ensures all bidders have sufficient funds before participating
- **Enhanced Security & Privacy**: Alias-based bidding protects user identities
- **Mobile-First Design**: Optimized for smartphone users
- **Localized Payment Methods**: Integration with GCash and Maya payment systems

---

## 🚀 **Features & Functionalities**

### **MVP Core Features (Phase 1)**

#### **1. User Management & Authentication**
- ✅ **User Registration**: Complete account creation with email and phone verification
- ✅ **Secure Login**: Password-based authentication with security features
- ✅ **Profile Management**: User data management and alias generation
- ✅ **Password Recovery**: Complete password recovery system with email/SMS verification
- ✅ **Alias System**: Automatic generation of unique bidding aliases for privacy

#### **2. Credit System & Payment Integration**
- ✅ **Credit Management**: Complete credit balance and transaction system
- ✅ **Credit Packages**: Predefined packages (100, 500, 1000 credits) with pricing
- ✅ **Payment Gateway Framework**: Extensible architecture for GCash and Maya
- ✅ **Redemption Code System**: Secure code generation and manual redemption
- ✅ **Transaction History**: Complete transaction tracking and logging
- ⏳ **Real Payment Integration**: GCash and Maya payment gateway implementation (60% complete)

#### **3. Item Management & Listing System**
- ✅ **Item Creation**: Complete item creation and management system
- ✅ **Image Management**: Image upload, compression, and optimization
- ✅ **Category System**: Hierarchical category management with subcategories
- ✅ **Search & Filtering**: Advanced search and filter capabilities
- ✅ **Item Validation**: Comprehensive validation and security
- ✅ **Auto-save Functionality**: Draft saving and form persistence

#### **4. Bidding Engine & Auction System**
- ✅ **Bid Placement**: Complete bidding functionality with validation
- ✅ **Auction Management**: Auction lifecycle management
- ✅ **Winner Determination**: Automatic winner selection logic
- ✅ **Credit Integration**: Real-time credit validation and processing
- ✅ **Bid History**: Complete record of all bids with timestamps
- ⏳ **Real-time Updates**: Live auction updates and notifications (70% complete)

#### **5. Security & Privacy**
- ✅ **Password Security**: Secure password hashing and validation
- ✅ **Data Encryption**: End-to-end encryption for sensitive data
- ✅ **Privacy Protection**: User data anonymization through aliases
- ✅ **Access Control**: Role-based permissions and user authorization

### **Future Features (Phase 2)**
- 🔄 **Real-time Bidding**: Live updates using WebSocket technology
- ⭐ **Rating System**: Advanced rating and review system
- 🔍 **AI Recommendations**: Enhanced search with AI-powered suggestions
- 💰 **Buy It Now**: Immediate purchase option
- 📊 **Analytics Dashboard**: Business intelligence and reporting
- 👥 **Social Features**: Community building tools

---

## 🏗️ **App Infrastructure**

### **Architecture Overview**
```
BidHub Android App
├── Presentation Layer (Activities & Fragments)
├── Business Logic Layer (Managers & Engines)
├── Data Access Layer (Database & APIs)
└── Infrastructure Layer (Security & Utilities)
```

### **Package Structure**
```
com.cc106.bidhub/
├── activities/           # Main app activities
├── fragments/           # UI fragments
├── adapters/            # RecyclerView adapters
├── bidding/             # Bidding engine components
├── credits/             # Credit management system
├── items/               # Item management system
├── payments/            # Payment gateway integration
├── redemption/          # Redemption code system
├── toast/               # Custom toast system
└── utils/               # Utility classes
```

### **Database Architecture**
- **Current**: SQLite (Local storage)
- **Target**: MySQL (Cloud database)
- **Tables**: 7 core tables with proper relationships
- **Features**: Foreign keys, indexes, and data validation

---

## 📁 **File Types & Meanings**

### **Java Files (.java)**
- **Activities**: Main app screens and user interactions
- **Fragments**: Reusable UI components within activities
- **Adapters**: Data binding for RecyclerView components
- **Managers**: Business logic and data management
- **Models**: Data structures and entities
- **Utils**: Helper classes and utilities

### **XML Layout Files (.xml)**
- **Activity Layouts**: Main screen layouts
- **Fragment Layouts**: Component layouts
- **Item Layouts**: RecyclerView item templates
- **Dialog Layouts**: Popup and dialog designs

### **Resource Files**
- **drawable/**: Images, icons, and vector graphics
- **values/**: Colors, strings, themes, and dimensions
- **anim/**: Animation definitions
- **menu/**: Navigation and action menus
- **mipmap/**: App launcher icons

### **Configuration Files**
- **AndroidManifest.xml**: App permissions and components
- **build.gradle.kts**: Dependencies and build configuration
- **proguard-rules.pro**: Code obfuscation rules

---

## 🎨 **Layout & XML Modification Guide**

### **Basic Layout Structure**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/background_light">
    
    <!-- Your content here -->
    
</LinearLayout>
```

### **Common Layout Patterns**

#### **1. Card-based Layout**
```xml
<androidx.cardview.widget.CardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="4dp"
    app:cardBackgroundColor="@color/white">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">
        
        <!-- Card content -->
        
    </LinearLayout>
    
</androidx.cardview.widget.CardView>
```

#### **2. Text Input with Material Design**
```xml
<com.google.android.material.textfield.TextInputLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="Enter text here"
    app:startIconDrawable="@drawable/ic_icon"
    app:boxBackgroundMode="outline"
    app:boxCornerRadiusTopStart="12dp"
    app:boxCornerRadiusTopEnd="12dp"
    app:boxCornerRadiusBottomStart="12dp"
    app:boxCornerRadiusBottomEnd="12dp"
    app:boxStrokeColor="@color/primary_blue">
    
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/et_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="text"
        android:maxLines="1" />
        
</com.google.android.material.textfield.TextInputLayout>
```

#### **3. Button with Custom Styling**
```xml
<Button
    android:id="@+id/btn_action"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="Action Button"
    android:textSize="16sp"
    android:textStyle="bold"
    android:background="@drawable/button_primary"
    android:textColor="@color/white"
    android:elevation="4dp" />
```

### **Color System**
```xml
<!-- Primary Colors -->
<color name="primary_blue">#FF2196F3</color>
<color name="primary_blue_dark">#FF1976D2</color>
<color name="primary_blue_light">#FFBBDEFB</color>

<!-- Accent Colors -->
<color name="accent_orange">#FFFF9800</color>
<color name="accent_orange_dark">#FFF57C00</color>

<!-- Status Colors -->
<color name="success_green">#FF4CAF50</color>
<color name="error_red">#FFF44336</color>
<color name="warning_yellow">#FFFFC107</color>
```

### **RecyclerView Implementation**
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rv_items"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="8dp"
    android:clipToPadding="false" />
```

**Java Code for RecyclerView:**
```java
// Setup RecyclerView
RecyclerView recyclerView = findViewById(R.id.rv_items);
recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
recyclerView.setAdapter(new ItemCardAdapter(itemList));
```

---

## 🗄️ **MySQL Database Connection Setup**

### **Current Status**
The app currently uses SQLite for local storage but is designed to migrate to MySQL for production use.

### **Step-by-Step MySQL Integration**

#### **1. Add MySQL Dependencies**
Add to `app/build.gradle.kts`:
```kotlin
dependencies {
    // Existing dependencies...
    
    // MySQL Connector
    implementation("mysql:mysql-connector-java:8.0.33")
    
    // Connection Pooling
    implementation("com.zaxxer:HikariCP:5.0.1")
    
    // JSON Processing
    implementation("com.google.code.gson:gson:2.10.1")
}
```

#### **2. Create Database Configuration**
Create `src/main/java/com/cc106/bidhub/database/MySQLConfig.java`:
```java
public class MySQLConfig {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bidhub_db";
    private static final String DB_USER = "your_username";
    private static final String DB_PASSWORD = "your_password";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(DB_DRIVER);
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Driver not found", e);
        }
    }
}
```

#### **3. Create MySQL Database Schema**
Run this SQL script in MySQL Workbench:
```sql
-- Create database
CREATE DATABASE bidhub_db;
USE bidhub_db;

-- Users table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    password BLOB NOT NULL,
    salt BLOB NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    alias VARCHAR(50) UNIQUE NOT NULL,
    credits DECIMAL(10,2) DEFAULT 0.00,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_email (email),
    INDEX idx_alias (alias)
);

-- Categories table
CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_id INT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (parent_id) REFERENCES categories(id)
);

-- Items table
CREATE TABLE items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category_id INT,
    seller_id INT NOT NULL,
    starting_bid DECIMAL(10,2) NOT NULL,
    current_bid DECIMAL(10,2) DEFAULT 0.00,
    current_bidder_id INT NULL,
    bid_deadline DATETIME NOT NULL,
    billing_deadline DATETIME NOT NULL,
    condition VARCHAR(50) NOT NULL,
    images JSON,
    status ENUM('draft', 'active', 'ended', 'sold', 'cancelled') DEFAULT 'draft',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (current_bidder_id) REFERENCES users(id)
);

-- Bids table
CREATE TABLE bids (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    bidder_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    bidder_alias VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_winning BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);

-- Credit transactions table
CREATE TABLE credit_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    type ENUM('purchase', 'redemption', 'bid', 'refund', 'transfer') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description TEXT,
    payment_method VARCHAR(50),
    status ENUM('pending', 'completed', 'failed') DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reference VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Redemption codes table
CREATE TABLE redemption_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    user_id INT NOT NULL,
    credits DECIMAL(10,2) NOT NULL,
    status ENUM('unused', 'used', 'expired') DEFAULT 'unused',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    used_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Password recovery table
CREATE TABLE password_recovery (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255),
    phone VARCHAR(20),
    verification_code VARCHAR(10) NOT NULL,
    expires_at BIGINT NOT NULL,
    is_email BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default categories
INSERT INTO categories (name) VALUES 
('Electronics'), ('Clothing & Accessories'), ('Home & Garden'), 
('Sports & Recreation'), ('Books & Media'), ('Collectibles'), 
('Automotive'), ('Health & Beauty'), ('Toys & Games'), ('Other');
```

#### **4. Create MySQL Database Helper**
Create `src/main/java/com/cc106/bidhub/database/MySQLDatabaseHelper.java`:
```java
public class MySQLDatabaseHelper {
    private static final String TAG = "MySQLDatabaseHelper";
    private static MySQLDatabaseHelper instance;
    private HikariDataSource dataSource;
    
    private MySQLDatabaseHelper() {
        setupConnectionPool();
    }
    
    public static synchronized MySQLDatabaseHelper getInstance() {
        if (instance == null) {
            instance = new MySQLDatabaseHelper();
        }
        return instance;
    }
    
    private void setupConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/bidhub_db");
        config.setUsername("your_username");
        config.setPassword("your_password");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        dataSource = new HikariDataSource(config);
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
```

#### **5. Update DatabaseHelper for MySQL**
Modify `DatabaseHelper.java` to support both SQLite and MySQL:
```java
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final boolean USE_MYSQL = true; // Toggle for database type
    
    // Existing SQLite code...
    
    // Add MySQL methods
    public boolean executeMySQLQuery(String query, Object[] params) {
        if (!USE_MYSQL) return false;
        
        try (Connection conn = MySQLDatabaseHelper.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            // Set parameters
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Log.e(TAG, "MySQL query failed", e);
            return false;
        }
    }
}
```

#### **6. Network Security Configuration**
Add to `res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

#### **7. Update AndroidManifest.xml**
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="true">
    
    <!-- Existing application content -->
    
</application>
```

### **Testing the Connection**
Create a test activity to verify MySQL connection:
```java
public class DatabaseTestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        
        // Test MySQL connection
        new Thread(() -> {
            try (Connection conn = MySQLDatabaseHelper.getInstance().getConnection()) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "MySQL Connected Successfully!", Toast.LENGTH_LONG).show();
                });
            } catch (SQLException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "MySQL Connection Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
```

---

## 🚀 **Getting Started**

### **Prerequisites**
- Android Studio Arctic Fox or later
- Android SDK 21 (Android 5.0) or higher
- Java 11
- MySQL Workbench (for database setup)

### **Installation Steps**
1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd BidHub-Android-App/bidhub
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Open the `bidhub` folder as a project
   - Wait for Gradle sync to complete

3. **Set up MySQL Database**
   - Install MySQL Server
   - Create database using the provided SQL schema
   - Update connection credentials in `MySQLConfig.java`

4. **Build and Run**
   - Connect Android device or start emulator
   - Click "Run" button in Android Studio
   - App will install and launch on device

### **Development Setup**
1. **Enable Developer Options** on your Android device
2. **Enable USB Debugging**
3. **Connect device** via USB cable
4. **Trust computer** when prompted on device

---

## 🛠️ **Development Guidelines**

### **Code Style**
- Follow Java naming conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Maintain consistent indentation (4 spaces)

### **Git Workflow**
- Create feature branches for new features
- Use descriptive commit messages
- Test thoroughly before merging
- Keep commits atomic and focused

### **Testing**
- Write unit tests for business logic
- Test on multiple device sizes
- Verify database operations
- Test payment integrations thoroughly

---

## 📊 **Project Status**

- **Overall Completion**: 42%
- **Core Features**: 70% complete
- **UI/UX**: 80% complete
- **Database**: 90% complete
- **Payment Integration**: 60% complete
- **Notifications**: 0% complete

---

## 🤝 **Contributing**

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

---

## 📄 **License**

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 📞 **Support**

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the project documentation

---

*Last updated: October 2025*
