package com.cc106.bidhub.utils;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import com.google.android.material.button.MaterialButton;

/**
 * Helper class to manage loading states consistently across activities
 * Reduces code duplication for loading state management
 */
public class LoadingStateHelper {
    
    private ProgressBar progressBar;
    private Button button;
    private MaterialButton materialButton;
    private String originalButtonText;
    
    /**
     * Constructor with ProgressBar and Button
     */
    public LoadingStateHelper(ProgressBar progressBar, Button button) {
        this.progressBar = progressBar;
        this.button = button;
        if (button != null) {
            this.originalButtonText = button.getText().toString();
        }
    }
    
    /**
     * Constructor with ProgressBar and MaterialButton
     */
    public LoadingStateHelper(ProgressBar progressBar, MaterialButton button) {
        this.progressBar = progressBar;
        this.materialButton = button;
        if (button != null) {
            this.originalButtonText = button.getText().toString();
        }
    }
    
    /**
     * Show or hide loading state
     * @param isLoading true to show loading, false to hide
     * @param loadingText optional text to show while loading (null to use default)
     */
    public void setLoading(boolean isLoading, String loadingText) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        
        if (button != null) {
            button.setEnabled(!isLoading);
            button.setText(isLoading ? (loadingText != null ? loadingText : "Loading...") : originalButtonText);
        }
        
        if (materialButton != null) {
            materialButton.setEnabled(!isLoading);
            materialButton.setText(isLoading ? (loadingText != null ? loadingText : "Loading...") : originalButtonText);
        }
    }
    
    /**
     * Show or hide loading state with default text
     */
    public void setLoading(boolean isLoading) {
        setLoading(isLoading, null);
    }
}

