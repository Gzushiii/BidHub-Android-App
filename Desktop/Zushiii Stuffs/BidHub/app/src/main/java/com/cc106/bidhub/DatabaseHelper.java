package com.cc106.bidhub;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "bidhub.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_ITEMS = "items";

    // Users Table Columns
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_ALIAS = "alias";
    public static final String COLUMN_USER_PASSWORD = "password";
    public static final String COLUMN_USER_CREDITS = "credits";

    // Items Table Columns
    public static final String COLUMN_ITEM_ID = "id";
    public static final String COLUMN_ITEM_NAME = "name";
    public static final String COLUMN_ITEM_DESCRIPTION = "description";
    public static final String COLUMN_ITEM_STARTING_BID = "starting_bid";
    public static final String COLUMN_ITEM_CURRENT_BID = "current_bid";
    public static final String COLUMN_ITEM_WINNER_ID = "winner_id";
    public static final String COLUMN_ITEM_SELLER_ID = "seller_id";
    public static final String COLUMN_ITEM_DEADLINE = "deadline";

    // Create Table Statements
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + "("
            + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USER_EMAIL + " TEXT NOT NULL UNIQUE,"
            + COLUMN_USER_ALIAS + " TEXT NOT NULL UNIQUE,"
            + COLUMN_USER_PASSWORD + " TEXT NOT NULL,"
            + COLUMN_USER_CREDITS + " REAL DEFAULT 0.0"
            + ")";

    private static final String CREATE_TABLE_ITEMS = "CREATE TABLE " + TABLE_ITEMS + "("
            + COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_ITEM_NAME + " TEXT NOT NULL,"
            + COLUMN_ITEM_DESCRIPTION + " TEXT,"
            + COLUMN_ITEM_STARTING_BID + " REAL NOT NULL,"
            + COLUMN_ITEM_CURRENT_BID + " REAL,"
            + COLUMN_ITEM_SELLER_ID + " INTEGER NOT NULL,"
            + COLUMN_ITEM_DEADLINE + " TEXT NOT NULL"
            + ")";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables when the database is created for the first time
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_ITEMS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if they exist and create them fresh
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
        onCreate(db);
    }
}
