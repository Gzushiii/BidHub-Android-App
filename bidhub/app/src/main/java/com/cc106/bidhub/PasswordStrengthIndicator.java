package com.cc106.bidhub;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

public class PasswordStrengthIndicator extends LinearLayout {
    
    private ProgressBar progressBar;
    private TextView strengthText;
    private TextView recommendationsText;
    private LinearLayout recommendationsContainer;
    
    public PasswordStrengthIndicator(Context context) {
        super(context);
        init(context);
    }
    
    public PasswordStrengthIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public PasswordStrengthIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        setPadding(0, 8, 0, 0);
        
        // Create strength indicator
        LinearLayout strengthLayout = new LinearLayout(context);
        strengthLayout.setOrientation(HORIZONTAL);
        strengthLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // Progress bar
        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(0, 8, 1));
        progressBar.setMax(6);
        progressBar.setProgress(0);
        // FIX: Convert color to ColorStateList - password_weak is a color, not a ColorStateList
        int weakColor = ContextCompat.getColor(context, R.color.password_weak);
        progressBar.setProgressTintList(ColorStateList.valueOf(weakColor));
        
        // Strength text
        strengthText = new TextView(context);
        strengthText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        strengthText.setTextSize(12);
        strengthText.setTextColor(ContextCompat.getColor(context, R.color.text_hint));
        strengthText.setPadding(16, 0, 0, 0);
        strengthText.setText("Enter password");
        
        strengthLayout.addView(progressBar);
        strengthLayout.addView(strengthText);
        
        // Recommendations container
        recommendationsContainer = new LinearLayout(context);
        recommendationsContainer.setOrientation(VERTICAL);
        recommendationsContainer.setVisibility(GONE);
        
        recommendationsText = new TextView(context);
        recommendationsText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        recommendationsText.setTextSize(11);
        recommendationsText.setTextColor(ContextCompat.getColor(context, R.color.text_hint));
        recommendationsText.setPadding(0, 4, 0, 0);
        recommendationsText.setLineSpacing(2, 1.2f);
        
        recommendationsContainer.addView(recommendationsText);
        
        addView(strengthLayout);
        addView(recommendationsContainer);
    }
    
    public void updateStrength(ValidationUtils.PasswordStrengthResult result) {
        if (result == null) {
            progressBar.setProgress(0);
            strengthText.setText("Enter password");
            strengthText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_hint));
            recommendationsContainer.setVisibility(GONE);
            return;
        }
        
        // Update progress bar
        progressBar.setProgress(result.score);
        
        // Update strength text and color
        strengthText.setText(result.message);
        
        int colorRes;
        if (result.score <= 1) {
            colorRes = R.color.password_weak;
        } else if (result.score <= 2) {
            colorRes = R.color.password_weak;
        } else if (result.score <= 4) {
            colorRes = R.color.password_fair;
        } else if (result.score <= 6) {
            colorRes = R.color.password_good;
        } else {
            colorRes = R.color.password_strong;
        }
        
        strengthText.setTextColor(ContextCompat.getColor(getContext(), colorRes));
        // FIX: Convert color to ColorStateList - password colors are colors, not ColorStateLists
        int color = ContextCompat.getColor(getContext(), colorRes);
        progressBar.setProgressTintList(ColorStateList.valueOf(color));
        
        // Update recommendations
        if (!result.recommendations.isEmpty()) {
            recommendationsText.setText(result.recommendations);
            recommendationsContainer.setVisibility(VISIBLE);
        } else {
            recommendationsContainer.setVisibility(GONE);
        }
    }
    
    public void clear() {
        progressBar.setProgress(0);
        strengthText.setText("Enter password");
        strengthText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_hint));
        // FIX: Convert color to ColorStateList - password_weak is a color, not a ColorStateList
        int weakColor = ContextCompat.getColor(getContext(), R.color.password_weak);
        progressBar.setProgressTintList(ColorStateList.valueOf(weakColor));
        recommendationsContainer.setVisibility(GONE);
    }
}
