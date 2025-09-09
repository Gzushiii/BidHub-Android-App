package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView textViewWelcome, textViewCredits, textViewAlias;
    private Button buttonLogout;
    private BottomNavigationView bottomNavigationView;
    private DatabaseHelper dbHelper;

    private String loggedInUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Initialize Views
        textViewWelcome = findViewById(R.id.textViewWelcome);
        textViewCredits = findViewById(R.id.textViewCredits);
        textViewAlias = findViewById(R.id.textViewAlias);
        buttonLogout = findViewById(R.id.buttonLogout);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");

        // Load user data and display it
        loadUserData();

        // --- Set OnClick Listeners for Buttons ---
        buttonLogout.setOnClickListener(v -> {
            // Navigate back to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close MainActivity
        });

        // --- Set Up Bottom Navigation ---
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // Already on home, do nothing
                return true;
            } else if (itemId == R.id.nav_browse) {
                Intent intent = new Intent(MainActivity.this, BrowseActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_post) {
                Intent intent = new Intent(MainActivity.this, PostActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_credits) {
                Intent intent = new Intent(MainActivity.this, CreditsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void loadUserData() {
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            // Should not happen if coming from a successful login
            Toast.makeText(this, "Error: User not identified.", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COLUMN_USER_ALIAS, DatabaseHelper.COLUMN_USER_CREDITS},
                DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
                new String[]{loggedInUserEmail},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            // Get data from the cursor
            String alias = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ALIAS));
            double credits = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_CREDITS));

            // Update the UI
            textViewWelcome.setText("Welcome back!");
            textViewAlias.setText(alias);
            textViewCredits.setText(String.format(Locale.getDefault(), "₱ %.2f", credits));

            cursor.close();
        }
        db.close();
    }
}
