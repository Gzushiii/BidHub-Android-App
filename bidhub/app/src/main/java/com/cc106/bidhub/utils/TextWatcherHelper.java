package com.cc106.bidhub.utils;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Helper class to create reusable TextWatcher implementations
 * Reduces code duplication for TextWatcher callbacks
 */
public class TextWatcherHelper {
    
    /**
     * Creates a simple TextWatcher that only handles onTextChanged
     * Useful for button state updates and simple validations
     */
    public static TextWatcher createSimpleWatcher(Runnable onTextChanged) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (onTextChanged != null) {
                    onTextChanged.run();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                // No-op
            }
        };
    }
    
    /**
     * Creates a TextWatcher with all three callbacks
     */
    public static TextWatcher createFullWatcher(
            Runnable beforeTextChanged,
            Runnable onTextChanged,
            Runnable afterTextChanged) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (beforeTextChanged != null) {
                    beforeTextChanged.run();
                }
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (onTextChanged != null) {
                    onTextChanged.run();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (afterTextChanged != null) {
                    afterTextChanged.run();
                }
            }
        };
    }
    
    /**
     * Empty TextWatcher implementation for cases where only one callback is needed
     */
    public static abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op by default
        }
        
        @Override
        public void afterTextChanged(Editable s) {
            // No-op by default
        }
    }
}

