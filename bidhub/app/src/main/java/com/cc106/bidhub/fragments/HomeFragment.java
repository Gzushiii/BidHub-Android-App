package com.cc106.bidhub.fragments;

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

import com.cc106.bidhub.DatabaseHelper;
import com.cc106.bidhub.R;

import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView textViewWelcome, textViewCredits, textViewAlias;
    private Button buttonLogout;
    private DatabaseHelper dbHelper;
    private String loggedInUserEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Get the logged-in user's email from arguments
        if (getArguments() != null) {
            loggedInUserEmail = getArguments().getString("USER_EMAIL");
        }
        
        dbHelper = new DatabaseHelper(getContext());
        
        // Initialize Views
        textViewWelcome = view.findViewById(R.id.textViewWelcome);
        textViewCredits = view.findViewById(R.id.textViewCredits);
        textViewAlias = view.findViewById(R.id.textViewAlias);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        
        // Load user data and display it
        loadUserData();
        
        // Set up click listeners
        buttonLogout.setOnClickListener(v -> {
            // Navigate back to LoginActivity
            getActivity().finish();
        });
        
        return view;
    }
    
    private void loadUserData() {
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            Toast.makeText(getContext(), "Error: User not identified.", Toast.LENGTH_SHORT).show();
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
    
    public void updateUserEmail(String email) {
        this.loggedInUserEmail = email;
        loadUserData();
    }
}
