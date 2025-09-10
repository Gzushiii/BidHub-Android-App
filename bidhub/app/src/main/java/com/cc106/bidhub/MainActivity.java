package com.cc106.bidhub;

import android.content.Intent;
import android.os.Bundle;
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
    
    private void showFragment(Fragment fragment) {
        try {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            );
            transaction.replace(R.id.content_frame, fragment);
            transaction.commit();
        } catch (Exception e) {
            Toast.makeText(this, "Error showing fragment: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        // Don't navigate if already on the current fragment
        if (isCurrentActivity(itemId)) {
            return true;
        }
        
        if (itemId == R.id.nav_home) {
            showFragment(homeFragment);
        } else if (itemId == R.id.nav_browse) {
            showFragment(browseFragment);
        } else if (itemId == R.id.nav_post) {
            showFragment(postFragment);
        } else if (itemId == R.id.nav_credits) {
            showFragment(creditsFragment);
        } else if (itemId == R.id.nav_profile) {
            showFragment(profileFragment);
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
