package com.cc106.bidhub.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cc106.bidhub.AliasGenerator;
import com.cc106.bidhub.DatabaseHelper;
import com.cc106.bidhub.LoginActivity;
import com.cc106.bidhub.R;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView textViewWelcome, textViewCredits, textViewAlias, textViewEmail, textViewUsername;
    private Button buttonLogout, buttonRegenerateAlias, buttonViewBids, buttonTransactionHistory;
    private DatabaseHelper dbHelper;
    private String loggedInUserEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Get the logged-in user's email from arguments
        if (getArguments() != null) {
            loggedInUserEmail = getArguments().getString("USER_EMAIL");
        }
        
        dbHelper = new DatabaseHelper(getContext());
        
        // Initialize Views
        textViewWelcome = view.findViewById(R.id.textViewWelcome);
        textViewCredits = view.findViewById(R.id.textViewCredits);
        textViewAlias = view.findViewById(R.id.textViewAlias);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        textViewUsername = view.findViewById(R.id.textViewUsername);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        buttonRegenerateAlias = view.findViewById(R.id.buttonRegenerateAlias);
        buttonViewBids = view.findViewById(R.id.buttonViewBids);
        buttonTransactionHistory = view.findViewById(R.id.buttonTransactionHistory);
        
        // Load user data and display it
        loadUserData();
        
        // Set up click listeners
        setupClickListeners();
        
        return view;
    }

    private void setupClickListeners() {
        buttonLogout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        });
        
        buttonRegenerateAlias.setOnClickListener(v -> {
            regenerateAlias();
        });
        
        buttonViewBids.setOnClickListener(v -> {
            Toast.makeText(getContext(), "My Bids - Coming Soon!", Toast.LENGTH_SHORT).show();
        });
        
        buttonTransactionHistory.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Transaction History - Coming Soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserData() {
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            Toast.makeText(getContext(), "Error: User not identified.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "Alias regenerated: " + newAlias, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Failed to regenerate alias", Toast.LENGTH_SHORT).show();
        }
        
        db.close();
    }
    
    public void updateUserEmail(String email) {
        this.loggedInUserEmail = email;
        loadUserData();
    }
}
