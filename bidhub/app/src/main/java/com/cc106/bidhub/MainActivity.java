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
            homeFragment = new HomeFragment();
            browseFragment = new BrowseFragment();
            postFragment = new PostFragment();
            creditsFragment = new CreditsFragment();
            profileFragment = new ProfileFragment();
            
            // Pass user email to all fragments
            Bundle args = new Bundle();
            args.putString("USER_EMAIL", loggedInUserEmail);
            
            homeFragment.setArguments(args);
            browseFragment.setArguments(args);
            postFragment.setArguments(args);
            creditsFragment.setArguments(args);
            profileFragment.setArguments(args);
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
        
        // Show fragment with directional intelligence
        if (itemId == R.id.nav_home) {
            showFragment(homeFragment, newTabPosition);
        } else if (itemId == R.id.nav_browse) {
            showFragment(browseFragment, newTabPosition);
        } else if (itemId == R.id.nav_post) {
            showFragment(postFragment, newTabPosition);
        } else if (itemId == R.id.nav_credits) {
            showFragment(creditsFragment, newTabPosition);
        } else if (itemId == R.id.nav_profile) {
            showFragment(profileFragment, newTabPosition);
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
}
