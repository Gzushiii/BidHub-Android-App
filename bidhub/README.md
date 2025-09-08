# BidHub

A bidding application for Android built with Java and SQLite.

## Features

- User registration and authentication
- Secure password hashing
- User credit system
- Item browsing and posting (placeholder)
- Bidding functionality (placeholder)

## Project Structure

```
bidhub/
├── app/                    # Main application module
│   ├── src/main/
│   │   ├── java/com/cc106/bidhub/  # Java source code
│   │   ├── res/            # Android resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts    # App-level build configuration
├── gradle/                 # Gradle wrapper and version catalog
├── build.gradle.kts        # Project-level build configuration
├── settings.gradle.kts     # Project settings
└── README.md              # This file
```

## Getting Started

1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Build and run the application

## Requirements

- Android Studio Arctic Fox or later
- Android SDK 21 (Android 5.0) or higher
- Java 11

## Package Structure

- `com.cc106.bidhub` - Main package
  - `LoginActivity` - User login interface
  - `RegisterActivity` - User registration interface
  - `MainActivity` - Main application dashboard
  - `DatabaseHelper` - SQLite database management
  - `PasswordHasher` - Password security utilities

## Database Schema

The application uses SQLite with the following main table:
- `users` - Stores user information including email, alias, password hash, and credits

## Development

This project follows standard Android development practices with:
- Material Design components
- SQLite for local data storage
- Secure password hashing
- Proper activity lifecycle management
