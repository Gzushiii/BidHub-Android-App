package com.cc106.bidhub;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import com.cc106.bidhub.toast.ToastHelper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.cc106.bidhub.fragments.BrowseFragment;
import com.cc106.bidhub.fragments.CreditsFragment;
import com.cc106.bidhub.fragments.HomeFragment;
import com.cc106.bidhub.fragments.PostFragment;
import com.cc106.bidhub.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends BaseActivity {

    private String loggedInUserEmail;
    private HomeFragment homeFragment;
    private BrowseFragment browseFragment;
    private PostFragment postFragment;
    private CreditsFragment creditsFragment;
    private ProfileFragment profileFragment;
    
    // UI Components
    private ProgressBar loadingIndicator;
    private View contentFrame;
    
    // Tab position mapping for directional intelligence
    private static final int TAB_HOME = 0;
    private static final int TAB_BROWSE = 1;
    private static final int TAB_POST = 2;
    private static final int TAB_CREDITS = 3;
    private static final int TAB_PROFILE = 4;
    
    private int currentTabPosition = TAB_HOME;
    private boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Initialize UI components
            initializeUI();
            
            // Get the logged-in user's email from the Intent
            loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");

            if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
                ToastHelper.showError(this, "Error: No user email provided");
                finish();
                return;
            }
            
            // Show loading state
            setLoadingState(true);
            
            // Initialize fragments asynchronously
            initializeFragmentsAsync();
            
        } catch (Exception e) {
            ToastHelper.showError(this, "Error: " + e.getMessage());
            e.printStackTrace();
            setLoadingState(false);
        }
    }
    
    /**
     * Initialize UI components
     */
    private void initializeUI() {
        try {
            contentFrame = findViewById(R.id.content_frame);
            loadingIndicator = findViewById(R.id.loading_indicator);
            
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            ToastHelper.showError(this, "Error initializing UI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize fragments asynchronously for better performance
     */
    private void initializeFragmentsAsync() {
        new Thread(() -> {
            try {
                // Initialize fragments
                initializeFragments();
                
                // Run UI updates on main thread
                runOnUiThread(() -> {
                    if (homeFragment == null) {
                        ToastHelper.showError(this, "Error: HomeFragment is null, cannot proceed");
                        finish();
                        return;
                    }
                    
                    // Ensure arguments are set before showing the fragment
                    setFragmentArguments();
                    
                    // Show home fragment by default
                    showFragment(homeFragment);

                    // Set selected navigation item
                    setCurrentTabSelected();
                    
                    // Hide loading state
                    setLoadingState(false);
                    isInitialized = true;
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    ToastHelper.showError(this, "Error initializing app: " + e.getMessage());
                    setLoadingState(false);
                });
            }
        }).start();
    }
    
    private void initializeFragments() {
        try {
            // Create fragments one by one with individual error handling
            homeFragment = new HomeFragment();
            if (homeFragment == null) {
                ToastHelper.showError(this, "Error: Failed to create HomeFragment");
                return;
            }
            
            browseFragment = new BrowseFragment();
            if (browseFragment == null) {
                ToastHelper.showError(this, "Error: Failed to create BrowseFragment");
                return;
            }
            
            postFragment = new PostFragment();
            if (postFragment == null) {
                ToastHelper.showError(this, "Error: Failed to create PostFragment");
                return;
            }
            
            creditsFragment = new CreditsFragment();
            if (creditsFragment == null) {
                ToastHelper.showError(this, "Error: Failed to create CreditsFragment");
                return;
            }
            
            profileFragment = new ProfileFragment();
            if (profileFragment == null) {
                ToastHelper.showError(this, "Error: Failed to create ProfileFragment");
                return;
            }
            
            // Always pass user email to all fragments (important for re-initialization)
            setFragmentArguments();
            
        } catch (Exception e) {
            ToastHelper.showError(this, "Error initializing fragments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Set arguments for all fragments
     */
    private void setFragmentArguments() {
        if (loggedInUserEmail != null && !loggedInUserEmail.isEmpty()) {
            Bundle args = new Bundle();
            args.putString("USER_EMAIL", loggedInUserEmail);
            
            if (homeFragment != null) {
                homeFragment.setArguments(args);
            }
            if (browseFragment != null) {
                browseFragment.setArguments(args);
            }
            if (postFragment != null) {
                postFragment.setArguments(args);
            }
            if (creditsFragment != null) {
                creditsFragment.setArguments(args);
            }
            if (profileFragment != null) {
                profileFragment.setArguments(args);
            }
        }
    }
    
    /**
     * Get tab position from menu item ID
     */
    private int getTabPosition(int itemId) {
        if (itemId == R.id.nav_home) return TAB_HOME;
        if (itemId == R.id.nav_browse) return TAB_BROWSE;
        if (itemId == R.id.nav_post) return TAB_POST;
        if (itemId == R.id.nav_credits) return TAB_CREDITS;
        if (itemId == R.id.nav_profile) return TAB_PROFILE;
        return TAB_HOME; // Default fallback
    }
    
    /**
     * Show fragment with directional intelligence
     */
    private void showFragment(Fragment fragment, int newTabPosition) {
        try {
            // Check if fragment is null
            if (fragment == null) {
                ToastHelper.showError(this, "Error: Fragment is null");
                setLoadingState(false);
                return;
            }
            
            // Check if content frame exists
            if (findViewById(R.id.content_frame) == null) {
                ToastHelper.showError(this, "Error: Content frame not found");
                setLoadingState(false);
                return;
            }
            
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            
            // Determine slide direction based on tab position
            int enterAnim, exitAnim;
            if (newTabPosition > currentTabPosition) {
                // Moving forward (left to right) - slide in from right
                enterAnim = R.anim.slide_in_right_smooth;
                exitAnim = R.anim.slide_out_left_smooth;
            } else if (newTabPosition < currentTabPosition) {
                // Moving backward (right to left) - slide in from left
                enterAnim = R.anim.slide_in_left_smooth;
                exitAnim = R.anim.slide_out_right_smooth;
            } else {
                // Same tab - use fade for subtle effect
                enterAnim = R.anim.fade_in;
                exitAnim = R.anim.fade_out;
            }
            
            transaction.setCustomAnimations(enterAnim, exitAnim, enterAnim, exitAnim);
            transaction.replace(R.id.content_frame, fragment);
            transaction.commit();
            
            // Update current tab position
            currentTabPosition = newTabPosition;
            
            // Provide subtle haptic feedback for tab switch
            provideHapticFeedback();
            
            // Hide loading state after a short delay to ensure smooth transition
            contentFrame.postDelayed(() -> setLoadingState(false), 200);
            
        } catch (Exception e) {
            ToastHelper.showError(this, "Error showing fragment: " + e.getMessage());
            e.printStackTrace();
            setLoadingState(false);
        }
    }
    
    /**
     * Show fragment with default home position (for initial load)
     */
    private void showFragment(Fragment fragment) {
        showFragment(fragment, TAB_HOME);
    }
    
    /**
     * Provide subtle haptic feedback for tab switches
     */
    private void provideHapticFeedback() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    VibrationEffect effect = VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE);
                    vibrator.vibrate(effect);
                } else {
                    vibrator.vibrate(30);
                }
            }
        } catch (Exception e) {
            // Haptic feedback is optional, don't crash if it fails
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Don't handle navigation if not initialized
        if (!isInitialized) {
            return false;
        }
        
        int itemId = item.getItemId();
        int newTabPosition = getTabPosition(itemId);
        
        // Don't navigate if already on the current fragment
        if (isCurrentActivity(itemId)) {
            return true;
        }
        
        // Show loading state for navigation
        setLoadingState(true);
        
        // Check if fragments need to be re-initialized
        ensureFragmentsInitialized();
        
        // Show fragment with directional intelligence
        Fragment targetFragment = null;
        if (itemId == R.id.nav_home) {
            targetFragment = homeFragment;
        } else if (itemId == R.id.nav_browse) {
            targetFragment = browseFragment;
        } else if (itemId == R.id.nav_post) {
            targetFragment = postFragment;
        } else if (itemId == R.id.nav_credits) {
            targetFragment = creditsFragment;
        } else if (itemId == R.id.nav_profile) {
            targetFragment = profileFragment;
        }
        
        if (targetFragment != null) {
            showFragment(targetFragment, newTabPosition);
        } else {
            ToastHelper.showError(this, "Navigation error: Fragment not found");
            setLoadingState(false);
        }
        
        return true;
    }

    @Override
    protected boolean isCurrentActivity(int itemId) {
        // For fragments, we'll check which fragment is currently displayed
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        
        if (itemId == R.id.nav_home) {
            return currentFragment instanceof HomeFragment;
        } else if (itemId == R.id.nav_browse) {
            return currentFragment instanceof BrowseFragment;
        } else if (itemId == R.id.nav_post) {
            return currentFragment instanceof PostFragment;
        } else if (itemId == R.id.nav_credits) {
            return currentFragment instanceof CreditsFragment;
        } else if (itemId == R.id.nav_profile) {
            return currentFragment instanceof ProfileFragment;
        }
        
        return false;
    }

    @Override
    protected void setCurrentTabSelected() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    @Override
    public String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
    
    /**
     * Check and re-initialize fragments if needed
     */
    private void ensureFragmentsInitialized() {
        if (homeFragment == null || browseFragment == null || postFragment == null || 
            creditsFragment == null || profileFragment == null) {
            initializeFragments();
        } else {
            // Even if fragments exist, ensure they have the correct arguments
            setFragmentArguments();
        }
    }
    
    /**
     * Set loading state for UI elements
     */
    @Override
    protected void setLoadingState(boolean isLoading) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        
        if (contentFrame != null) {
            contentFrame.setAlpha(isLoading ? 0.7f : 1.0f);
        }
        
        // Disable bottom navigation during loading
        if (bottomNavigationView != null) {
            bottomNavigationView.setEnabled(!isLoading);
        }
    }
    
    /**
     * Switch to browse tab programmatically
     * @param categoryFilter Optional category name to filter by
     */
    public void switchToBrowseTab(String categoryFilter) {
        try {
            if (bottomNavigationView != null) {
                bottomNavigationView.setSelectedItemId(R.id.nav_browse);
                
                // If category filter is provided, pass it to BrowseActivity
                if (categoryFilter != null && !categoryFilter.isEmpty()) {
                    Intent browseIntent = new Intent(this, BrowseActivity.class);
                    browseIntent.putExtra("USER_EMAIL", getCurrentUserEmail());
                    browseIntent.putExtra(BrowseActivity.EXTRA_CATEGORY_FILTER, categoryFilter);
                    startActivity(browseIntent);
                    return;
                }
                
                // Force refresh the browse fragment after navigation
                android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.postDelayed(() -> {
                    try {
                        // Get the current fragment and refresh it
                        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager()
                                .findFragmentById(R.id.content_frame);
                        if (currentFragment instanceof com.cc106.bidhub.fragments.BrowseFragment) {
                            com.cc106.bidhub.fragments.BrowseFragment browseFragment = 
                                (com.cc106.bidhub.fragments.BrowseFragment) currentFragment;
                            
                            // Apply category filter if provided
                            if (categoryFilter != null && !categoryFilter.isEmpty()) {
                                browseFragment.setCategoryFilter(categoryFilter);
                            } else {
                                browseFragment.loadItems();
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Error refreshing browse fragment", e);
                    }
                }, 500); // Small delay to ensure navigation is complete
            }
        } catch (Exception e) {
            ToastHelper.showError(this, "Error switching to browse tab: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Switch to browse tab programmatically (overload without category filter)
     */
    public void switchToBrowseTab() {
        switchToBrowseTab(null);
    }
    
    /**
     * Switch to profile tab programmatically
     */
    public void switchToProfileTab() {
        try {
            if (bottomNavigationView != null) {
                bottomNavigationView.setSelectedItemId(R.id.nav_profile);
            }
        } catch (Exception e) {
            ToastHelper.showError(this, "Error switching to profile tab: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle back button press
     */
    @Override
    public void onBackPressed() {
        // If not on home tab, navigate to home
        if (currentTabPosition != TAB_HOME) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            // If on home tab, show exit confirmation
            super.onBackPressed();
        }
    }
}
