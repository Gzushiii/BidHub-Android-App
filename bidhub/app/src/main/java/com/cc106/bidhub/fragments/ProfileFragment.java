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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.cc106.bidhub.api.AuthApiClient;
import com.cc106.bidhub.api.ApiResponse;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.utils.ProfilePictureManager;
import com.cc106.bidhub.utils.SharedPreferencesHelper;
import com.cc106.bidhub.credits.SimpleCreditManager;

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
    private Button buttonLogout, buttonRegenerateAlias, buttonViewBids, buttonTransactionHistory, buttonFAQ;
    private ImageButton buttonEditProfile, buttonSettings;
    private ImageView imageViewProfilePicture;
    private DatabaseHelper dbHelper;
    private SharedPreferencesHelper prefsHelper;
    private SimpleCreditManager creditManager;
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
        prefsHelper = new SharedPreferencesHelper(getContext());
        creditManager = new SimpleCreditManager(getContext());
        
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
        try {
            // Get user data from SharedPreferences (stored during login)
            String email = prefsHelper.getUserEmail();
            String username = prefsHelper.getUsername();
            String alias = prefsHelper.getAlias();
            
            if (email == null || email.isEmpty()) {
                ToastHelper.showError(getContext(), "No user data found. Please log in again.");
                return;
            }

            // Get credits from SimpleCreditManager (same system as CreditsFragment)
            double credits = 0.0;
            if (creditManager != null) {
                // Use email as userId for consistency with CreditsFragment
                credits = creditManager.getCreditBalance(email);
            }

            // Update the UI with user data
            if (textViewWelcome != null) textViewWelcome.setText("Profile");
            if (textViewUsername != null) textViewUsername.setText(username != null ? username : "Unknown");
            if (textViewAlias != null) textViewAlias.setText(alias != null ? alias : "No alias");
            if (textViewEmail != null) textViewEmail.setText(email);
            if (textViewCredits != null) textViewCredits.setText(String.format(Locale.getDefault(), "₱ %.2f", credits));
            
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
    
    /**
     * Refresh user data, especially credits
     */
    public void refreshUserData() {
        if (getView() != null) {
            loadUserData();
        }
    }
}
