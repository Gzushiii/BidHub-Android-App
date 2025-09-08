package com.cc106.bidhub;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bidhub.db";
    private static final int DATABASE_VERSION = 1;

    // Table Name
    public static final String TABLE_USERS = "users";

    // Common Columns
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_PASSWORD = "password";
    public static final String COLUMN_USER_SALT = "salt";
    public static final String COLUMN_USER_CREDITS = "credits";

    // New Registration Columns
    public static final String COLUMN_USER_USERNAME = "username";
    public static final String COLUMN_USER_PHONE = "phone_number";
    public static final String COLUMN_USER_FIRST_NAME = "first_name";
    public static final String COLUMN_USER_LAST_NAME = "last_name";
    public static final String COLUMN_USER_ALIAS = "alias";


    // Create Table Statement
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + "("
            + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USER_USERNAME + " TEXT NOT NULL UNIQUE,"
            + COLUMN_USER_EMAIL + " TEXT NOT NULL UNIQUE,"
            + COLUMN_USER_PHONE + " TEXT NOT NULL,"
            + COLUMN_USER_PASSWORD + " BLOB NOT NULL,"
            + COLUMN_USER_SALT + " BLOB NOT NULL,"
            + COLUMN_USER_FIRST_NAME + " TEXT NOT NULL,"
            + COLUMN_USER_LAST_NAME + " TEXT NOT NULL,"
            + COLUMN_USER_ALIAS + " TEXT NOT NULL,"
            + COLUMN_USER_CREDITS + " REAL DEFAULT 0.0"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        // We will create the items table later
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}

