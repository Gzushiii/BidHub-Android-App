package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.utils.ProfilePictureManager;

import java.util.Locale;

public class ProfileActivity extends BaseActivity {

    private TextView textViewWelcome, textViewCredits, textViewAlias, textViewEmail, textViewUsername;
    private Button buttonLogout, buttonRegenerateAlias, buttonViewBids, buttonTransactionHistory, buttonEditProfile, buttonSettings;
    private ImageView imageViewProfilePicture;
    private DatabaseHelper dbHelper;
    private String loggedInUserEmail;
    private String userId;

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
        buttonEditProfile = findViewById(R.id.buttonEditProfile);
        buttonSettings = findViewById(R.id.buttonSettings);
        imageViewProfilePicture = findViewById(R.id.imageViewProfilePicture);
        
        // Load user data and display it
        loadUserData();
        
        // Load profile picture
        loadProfilePicture();
        
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
        
        buttonEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileEditActivity.class);
            intent.putExtra("USER_EMAIL", loggedInUserEmail);
            startActivityForResult(intent, 1001);
        });
        
        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("USER_EMAIL", loggedInUserEmail);
            startActivity(intent);
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
                    DatabaseHelper.COLUMN_USER_ID,
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
            userId = String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)));
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

    private void loadProfilePicture() {
        if (userId != null && imageViewProfilePicture != null) {
            Bitmap profilePicture = ProfilePictureManager.loadProfilePicture(this, userId);
            if (profilePicture != null) {
                imageViewProfilePicture.setImageBitmap(profilePicture);
            } else {
                imageViewProfilePicture.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Profile was updated, refresh the data
            if (data != null && data.hasExtra("UPDATED_EMAIL")) {
                loggedInUserEmail = data.getStringExtra("UPDATED_EMAIL");
            }
            loadUserData();
            loadProfilePicture();
        }
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
