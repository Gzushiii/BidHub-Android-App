package com.cc106.bidhub;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextAlias, editTextPassword;
    private Button buttonRegister;
    private TextView textViewLoginLink;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextAlias = findViewById(R.id.editTextAlias);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLoginLink = findViewById(R.id.textViewLoginLink);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        textViewLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish this activity and go back to the LoginActivity
                finish();
            }
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

        // Hash the password and get the salt
        Map<String, byte[]> hashingResult = PasswordHasher.hashPassword(password);
        byte[] hashedPassword = hashingResult.get("hash");
        byte[] salt = hashingResult.get("salt");

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_EMAIL, email);
        values.put(DatabaseHelper.COLUMN_USER_ALIAS, alias);
        values.put(DatabaseHelper.COLUMN_USER_PASSWORD, hashedPassword); // Store the hashed password
        values.put(DatabaseHelper.COLUMN_USER_SALT, salt);             // Store the salt

        long newRowId = db.insert(DatabaseHelper.TABLE_USERS, null, values);

        if (newRowId != -1) {
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
            finish(); // Close the registration activity and go back to login
        } else {
            Toast.makeText(this, "Registration failed. Email or Alias may already exist.", Toast.LENGTH_LONG).show();
        }
    }
}

