package com.cc106.bidhub;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextAlias, editTextPassword;
    private Button buttonRegister;
    private TextView textViewGoToLogin;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        editTextEmail = findViewById(R.id.editTextEmailRegister);
        editTextAlias = findViewById(R.id.editTextAliasRegister);
        editTextPassword = findViewById(R.id.editTextPasswordRegister);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewGoToLogin = findViewById(R.id.textViewGoToLogin);

        buttonRegister.setOnClickListener(v -> registerUser());

        textViewGoToLogin.setOnClickListener(v -> {
            // Go back to the LoginActivity
            finish(); // This closes the current activity
        });
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String alias = editTextAlias.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || alias.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_EMAIL, email);
        values.put(DatabaseHelper.COLUMN_USER_ALIAS, alias);
        values.put(DatabaseHelper.COLUMN_USER_PASSWORD, password); // Note: In a real app, you MUST hash the password!

        long newRowId = db.insert(DatabaseHelper.TABLE_USERS, null, values);

        if (newRowId != -1) {
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
            // Go to login screen after successful registration
            finish();
        } else {
            Toast.makeText(this, "Registration failed. Email or Alias may already exist.", Toast.LENGTH_LONG).show();
        }
        db.close();
    }
}
