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
import com.cc106.bidhub.FAQActivity;
import com.cc106.bidhub.R;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView textViewWelcome, textViewCredits, textViewAlias, textViewEmail, textViewUsername;
    private Button buttonLogout;
    private com.google.android.material.card.MaterialCardView buttonRegenerateAlias;
    private android.widget.ImageButton imageButtonRegenerateAlias;
    private ImageButton buttonFAQ;
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
        imageButtonRegenerateAlias = view.findViewById(R.id.imageButtonRegenerateAlias);
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
        
        // Set click listener on both the card and the image button to ensure clicks work
        if (buttonRegenerateAlias != null) {
            buttonRegenerateAlias.setOnClickListener(v -> {
                android.util.Log.d("ProfileFragment", "Regenerate alias button clicked (card)");
                regenerateAlias();
            });
        }
        
        // Also set listener on ImageButton as backup
        if (imageButtonRegenerateAlias != null) {
            imageButtonRegenerateAlias.setOnClickListener(v -> {
                android.util.Log.d("ProfileFragment", "Regenerate alias button clicked (image)");
                regenerateAlias();
            });
        }
        
        if (buttonFAQ != null) {
            buttonFAQ.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), FAQActivity.class);
                startActivity(intent);
            });
        }
    }

    public void loadUserData() {
        try {
            // Use UserRepository as single source of truth
            com.cc106.bidhub.repository.UserRepository userRepo = 
                com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
            
            // Reload data from SharedPreferences to ensure latest values
            userRepo.reloadUserData();
            
            String email = userRepo.getUserEmail();
            String username = userRepo.getUsername();
            String alias = userRepo.getAlias();
            double credits = userRepo.getCredits();
            
            android.util.Log.d("ProfileFragment", "=== LOADING USER DATA ===");
            android.util.Log.d("ProfileFragment", "Email: " + email);
            android.util.Log.d("ProfileFragment", "Username: " + username);
            android.util.Log.d("ProfileFragment", String.format("Credits: %.2f", credits));
            
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
            android.util.Log.e("ProfileFragment", "Error loading user data", e);
            ToastHelper.showError(getContext(), "Error loading user data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("ProfileFragment", "=== ON RESUME - REFRESHING USER DATA ===");
        // Reload user data (including credits) from SharedPreferences
        loadUserData();
        // Then refresh from backend to get latest value
        refreshCreditsFromBackend();
    }

    private static final String BASE_URL = "https://bidhub-android-app.onrender.com/api";

    private void refreshCreditsFromBackend() {
        // Use UserRepository for centralized management
        com.cc106.bidhub.repository.UserRepository userRepo = 
            com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
        
        // First, update UI with cached value from UserRepository
        double cachedBalance = userRepo.getCredits();
        if (textViewCredits != null) {
            textViewCredits.setText(String.format(Locale.getDefault(), "₱ %.2f", cachedBalance));
            android.util.Log.d("ProfileFragment", String.format("Displaying cached balance: %.2f", cachedBalance));
        }
        
        // Then refresh from backend to get latest value
        userRepo.refreshCreditsFromBackend(new com.cc106.bidhub.utils.CreditBalanceManager.BalanceUpdateCallback() {
            @Override
            public void onBalanceUpdated(double newBalance) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    if (textViewCredits != null) {
                        textViewCredits.setText(String.format(Locale.getDefault(), "₱ %.2f", newBalance));
                        android.util.Log.d("ProfileFragment", String.format("Balance updated from backend: %.2f", newBalance));
                    }
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                // Silent fail - cached value already displayed
                android.util.Log.w("ProfileFragment", "Backend refresh failed: " + errorMessage);
            }
        });
    }

    private void regenerateAlias() {
        try {
            android.util.Log.d("ProfileFragment", "regenerateAlias() called");
            String newAlias = AliasGenerator.generateAlias();
            android.util.Log.d("ProfileFragment", "Generated new alias: " + newAlias);
            
            // Update SharedPreferences (backend sync will happen on next login)
            prefsHelper.setAlias(newAlias);
            
            // Reload UserRepository from SharedPreferences to keep it in sync
            com.cc106.bidhub.repository.UserRepository userRepo = 
                com.cc106.bidhub.repository.UserRepository.getInstance(getContext());
            if (userRepo != null) {
                userRepo.loadUserDataFromPreferences();
            }
            
            // Update UI
            if (textViewAlias != null) {
                textViewAlias.setText(newAlias);
                android.util.Log.d("ProfileFragment", "Alias updated in UI: " + newAlias);
            } else {
                android.util.Log.w("ProfileFragment", "textViewAlias is null, cannot update UI");
            }
            
            ToastHelper.showSuccess(getContext(), "Alias regenerated: " + newAlias);
        } catch (Exception e) {
            android.util.Log.e("ProfileFragment", "Error regenerating alias", e);
            ToastHelper.showError(getContext(), "Failed to regenerate alias: " + e.getMessage());
        }
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
