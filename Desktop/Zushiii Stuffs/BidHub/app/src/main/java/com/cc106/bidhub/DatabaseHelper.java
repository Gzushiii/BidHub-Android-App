package com.cc106.bidhub;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bidhub.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_ITEMS = "items";

    // Users Table Columns
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_ALIAS = "alias";
    public static final String COLUMN_USER_PASSWORD = "password"; // This will store the hash
    public static final String COLUMN_USER_SALT = "salt";         // New column for the salt
    public static final String COLUMN_USER_CREDITS = "credits";

    // Items Table Columns (Unchanged)
    public static final String COLUMN_ITEM_ID = "id";
    public static final String COLUMN_ITEM_NAME = "name";
    // ... other item columns

    // Create Table Statement for Users (with new salt column)
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + "("
            + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USER_EMAIL + " TEXT NOT NULL UNIQUE,"
            + COLUMN_USER_ALIAS + " TEXT NOT NULL UNIQUE,"
            + COLUMN_USER_PASSWORD + " BLOB NOT NULL," // Use BLOB for binary hash data
            + COLUMN_USER_SALT + " BLOB NOT NULL,"     // Use BLOB for binary salt data
            + COLUMN_USER_CREDITS + " REAL DEFAULT 0.0"
            + ")";

    // Create Table Statement for Items (Unchanged)
    private static final String CREATE_TABLE_ITEMS = "CREATE TABLE " + TABLE_ITEMS + "("
            + COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_ITEM_NAME + " TEXT NOT NULL,"
            + "description TEXT, starting_bid REAL, current_bid REAL, seller_id INTEGER, deadline TEXT"
            + ")";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_ITEMS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
        onCreate(db);
    }
}

