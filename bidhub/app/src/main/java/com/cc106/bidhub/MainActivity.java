package com.cc106.bidhub;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MenuItem;
import android.widget.Toast;

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
    
    // Tab position mapping for directional intelligence
    private static final int TAB_HOME = 0;
    private static final int TAB_BROWSE = 1;
    private static final int TAB_POST = 2;
    private static final int TAB_CREDITS = 3;
    private static final int TAB_PROFILE = 4;
    
    private int currentTabPosition = TAB_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Get the logged-in user's email from the Intent
            loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");

            if (loggedInUserEmail == null || loggedInUserEmail.isEmpty()) {
                Toast.makeText(this, "Error: No user email provided", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            // Initialize fragments
            initializeFragments();
            
            // Check if fragments were initialized successfully before showing
            if (homeFragment == null) {
                Toast.makeText(this, "Error: HomeFragment is null, cannot proceed", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            // Show home fragment by default
            showFragment(homeFragment);

            // Set selected navigation item
            setCurrentTabSelected();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    private void initializeFragments() {
        try {
            Toast.makeText(this, "Starting fragment initialization...", Toast.LENGTH_SHORT).show();
            
            // Create fragments one by one with individual error handling
            homeFragment = new HomeFragment();
            Toast.makeText(this, "HomeFragment created: " + (homeFragment != null), Toast.LENGTH_SHORT).show();
            if (homeFragment == null) {
                Toast.makeText(this, "Error: Failed to create HomeFragment", Toast.LENGTH_LONG).show();
                return;
            }
            
            browseFragment = new BrowseFragment();
            if (browseFragment == null) {
                Toast.makeText(this, "Error: Failed to create BrowseFragment", Toast.LENGTH_LONG).show();
                return;
            }
            
            postFragment = new PostFragment();
            if (postFragment == null) {
                Toast.makeText(this, "Error: Failed to create PostFragment", Toast.LENGTH_LONG).show();
                return;
            }
            
            creditsFragment = new CreditsFragment();
            if (creditsFragment == null) {
                Toast.makeText(this, "Error: Failed to create CreditsFragment", Toast.LENGTH_LONG).show();
                return;
            }
            
            profileFragment = new ProfileFragment();
            if (profileFragment == null) {
                Toast.makeText(this, "Error: Failed to create ProfileFragment", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Pass user email to all fragments
            Bundle args = new Bundle();
            args.putString("USER_EMAIL", loggedInUserEmail);
            
            homeFragment.setArguments(args);
            browseFragment.setArguments(args);
            postFragment.setArguments(args);
            creditsFragment.setArguments(args);
            profileFragment.setArguments(args);
            
            Toast.makeText(this, "All fragments initialized successfully", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing fragments: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
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
                Toast.makeText(this, "Error: Fragment is null", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if content frame exists
            if (findViewById(R.id.content_frame) == null) {
                Toast.makeText(this, "Error: Content frame not found", Toast.LENGTH_SHORT).show();
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
            
        } catch (Exception e) {
            Toast.makeText(this, "Error showing fragment: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
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
        int itemId = item.getItemId();
        int newTabPosition = getTabPosition(itemId);
        
        // Don't navigate if already on the current fragment
        if (isCurrentActivity(itemId)) {
            return true;
        }
        
        // Check if fragments need to be re-initialized
        ensureFragmentsInitialized();
        
        // Show fragment with directional intelligence
        if (itemId == R.id.nav_home) {
            if (homeFragment != null) {
                showFragment(homeFragment, newTabPosition);
            } else {
                Toast.makeText(this, "Error: HomeFragment not available after re-initialization", Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.nav_browse) {
            if (browseFragment != null) {
                showFragment(browseFragment, newTabPosition);
            } else {
                Toast.makeText(this, "Error: BrowseFragment not available", Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.nav_post) {
            if (postFragment != null) {
                showFragment(postFragment, newTabPosition);
            } else {
                Toast.makeText(this, "Error: PostFragment not available", Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.nav_credits) {
            if (creditsFragment != null) {
                showFragment(creditsFragment, newTabPosition);
            } else {
                Toast.makeText(this, "Error: CreditsFragment not available", Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.nav_profile) {
            if (profileFragment != null) {
                showFragment(profileFragment, newTabPosition);
            } else {
                Toast.makeText(this, "Error: ProfileFragment not available", Toast.LENGTH_SHORT).show();
            }
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
    protected String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
    
    /**
     * Check and re-initialize fragments if needed
     */
    private void ensureFragmentsInitialized() {
        if (homeFragment == null || browseFragment == null || postFragment == null || 
            creditsFragment == null || profileFragment == null) {
            Toast.makeText(this, "Some fragments are null, re-initializing...", Toast.LENGTH_SHORT).show();
            initializeFragments();
        }
    }
}
