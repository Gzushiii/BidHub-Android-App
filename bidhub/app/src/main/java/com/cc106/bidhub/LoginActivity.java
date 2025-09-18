package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonTestPasswordRecovery;
    private TextView textViewRegisterLink, textViewForgotPassword;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonTestPasswordRecovery = findViewById(R.id.buttonTestPasswordRecovery);
        textViewRegisterLink = findViewById(R.id.textViewRegisterLink);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        textViewRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to open RegisterActivity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        textViewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to open PasswordRecoveryRequestActivity
                Intent intent = new Intent(LoginActivity.this, PasswordRecoveryRequestActivity.class);
                startActivity(intent);
            }
        });

        buttonTestPasswordRecovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to open PasswordRecoveryTestActivity
                Intent intent = new Intent(LoginActivity.this, PasswordRecoveryTestActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            ToastHelper.showWarning(this, "Please fill all fields");
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // We need to retrieve the stored password (hash) and the salt
        String[] columns = {DatabaseHelper.COLUMN_USER_PASSWORD, DatabaseHelper.COLUMN_USER_SALT};
        String selection = DatabaseHelper.COLUMN_USER_EMAIL + " = ?";
        String[] selectionArgs = {email};

        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, columns, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Get the hash and salt from the database for this user
            byte[] storedHash = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PASSWORD));
            byte[] salt = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_SALT));
            cursor.close();

            // Use our hasher to verify the password
            if (PasswordHasher.verifyPassword(password, storedHash, salt)) {
                ToastHelper.showSuccess(this, "Login Successful!");
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("USER_EMAIL", email);
                startActivity(intent);
                finish();
            } else {
                // Password was incorrect
                ToastHelper.showError(this, "Invalid email or password");
            }
        } else {
            // User with that email was not found
            if(cursor != null) {
                cursor.close();
            }
            ToastHelper.showError(this, "Invalid email or password");
        }
        
        // Always close the database connection
        db.close();
    }
}

