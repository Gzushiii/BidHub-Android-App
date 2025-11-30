package com.cc106.bidhub.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
            double credits = prefsHelper.getCredits();
            
            if (email == null || email.isEmpty()) {
                ToastHelper.showError(getContext(), "No user data found. Please log in again.");
                return;
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

    @Override
    public void onResume() {
        super.onResume();
        refreshCreditsFromBackend();
    }

    private static final String BASE_URL = "https://bidhub-android-app.onrender.com/api";

    private void refreshCreditsFromBackend() {
        com.cc106.bidhub.utils.CreditBalanceManager.refreshBalance(
            getContext(),
            new com.cc106.bidhub.utils.CreditBalanceManager.BalanceUpdateCallback() {
                @Override
                public void onBalanceUpdated(double newBalance) {
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        if (textViewCredits != null) {
                            textViewCredits.setText(String.format(Locale.getDefault(), "₱ %.2f", newBalance));
                        }
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    // Silent fail - use cached value
                }
            }
        );
    }

    private void regenerateAlias() {
        String newAlias = AliasGenerator.generateAlias();
        
        // Update SharedPreferences (backend sync will happen on next login)
        prefsHelper.setAlias(newAlias);
        
        // Update UI
        textViewAlias.setText(newAlias);
        ToastHelper.showSuccess(getContext(), "Alias regenerated: " + newAlias);
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
