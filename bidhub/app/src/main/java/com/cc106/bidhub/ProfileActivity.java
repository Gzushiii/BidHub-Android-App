package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class ProfileActivity extends BaseActivity {

    private TextView textViewAlias, textViewEmail, textViewCredits, textViewUsername;
    private Button buttonLogout, buttonRegenerateAlias, buttonViewBids, buttonTransactionHistory;
    private DatabaseHelper dbHelper;
    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        
        // Inflate profile content into the base layout
        View profileContent = LayoutInflater.from(this).inflate(R.layout.activity_profile, null);
        ((android.widget.FrameLayout) findViewById(R.id.content_frame)).addView(profileContent);
        
        // Set selected navigation item
        setSelectedNavItem(R.id.nav_profile);
        
        dbHelper = new DatabaseHelper(this);
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        if (loggedInUserEmail == null) {
            // If no email passed, get from MainActivity or use default
            loggedInUserEmail = "user@example.com"; // This should be passed from MainActivity
        }
        
        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        textViewAlias = findViewById(R.id.textViewAlias);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewCredits = findViewById(R.id.textViewCredits);
        textViewUsername = findViewById(R.id.textViewUsername);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonRegenerateAlias = findViewById(R.id.buttonRegenerateAlias);
        buttonViewBids = findViewById(R.id.buttonViewBids);
        buttonTransactionHistory = findViewById(R.id.buttonTransactionHistory);
    }

    private void setupClickListeners() {
        buttonLogout.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        buttonRegenerateAlias.setOnClickListener(v -> {
            regenerateAlias();
        });

        buttonViewBids.setOnClickListener(v -> {
            Toast.makeText(this, "My Bids - Coming Soon!", Toast.LENGTH_SHORT).show();
        });

        buttonTransactionHistory.setOnClickListener(v -> {
            Toast.makeText(this, "Transaction History - Coming Soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserData() {
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            Toast.makeText(this, "Error: User not identified.", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{
                    DatabaseHelper.COLUMN_USER_ALIAS,
                    DatabaseHelper.COLUMN_USER_CREDITS,
                    DatabaseHelper.COLUMN_USER_USERNAME,
                    DatabaseHelper.COLUMN_USER_EMAIL
                },
                DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
                new String[]{loggedInUserEmail},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String alias = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ALIAS));
            double credits = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_CREDITS));
            String username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_USERNAME));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_EMAIL));

            textViewAlias.setText(alias);
            textViewCredits.setText(String.format(Locale.getDefault(), "₱ %.2f", credits));
            textViewUsername.setText(username);
            textViewEmail.setText(email);

            cursor.close();
        }
        db.close();
    }

    private void regenerateAlias() {
        String newAlias = AliasGenerator.generateAlias();
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ALIAS, newAlias);
        
        int rowsAffected = db.update(
            DatabaseHelper.TABLE_USERS,
            values,
            DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
            new String[]{loggedInUserEmail}
        );
        
        if (rowsAffected > 0) {
            textViewAlias.setText(newAlias);
            Toast.makeText(this, "New alias generated: " + newAlias, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Failed to regenerate alias", Toast.LENGTH_SHORT).show();
        }
        
        db.close();
    }
}
