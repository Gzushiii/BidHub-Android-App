package com.cc106.bidhub.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.cc106.bidhub.toast.ToastHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cc106.bidhub.BrowseActivity;
import com.cc106.bidhub.CreditsActivity;
import com.cc106.bidhub.DatabaseHelper;
import com.cc106.bidhub.HelpSupportActivity;
import com.cc106.bidhub.MainActivity;
import com.cc106.bidhub.PostActivity;
import com.cc106.bidhub.ProfileActivity;
import com.cc106.bidhub.R;

import java.util.Locale;

public class HomeFragment extends Fragment {

    // Header components
    private ImageButton btnMenu, btnInbox, btnNotifications;
    private ImageView imgProfile;
    private TextView textViewWelcome, textViewCredits, textViewAlias;
    
    // Onboarding components
    private ProgressBar progressOnboarding;
    private TextView textOnboardingProgress;
    
    // Feature cards
    private View cardBrowse, cardSell, cardCredits, cardHelp;
    
    // Promotional banner
    private Button btnClaimOffer;
    
    // Primary action buttons
    private Button btnBuy, btnPost;
    
    // Stats components
    private TextView textActiveBids, textItemsPosted;
    
    // Logout button
    private Button buttonLogout;
    
    private DatabaseHelper dbHelper;
    private String loggedInUserEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_home, container, false);
            
            // Get the logged-in user's email from arguments
            if (getArguments() != null) {
                loggedInUserEmail = getArguments().getString("USER_EMAIL");
            }
            
            // Initialize database helper
            if (getContext() != null) {
                try {
                    dbHelper = new DatabaseHelper(getContext());
                } catch (Exception e) {
                    ToastHelper.showError(getContext(), "Error initializing database: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Initialize all UI components
            initializeViews(view);
            
            // Load user data and display it
            loadUserData();
            
            // Set up click listeners
            setupClickListeners();
            
            return view;
        } catch (Exception e) {
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error creating home view: " + e.getMessage());
            }
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeViews(View view) {
        if (view == null) {
            return;
        }
        
        // Header components
        btnMenu = view.findViewById(R.id.btnMenu);
        btnInbox = view.findViewById(R.id.btnInbox);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        imgProfile = view.findViewById(R.id.imgProfile);
        textViewWelcome = view.findViewById(R.id.textViewWelcome);
        textViewCredits = view.findViewById(R.id.textViewCredits);
        textViewAlias = view.findViewById(R.id.textViewAlias);
        
        // Onboarding components
        progressOnboarding = view.findViewById(R.id.progressOnboarding);
        textOnboardingProgress = view.findViewById(R.id.textOnboardingProgress);
        
        // Feature cards
        cardBrowse = view.findViewById(R.id.cardBrowse);
        cardSell = view.findViewById(R.id.cardSell);
        cardCredits = view.findViewById(R.id.cardCredits);
        cardHelp = view.findViewById(R.id.cardHelp);
        
        // Promotional banner
        btnClaimOffer = view.findViewById(R.id.btnClaimOffer);
        
        // Primary action buttons
        btnBuy = view.findViewById(R.id.btnBuy);
        btnPost = view.findViewById(R.id.btnPost);
        
        // Stats components
        textActiveBids = view.findViewById(R.id.textActiveBids);
        textItemsPosted = view.findViewById(R.id.textItemsPosted);
        
        // Logout button
        buttonLogout = view.findViewById(R.id.buttonLogout);
    }
    
    /**
     * Set up click listeners for all interactive elements
     */
    private void setupClickListeners() {
        // Header buttons
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                // TODO: Open navigation drawer or menu
                ToastHelper.showInfo(getContext(), "Menu clicked");
            });
        }
        
        if (btnInbox != null) {
            btnInbox.setOnClickListener(v -> {
                // TODO: Navigate to inbox/messages
                ToastHelper.showInfo(getContext(), "Inbox clicked");
            });
        }
        
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                // TODO: Navigate to notifications
                ToastHelper.showInfo(getContext(), "Notifications clicked");
            });
        }
        
                if (imgProfile != null) {
                    imgProfile.setOnClickListener(v -> {
                        try {
                            // Navigate to profile
                            Intent intent = new Intent(getContext(), ProfileActivity.class);
                            intent.putExtra("USER_EMAIL", loggedInUserEmail);
                            startActivity(intent);
                        } catch (Exception e) {
                            if (getContext() != null) {
                                ToastHelper.showError(getContext(), "Error opening profile: " + e.getMessage());
                            }
                            e.printStackTrace();
                        }
                    });
                }
        
        // Feature cards
        if (cardBrowse != null) {
            cardBrowse.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(getContext(), BrowseActivity.class);
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    startActivity(intent);
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening browse: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
            });
        }
        
        if (cardSell != null) {
            cardSell.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(getContext(), PostActivity.class);
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    startActivity(intent);
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening post: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
            });
        }
        
        if (cardCredits != null) {
            cardCredits.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(getContext(), CreditsActivity.class);
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    startActivity(intent);
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening credits: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
            });
        }
        
        if (cardHelp != null) {
            cardHelp.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(getContext(), HelpSupportActivity.class);
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    startActivity(intent);
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening help: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
            });
        }
        
        // Promotional banner
        if (btnClaimOffer != null) {
            btnClaimOffer.setOnClickListener(v -> {
                try {
                    // Navigate to credits with special offer
                    Intent intent = new Intent(getContext(), CreditsActivity.class);
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    intent.putExtra("SPECIAL_OFFER", true);
                    startActivity(intent);
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening credits: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
            });
        }
        
        // Primary action buttons
        if (btnBuy != null) {
            btnBuy.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(getContext(), BrowseActivity.class);
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    startActivity(intent);
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening browse: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
            });
        }
        
        if (btnPost != null) {
            btnPost.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(getContext(), PostActivity.class);
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    startActivity(intent);
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening post: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
            });
        }
        
        // Logout button
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> {
                try {
                    // Navigate back to LoginActivity
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error during logout: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
            });
        }
    }
    
    private void loadUserData() {
        // Try to get user email from arguments first
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            if (getArguments() != null) {
                loggedInUserEmail = getArguments().getString("USER_EMAIL");
            }
        }
        
        // If still null, try to get it from MainActivity
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            if (getActivity() instanceof MainActivity) {
                loggedInUserEmail = ((MainActivity) getActivity()).getCurrentUserEmail();
            }
        }
        
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error: User not identified.");
            }
            return;
        }

        if (dbHelper == null) {
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error: Database not initialized.");
            }
            return;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
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
                if (textViewWelcome != null) {
                    textViewWelcome.setText("Welcome back!");
                }
                if (textViewAlias != null) {
                    textViewAlias.setText(alias != null ? alias : "User");
                }
                if (textViewCredits != null) {
                    textViewCredits.setText(String.format(Locale.getDefault(), "₱ %.2f", credits));
                }
            }
        } catch (Exception e) {
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error loading user data: " + e.getMessage());
            }
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        
        // Load additional data
        loadUserStats();
        updateOnboardingProgress();
    }
    
    /**
     * Load user statistics (active bids, items posted)
     */
    private void loadUserStats() {
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            return;
        }
        
        if (dbHelper == null) {
            return;
        }
        
        SQLiteDatabase db = null;
        Cursor bidsCursor = null;
        Cursor itemsCursor = null;
        
        try {
            db = dbHelper.getReadableDatabase();
            
            // Count active bids
            int activeBids = 0;
            bidsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_BIDS + 
                " WHERE " + DatabaseHelper.COLUMN_BID_BIDDER_EMAIL + " = ? AND " + 
                DatabaseHelper.COLUMN_BID_STATUS + " = 'ACTIVE'",
                new String[]{loggedInUserEmail}
            );
            if (bidsCursor != null && bidsCursor.moveToFirst()) {
                activeBids = bidsCursor.getInt(0);
            }
            
            // Count items posted
            int itemsPosted = 0;
            itemsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ITEMS + 
                " WHERE " + DatabaseHelper.COLUMN_ITEM_SELLER_EMAIL + " = ?",
                new String[]{loggedInUserEmail}
            );
            if (itemsCursor != null && itemsCursor.moveToFirst()) {
                itemsPosted = itemsCursor.getInt(0);
            }
            
            // Update UI
            if (textActiveBids != null) {
                textActiveBids.setText(String.valueOf(activeBids));
            }
            if (textItemsPosted != null) {
                textItemsPosted.setText(String.valueOf(itemsPosted));
            }
            
        } catch (Exception e) {
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error loading user stats: " + e.getMessage());
            }
            e.printStackTrace();
        } finally {
            if (bidsCursor != null) {
                bidsCursor.close();
            }
            if (itemsCursor != null) {
                itemsCursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }
    
    /**
     * Update onboarding progress based on user profile completion
     */
    private void updateOnboardingProgress() {
        if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
            return;
        }
        
        if (dbHelper == null) {
            return;
        }
        
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    DatabaseHelper.TABLE_USERS,
                    new String[]{
                        DatabaseHelper.COLUMN_USER_ALIAS,
                        DatabaseHelper.COLUMN_USER_PHONE,
                        DatabaseHelper.COLUMN_USER_PROFILE_PICTURE,
                        DatabaseHelper.COLUMN_USER_VERIFIED
                    },
                    DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
                    new String[]{loggedInUserEmail},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int completedSteps = 0;
                int totalSteps = 4;
                
                // Check if alias is set (step 1)
                String alias = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ALIAS));
                if (alias != null && !alias.isEmpty()) {
                    completedSteps++;
                }
                
                // Check if phone is set (step 2)
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PHONE));
                if (phone != null && !phone.isEmpty()) {
                    completedSteps++;
                }
                
                // Check if profile picture is set (step 3)
                String profilePicture = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PROFILE_PICTURE));
                if (profilePicture != null && !profilePicture.isEmpty()) {
                    completedSteps++;
                }
                
                // Check if account is verified (step 4)
                int verified = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_VERIFIED));
                if (verified == 1) {
                    completedSteps++;
                }
                
                // Update progress bar
                if (progressOnboarding != null) {
                    int progress = (completedSteps * 100) / totalSteps;
                    progressOnboarding.setProgress(progress);
                }
                
                // Update progress text
                if (textOnboardingProgress != null) {
                    textOnboardingProgress.setText(completedSteps + "/" + totalSteps);
                }
            }
        } catch (Exception e) {
            if (getContext() != null) {
                ToastHelper.showError(getContext(), "Error updating onboarding progress: " + e.getMessage());
            }
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }
    
    public void updateUserEmail(String email) {
        this.loggedInUserEmail = email;
        loadUserData();
    }
}
