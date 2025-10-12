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
import com.cc106.bidhub.MyListingsActivity;
import com.cc106.bidhub.PostActivity;
import com.cc106.bidhub.ProfileActivity;
import com.cc106.bidhub.R;

import java.util.Locale;

public class HomeFragment extends Fragment {

    // Header components
    private ImageButton btnNotifications;
    private ImageView imgProfile;
    private TextView textViewWelcome, textViewCredits, textViewAlias;
    private View searchBar;
    
    // Quick action buttons
    private View cardBrowse, cardSell, cardCredits, cardMyListings;
    
    // Featured auctions
    private TextView textFeaturedAuctions;
    
    // Active bids
    private TextView textActiveBids;
    
    // Quick stats cards
    private View cardActiveBids, cardWatching, cardWonItems, cardSoldItems;
    private TextView tvActiveBidsCount, tvWatchingCount, tvWonItemsCount, tvSoldItemsCount;
    
    // Recent activity
    private TextView textRecentActivity;
    private View layoutRecentActivity;
    
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
            
            // Load quick stats
            loadQuickStats();
            
            // Load recent activity
            loadRecentActivity();
            
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
        btnNotifications = view.findViewById(R.id.btnNotifications);
        imgProfile = view.findViewById(R.id.imgProfile);
        textViewWelcome = view.findViewById(R.id.textViewWelcome);
        textViewCredits = view.findViewById(R.id.textViewCredits);
        textViewAlias = view.findViewById(R.id.textViewAlias);
        searchBar = view.findViewById(R.id.searchBar);
        
        // Quick action buttons
        cardBrowse = view.findViewById(R.id.cardBrowse);
        cardSell = view.findViewById(R.id.cardSell);
        cardCredits = view.findViewById(R.id.cardCredits);
        cardMyListings = view.findViewById(R.id.cardMyListings);
        
        // Featured auctions
        textFeaturedAuctions = view.findViewById(R.id.textFeaturedAuctions);
        
        // Active bids
        textActiveBids = view.findViewById(R.id.textActiveBids);
        
        // Logout button
        buttonLogout = view.findViewById(R.id.buttonLogout);
    }
    
    /**
     * Set up click listeners for all interactive elements
     */
    private void setupClickListeners() {
        // Header buttons
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                // TODO: Navigate to notifications
                ToastHelper.showInfo(getContext(), "Notifications clicked");
            });
        }
        
        // Search bar click listener
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                try {
                    // Navigate to browse tab
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).switchToBrowseTab();
                    }
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening browse: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
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
        
        if (cardMyListings != null) {
            cardMyListings.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(getContext(), MyListingsActivity.class);
                    intent.putExtra("USER_EMAIL", loggedInUserEmail);
                    startActivity(intent);
                } catch (Exception e) {
                    if (getContext() != null) {
                        ToastHelper.showError(getContext(), "Error opening my listings: " + e.getMessage());
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
    
    
    public void updateUserEmail(String email) {
        this.loggedInUserEmail = email;
        loadUserData();
    }
    
    /**
     * Load quick stats for dashboard cards
     */
    private void loadQuickStats() {
        if (dbHelper == null || loggedInUserEmail == null) {
            return;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            
            // Get user ID
            String userId = getCurrentUserId();
            if (userId == null) {
                return;
            }
            
            // Count active bids
            int activeBids = 0;
            Cursor bidsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM bids WHERE bidder_id = ? AND status = 'ACTIVE'",
                new String[]{userId}
            );
            if (bidsCursor.moveToFirst()) {
                activeBids = bidsCursor.getInt(0);
            }
            bidsCursor.close();
            
            // Count watching items (placeholder - would need watchlist table)
            int watchingItems = 0;
            
            // Count won items
            int wonItems = 0;
            Cursor wonCursor = db.rawQuery(
                "SELECT COUNT(*) FROM bids WHERE bidder_id = ? AND status = 'WINNING'",
                new String[]{userId}
            );
            if (wonCursor.moveToFirst()) {
                wonItems = wonCursor.getInt(0);
            }
            wonCursor.close();
            
            // Count sold items
            int soldItems = 0;
            Cursor soldCursor = db.rawQuery(
                "SELECT COUNT(*) FROM items WHERE seller_id = ? AND status = 'ENDED'",
                new String[]{userId}
            );
            if (soldCursor.moveToFirst()) {
                soldItems = soldCursor.getInt(0);
            }
            soldCursor.close();
            
            // Update UI if views exist
            if (tvActiveBidsCount != null) {
                tvActiveBidsCount.setText(String.valueOf(activeBids));
            }
            if (tvWatchingCount != null) {
                tvWatchingCount.setText(String.valueOf(watchingItems));
            }
            if (tvWonItemsCount != null) {
                tvWonItemsCount.setText(String.valueOf(wonItems));
            }
            if (tvSoldItemsCount != null) {
                tvSoldItemsCount.setText(String.valueOf(soldItems));
            }
            
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading quick stats: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load recent activity for dashboard
     */
    private void loadRecentActivity() {
        if (dbHelper == null || loggedInUserEmail == null) {
            return;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String userId = getCurrentUserId();
            if (userId == null) {
                return;
            }
            
            // Get recent bids
            Cursor recentBidsCursor = db.rawQuery(
                "SELECT b.amount, i.title, b.placed_at FROM bids b " +
                "JOIN items i ON b.item_id = i.id " +
                "WHERE b.bidder_id = ? " +
                "ORDER BY b.placed_at DESC LIMIT 5",
                new String[]{userId}
            );
            
            // TODO: Display recent activity in layoutRecentActivity
            // This would show recent bids, won items, etc.
            
            recentBidsCursor.close();
            
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading recent activity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get current user ID from email
     */
    private String getCurrentUserId() {
        if (dbHelper == null || loggedInUserEmail == null) {
            return null;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                "SELECT id FROM users WHERE email = ?",
                new String[]{loggedInUserEmail}
            );
            
            String userId = null;
            if (cursor.moveToFirst()) {
                userId = cursor.getString(0);
            }
            cursor.close();
            return userId;
            
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error getting user ID: " + e.getMessage(), e);
            return null;
        }
    }
}
