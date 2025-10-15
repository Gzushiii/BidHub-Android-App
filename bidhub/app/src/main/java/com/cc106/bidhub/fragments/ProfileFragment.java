package com.cc106.bidhub.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.utils.ProfilePictureManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cc106.bidhub.AliasGenerator;
import com.cc106.bidhub.DatabaseHelper;
import com.cc106.bidhub.LoginActivity;
import com.cc106.bidhub.ProfileEditActivity;
import com.cc106.bidhub.ProfileSettingsActivity;
import com.cc106.bidhub.R;
import com.cc106.bidhub.FAQActivity;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView textViewWelcome, textViewCredits, textViewAlias, textViewEmail, textViewUsername;
    private Button buttonLogout, buttonRegenerateAlias, buttonViewBids, buttonTransactionHistory, buttonEditProfile, buttonSettings, buttonFAQ;
    private ImageView imageViewProfilePicture;
    private DatabaseHelper dbHelper;
    private String loggedInUserEmail;
    private String userId;

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
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile);
        buttonSettings = view.findViewById(R.id.buttonSettings);
        buttonFAQ = view.findViewById(R.id.buttonFAQ);
        imageViewProfilePicture = view.findViewById(R.id.imageViewProfilePicture);
        
        // Load user data and display it
        loadUserData();
        
        // Load profile picture
        loadProfilePicture();
        
        // Set up click listeners
        setupClickListeners();
        
        return view;
    }

    private void setupClickListeners() {
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
            });
        }
        
        if (buttonRegenerateAlias != null) {
            buttonRegenerateAlias.setOnClickListener(v -> {
                regenerateAlias();
            });
        }
        
        if (buttonViewBids != null) {
            buttonViewBids.setOnClickListener(v -> {
                ToastHelper.showInfo(getContext(), "My Bids - Coming Soon!");
            });
        }
        
        if (buttonTransactionHistory != null) {
            buttonTransactionHistory.setOnClickListener(v -> {
                ToastHelper.showInfo(getContext(), "Transaction History - Coming Soon!");
            });
        }
        
        if (buttonEditProfile != null) {
            buttonEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ProfileEditActivity.class);
                intent.putExtra("USER_EMAIL", loggedInUserEmail);
                startActivity(intent);
            });
        }
        
        if (buttonSettings != null) {
            buttonSettings.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ProfileSettingsActivity.class);
                intent.putExtra("USER_EMAIL", loggedInUserEmail);
                startActivity(intent);
            });
        }
        
        if (buttonFAQ != null) {
            buttonFAQ.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), FAQActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loadUserData() {
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            ToastHelper.showError(getContext(), "Error: User not identified.");
            return;
        }

        try {
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

                // Update the UI with null checks
                if (textViewWelcome != null) textViewWelcome.setText("Profile");
                if (textViewUsername != null) textViewUsername.setText(username);
                if (textViewAlias != null) textViewAlias.setText(alias);
                if (textViewEmail != null) textViewEmail.setText(email);
                if (textViewCredits != null) textViewCredits.setText(String.format(Locale.getDefault(), "₱ %.2f", credits));

                cursor.close();
            }
            db.close();
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error loading user data: " + e.getMessage());
            e.printStackTrace();
        }
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
            ToastHelper.showSuccess(getContext(), "Alias regenerated: " + newAlias);
        } else {
            ToastHelper.showError(getContext(), "Failed to regenerate alias");
        }
        
        db.close();
    }

    private void loadProfilePicture() {
        try {
            if (userId != null && imageViewProfilePicture != null) {
                Bitmap profilePicture = ProfilePictureManager.loadProfilePicture(getContext(), userId);
                if (profilePicture != null) {
                    imageViewProfilePicture.setImageBitmap(profilePicture);
                } else {
                    imageViewProfilePicture.setImageResource(R.drawable.ic_profile);
                }
            }
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error loading profile picture: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void updateUserEmail(String email) {
        this.loggedInUserEmail = email;
        loadUserData();
        loadProfilePicture();
    }
}
