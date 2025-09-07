package com.cc106.bidhub;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextFirstName, editTextLastName, editTextUsername, editTextAlias, editTextEmail, editTextPhone, editTextPassword;
    private CheckBox checkboxTerms, checkboxPrivacy;
    private Button buttonRegister;
    private TextView textViewLoginLink;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        // Initialize all the UI components
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextAlias = findViewById(R.id.editTextAlias);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextPassword = findViewById(R.id.editTextPassword);
        checkboxTerms = findViewById(R.id.checkboxTerms);
        checkboxPrivacy = findViewById(R.id.checkboxPrivacy);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLoginLink = findViewById(R.id.textViewLoginLink);

        buttonRegister.setOnClickListener(v -> registerUser());
        textViewLoginLink.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        // Get text from all fields
        String firstName = editTextFirstName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String alias = editTextAlias.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // --- Form Validation ---
        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(username) ||
                TextUtils.isEmpty(alias) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkboxTerms.isChecked() || !checkboxPrivacy.isChecked()) {
            Toast.makeText(this, "You must accept the Terms and Privacy Policy", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Database Insertion ---
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Hash the password
        Map<String, byte[]> hashingResult = PasswordHasher.hashPassword(password);
        byte[] hashedPassword = hashingResult.get("hash");
        byte[] salt = hashingResult.get("salt");

        // Put all user data into the ContentValues object
        values.put(DatabaseHelper.COLUMN_USER_FIRST_NAME, firstName);
        values.put(DatabaseHelper.COLUMN_USER_LAST_NAME, lastName);
        values.put(DatabaseHelper.COLUMN_USER_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_USER_ALIAS, alias);
        values.put(DatabaseHelper.COLUMN_USER_EMAIL, email); // <-- THIS LINE IS NOW CORRECTED
        values.put(DatabaseHelper.COLUMN_USER_PHONE, phone);
        values.put(DatabaseHelper.COLUMN_USER_PASSWORD, hashedPassword);
        values.put(DatabaseHelper.COLUMN_USER_SALT, salt);

        long newRowId = db.insert(DatabaseHelper.TABLE_USERS, null, values);

        if (newRowId != -1) {
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
            finish(); // Go back to the login screen
        } else {
            Toast.makeText(this, "Registration failed. Username or Email may already exist.", Toast.LENGTH_LONG).show();
        }
    }
}

