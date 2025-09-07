package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewGoToRegister;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.dbHelper = new DatabaseHelper(this);

        editTextEmail = findViewById(R.id.editTextEmailLogin);
        editTextPassword = findViewById(R.id.editTextPasswordLogin);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewGoToRegister = findViewById(R.id.textViewGoToRegister);

        buttonLogin.setOnClickListener(v -> loginUser());

        textViewGoToRegister.setOnClickListener(v -> {
            // Start RegisterActivity if it exists
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // We only need to check if a user with these credentials exists.
        String selection = DatabaseHelper.COLUMN_USER_EMAIL + " = ?" + " AND " + DatabaseHelper.COLUMN_USER_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};

        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Login successful
            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();

            // Navigate to the main dashboard activity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("USER_EMAIL", email); // Pass the email to MainActivity so it knows who is logged in
            startActivity(intent);

            finish(); // Close this activity so the user can't go back to it
        } else {
            // Login failed
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
        }

        // Always close the cursor and database when you're done.
        if (cursor != null) {
            cursor.close();
        }
        db.close();
    }
}

