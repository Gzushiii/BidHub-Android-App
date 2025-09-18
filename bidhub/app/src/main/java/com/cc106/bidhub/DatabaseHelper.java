package com.cc106.bidhub;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bidhub.db";
    private static final int DATABASE_VERSION = 3; // Updated version for password recovery schema

    // ==================== TABLE NAMES ====================
    public static final String TABLE_USERS = "users";
    public static final String TABLE_ITEMS = "items";
    public static final String TABLE_BIDS = "bids";
    public static final String TABLE_CREDIT_TRANSACTIONS = "credit_transactions";
    public static final String TABLE_REDEMPTION_CODES = "redemption_codes";
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_PASSWORD_RECOVERY = "password_recovery";

    // ==================== USERS TABLE COLUMNS ====================
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USER_USERNAME = "username";
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_PHONE = "phone_number";
    public static final String COLUMN_USER_PASSWORD = "password";
    public static final String COLUMN_USER_SALT = "salt";
    public static final String COLUMN_USER_FIRST_NAME = "first_name";
    public static final String COLUMN_USER_LAST_NAME = "last_name";
    public static final String COLUMN_USER_ALIAS = "alias";
    public static final String COLUMN_USER_CREDITS = "credits";
    public static final String COLUMN_USER_IS_VERIFIED = "is_verified";
    public static final String COLUMN_USER_CREATED_AT = "created_at";
    public static final String COLUMN_USER_LAST_LOGIN = "last_login";
    public static final String COLUMN_USER_IS_ACTIVE = "is_active";

    // ==================== ITEMS TABLE COLUMNS ====================
    public static final String COLUMN_ITEM_ID = "id";
    public static final String COLUMN_ITEM_TITLE = "title";
    public static final String COLUMN_ITEM_DESCRIPTION = "description";
    public static final String COLUMN_ITEM_CATEGORY_ID = "category_id";
    public static final String COLUMN_ITEM_SELLER_ID = "seller_id";
    public static final String COLUMN_ITEM_STARTING_BID = "starting_bid";
    public static final String COLUMN_ITEM_CURRENT_BID = "current_bid";
    public static final String COLUMN_ITEM_CURRENT_BIDDER_ID = "current_bidder_id";
    public static final String COLUMN_ITEM_BID_DEADLINE = "bid_deadline";
    public static final String COLUMN_ITEM_BILLING_DEADLINE = "billing_deadline";
    public static final String COLUMN_ITEM_CONDITION = "condition";
    public static final String COLUMN_ITEM_IMAGES = "images"; // JSON string of image paths
    public static final String COLUMN_ITEM_STATUS = "status"; // active, ended, sold, cancelled
    public static final String COLUMN_ITEM_CREATED_AT = "created_at";
    public static final String COLUMN_ITEM_UPDATED_AT = "updated_at";

    // ==================== BIDS TABLE COLUMNS ====================
    public static final String COLUMN_BID_ID = "id";
    public static final String COLUMN_BID_ITEM_ID = "item_id";
    public static final String COLUMN_BID_BIDDER_ID = "bidder_id";
    public static final String COLUMN_BID_AMOUNT = "amount";
    public static final String COLUMN_BID_ALIAS = "bidder_alias";
    public static final String COLUMN_BID_CREATED_AT = "created_at";
    public static final String COLUMN_BID_IS_WINNING = "is_winning";

    // ==================== CREDIT TRANSACTIONS TABLE COLUMNS ====================
    public static final String COLUMN_TRANSACTION_ID = "id";
    public static final String COLUMN_TRANSACTION_USER_ID = "user_id";
    public static final String COLUMN_TRANSACTION_TYPE = "type"; // purchase, redemption, bid, refund
    public static final String COLUMN_TRANSACTION_AMOUNT = "amount";
    public static final String COLUMN_TRANSACTION_DESCRIPTION = "description";
    public static final String COLUMN_TRANSACTION_PAYMENT_METHOD = "payment_method"; // gcash, maya
    public static final String COLUMN_TRANSACTION_STATUS = "status"; // pending, completed, failed
    public static final String COLUMN_TRANSACTION_CREATED_AT = "created_at";
    public static final String COLUMN_TRANSACTION_REFERENCE = "reference"; // payment reference or item_id for bids

    // ==================== REDEMPTION CODES TABLE COLUMNS ====================
    public static final String COLUMN_CODE_ID = "id";
    public static final String COLUMN_CODE_CODE = "code";
    public static final String COLUMN_CODE_USER_ID = "user_id";
    public static final String COLUMN_CODE_CREDITS = "credits";
    public static final String COLUMN_CODE_STATUS = "status"; // unused, used, expired
    public static final String COLUMN_CODE_CREATED_AT = "created_at";
    public static final String COLUMN_CODE_EXPIRES_AT = "expires_at";
    public static final String COLUMN_CODE_USED_AT = "used_at";

    // ==================== CATEGORIES TABLE COLUMNS ====================
    public static final String COLUMN_CATEGORY_ID = "id";
    public static final String COLUMN_CATEGORY_NAME = "name";
    public static final String COLUMN_CATEGORY_DESCRIPTION = "description";
    public static final String COLUMN_CATEGORY_PARENT_ID = "parent_id";
    public static final String COLUMN_CATEGORY_IS_ACTIVE = "is_active";

    // ==================== PASSWORD RECOVERY TABLE COLUMNS ====================
    public static final String COLUMN_RECOVERY_ID = "id";
    public static final String COLUMN_RECOVERY_EMAIL = "email";
    public static final String COLUMN_RECOVERY_PHONE = "phone";
    public static final String COLUMN_RECOVERY_CODE = "verification_code";
    public static final String COLUMN_RECOVERY_EXPIRES_AT = "expires_at";
    public static final String COLUMN_RECOVERY_IS_EMAIL = "is_email";
    public static final String COLUMN_RECOVERY_CREATED_AT = "created_at";

    // ==================== CREATE TABLE STATEMENTS ====================
    
    // Users table with enhanced fields
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + "("
            + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USER_USERNAME + " TEXT NOT NULL UNIQUE,"
            + COLUMN_USER_EMAIL + " TEXT NOT NULL UNIQUE,"
            + COLUMN_USER_PHONE + " TEXT NOT NULL,"
            + COLUMN_USER_PASSWORD + " BLOB NOT NULL,"
            + COLUMN_USER_SALT + " BLOB NOT NULL,"
            + COLUMN_USER_FIRST_NAME + " TEXT NOT NULL,"
            + COLUMN_USER_LAST_NAME + " TEXT NOT NULL,"
            + COLUMN_USER_ALIAS + " TEXT NOT NULL UNIQUE,"
            + COLUMN_USER_CREDITS + " REAL DEFAULT 0.0,"
            + COLUMN_USER_IS_VERIFIED + " INTEGER DEFAULT 0,"
            + COLUMN_USER_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + COLUMN_USER_LAST_LOGIN + " DATETIME,"
            + COLUMN_USER_IS_ACTIVE + " INTEGER DEFAULT 1"
            + ")";

    // Categories table for item organization
    private static final String CREATE_TABLE_CATEGORIES = "CREATE TABLE " + TABLE_CATEGORIES + "("
            + COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_CATEGORY_NAME + " TEXT NOT NULL,"
            + COLUMN_CATEGORY_DESCRIPTION + " TEXT,"
            + COLUMN_CATEGORY_PARENT_ID + " INTEGER,"
            + COLUMN_CATEGORY_IS_ACTIVE + " INTEGER DEFAULT 1,"
            + "FOREIGN KEY(" + COLUMN_CATEGORY_PARENT_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ")"
            + ")";

    // Items table for auction listings
    private static final String CREATE_TABLE_ITEMS = "CREATE TABLE " + TABLE_ITEMS + "("
            + COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_ITEM_TITLE + " TEXT NOT NULL,"
            + COLUMN_ITEM_DESCRIPTION + " TEXT NOT NULL,"
            + COLUMN_ITEM_CATEGORY_ID + " INTEGER,"
            + COLUMN_ITEM_SELLER_ID + " INTEGER NOT NULL,"
            + COLUMN_ITEM_STARTING_BID + " REAL NOT NULL,"
            + COLUMN_ITEM_CURRENT_BID + " REAL DEFAULT 0.0,"
            + COLUMN_ITEM_CURRENT_BIDDER_ID + " INTEGER,"
            + COLUMN_ITEM_BID_DEADLINE + " DATETIME NOT NULL,"
            + COLUMN_ITEM_BILLING_DEADLINE + " DATETIME NOT NULL,"
            + COLUMN_ITEM_CONDITION + " TEXT NOT NULL,"
            + COLUMN_ITEM_IMAGES + " TEXT,"
            + COLUMN_ITEM_STATUS + " TEXT DEFAULT 'active',"
            + COLUMN_ITEM_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + COLUMN_ITEM_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + "FOREIGN KEY(" + COLUMN_ITEM_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + "),"
            + "FOREIGN KEY(" + COLUMN_ITEM_SELLER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "),"
            + "FOREIGN KEY(" + COLUMN_ITEM_CURRENT_BIDDER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
            + ")";

    // Bids table for tracking all bids
    private static final String CREATE_TABLE_BIDS = "CREATE TABLE " + TABLE_BIDS + "("
            + COLUMN_BID_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_BID_ITEM_ID + " INTEGER NOT NULL,"
            + COLUMN_BID_BIDDER_ID + " INTEGER NOT NULL,"
            + COLUMN_BID_AMOUNT + " REAL NOT NULL,"
            + COLUMN_BID_ALIAS + " TEXT NOT NULL,"
            + COLUMN_BID_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + COLUMN_BID_IS_WINNING + " INTEGER DEFAULT 0,"
            + "FOREIGN KEY(" + COLUMN_BID_ITEM_ID + ") REFERENCES " + TABLE_ITEMS + "(" + COLUMN_ITEM_ID + "),"
            + "FOREIGN KEY(" + COLUMN_BID_BIDDER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
            + ")";

    // Credit transactions table for financial tracking
    private static final String CREATE_TABLE_CREDIT_TRANSACTIONS = "CREATE TABLE " + TABLE_CREDIT_TRANSACTIONS + "("
            + COLUMN_TRANSACTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TRANSACTION_USER_ID + " INTEGER NOT NULL,"
            + COLUMN_TRANSACTION_TYPE + " TEXT NOT NULL,"
            + COLUMN_TRANSACTION_AMOUNT + " REAL NOT NULL,"
            + COLUMN_TRANSACTION_DESCRIPTION + " TEXT,"
            + COLUMN_TRANSACTION_PAYMENT_METHOD + " TEXT,"
            + COLUMN_TRANSACTION_STATUS + " TEXT DEFAULT 'pending',"
            + COLUMN_TRANSACTION_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + COLUMN_TRANSACTION_REFERENCE + " TEXT,"
            + "FOREIGN KEY(" + COLUMN_TRANSACTION_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
            + ")";

    // Redemption codes table for credit redemption system
    private static final String CREATE_TABLE_REDEMPTION_CODES = "CREATE TABLE " + TABLE_REDEMPTION_CODES + "("
            + COLUMN_CODE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_CODE_CODE + " TEXT NOT NULL UNIQUE,"
            + COLUMN_CODE_USER_ID + " INTEGER NOT NULL,"
            + COLUMN_CODE_CREDITS + " REAL NOT NULL,"
            + COLUMN_CODE_STATUS + " TEXT DEFAULT 'unused',"
            + COLUMN_CODE_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + COLUMN_CODE_EXPIRES_AT + " DATETIME NOT NULL,"
            + COLUMN_CODE_USED_AT + " DATETIME,"
            + "FOREIGN KEY(" + COLUMN_CODE_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
            + ")";

    // Password recovery table for password reset verification
    private static final String CREATE_TABLE_PASSWORD_RECOVERY = "CREATE TABLE " + TABLE_PASSWORD_RECOVERY + "("
            + COLUMN_RECOVERY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_RECOVERY_EMAIL + " TEXT,"
            + COLUMN_RECOVERY_PHONE + " TEXT,"
            + COLUMN_RECOVERY_CODE + " TEXT NOT NULL,"
            + COLUMN_RECOVERY_EXPIRES_AT + " INTEGER NOT NULL,"
            + COLUMN_RECOVERY_IS_EMAIL + " INTEGER NOT NULL,"
            + COLUMN_RECOVERY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create all tables in order (respecting foreign key dependencies)
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_ITEMS);
        db.execSQL(CREATE_TABLE_BIDS);
        db.execSQL(CREATE_TABLE_CREDIT_TRANSACTIONS);
        db.execSQL(CREATE_TABLE_REDEMPTION_CODES);
        db.execSQL(CREATE_TABLE_PASSWORD_RECOVERY);
        
        // Insert default categories
        insertDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop all tables and recreate for version 2
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_REDEMPTION_CODES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREDIT_TRANSACTIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BIDS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
        
        // Add password recovery table for version 3
        if (oldVersion < 3) {
            db.execSQL(CREATE_TABLE_PASSWORD_RECOVERY);
        }
    }

    /**
     * Insert default categories for the marketplace
     */
    private void insertDefaultCategories(SQLiteDatabase db) {
        String[] categories = {
            "Electronics", "Clothing & Accessories", "Home & Garden", 
            "Sports & Recreation", "Books & Media", "Collectibles", 
            "Automotive", "Health & Beauty", "Toys & Games", "Other"
        };
        
        for (String category : categories) {
            db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ") VALUES (?)", 
                      new String[]{category});
        }
    }
}

