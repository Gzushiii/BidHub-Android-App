package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

import java.util.Locale;

public class ProfileActivity extends BaseActivity {

    private TextView textViewWelcome, textViewCredits, textViewAlias, textViewEmail, textViewUsername;
    private Button buttonLogout, buttonRegenerateAlias, buttonViewBids, buttonTransactionHistory;
    private DatabaseHelper dbHelper;
    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the profile content into the content frame
        getLayoutInflater().inflate(R.layout.activity_profile_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        dbHelper = new DatabaseHelper(this);
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize Views
        textViewWelcome = findViewById(R.id.textViewWelcome);
        textViewCredits = findViewById(R.id.textViewCredits);
        textViewAlias = findViewById(R.id.textViewAlias);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewUsername = findViewById(R.id.textViewUsername);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonRegenerateAlias = findViewById(R.id.buttonRegenerateAlias);
        buttonViewBids = findViewById(R.id.buttonViewBids);
        buttonTransactionHistory = findViewById(R.id.buttonTransactionHistory);
        
        // Load user data and display it
        loadUserData();
        
        // Set up click listeners
        setupClickListeners();
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
            ToastHelper.showInfo(this, "My Bids - Coming Soon!");
        });
        
        buttonTransactionHistory.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "Transaction History - Coming Soon!");
        });
    }

    private void loadUserData() {
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            ToastHelper.showError(this, "Error: User not identified.");
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{
                    DatabaseHelper.COLUMN_USER_USERNAME,
                    DatabaseHelper.COLUMN_USER_ALIAS, 
                    DatabaseHelper.COLUMN_USER_CREDITS,
                    DatabaseHelper.COLUMN_USER_EMAIL
                },
                DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
                new String[]{loggedInUserEmail},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_USERNAME));
            String alias = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ALIAS));
            double credits = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_CREDITS));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_EMAIL));

            // Update the UI
            textViewWelcome.setText("Profile");
            textViewUsername.setText(username);
            textViewAlias.setText(alias);
            textViewEmail.setText(email);
            textViewCredits.setText(String.format(Locale.getDefault(), "₱ %.2f", credits));

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
            ToastHelper.showSuccess(this, "Alias regenerated: " + newAlias);
        } else {
            ToastHelper.showError(this, "Failed to regenerate alias");
        }
        
        db.close();
    }

    @Override
    protected boolean isCurrentActivity(int itemId) {
        return itemId == R.id.nav_profile;
    }

    @Override
    protected void setCurrentTabSelected() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }

    @Override
    public String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
}
