package com.cc106.bidhub.utils;

import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.cc106.bidhub.R;
import java.io.File;

/**
 * Utility class for centralized image loading using Glide
 * Handles different image sources: local file paths, URIs, and sample/placeholder images
 */
public class ImageLoader {
    
    private static final String TAG = "ImageLoader";
    
    /**
     * Load image from file path into ImageView
     * @param context Android context
     * @param imagePath Path to the image file
     * @param imageView Target ImageView
     */
    public static void loadImage(Context context, String imagePath, ImageView imageView) {
        if (context == null || imageView == null) {
            android.util.Log.w(TAG, "Context or ImageView is null, cannot load image");
            return;
        }
        
        if (imagePath == null || imagePath.isEmpty()) {
            loadPlaceholder(context, imageView);
            return;
        }
        
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                // Load image from file using Glide
                Glide.with(context)
                    .load(imageFile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(imageView);
                
                android.util.Log.d(TAG, "Successfully loaded image from path: " + imagePath);
            } else {
                android.util.Log.w(TAG, "Image file does not exist: " + imagePath);
                loadPlaceholder(context, imageView);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error loading image from path: " + imagePath, e);
            loadPlaceholder(context, imageView);
        }
    }
    
    /**
     * Load circular image from file path
     * @param context Android context
     * @param imagePath Path to the image file
     * @param imageView Target ImageView
     */
    public static void loadCircularImage(Context context, String imagePath, ImageView imageView) {
        if (context == null || imageView == null) {
            android.util.Log.w(TAG, "Context or ImageView is null, cannot load circular image");
            return;
        }
        
        if (imagePath == null || imagePath.isEmpty()) {
            loadPlaceholder(context, imageView);
            return;
        }
        
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                // Load circular image using Glide
                Glide.with(context)
                    .load(imageFile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .apply(RequestOptions.circleCropTransform())
                    .into(imageView);
                
                android.util.Log.d(TAG, "Successfully loaded circular image from path: " + imagePath);
            } else {
                android.util.Log.w(TAG, "Image file does not exist for circular load: " + imagePath);
                loadPlaceholder(context, imageView);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error loading circular image from path: " + imagePath, e);
            loadPlaceholder(context, imageView);
        }
    }
    
    /**
     * Load sample image from drawable resources
     * Used for sample/demo data
     * @param context Android context
     * @param drawableResId Drawable resource ID
     * @param imageView Target ImageView
     */
    public static void loadSampleImage(Context context, int drawableResId, ImageView imageView) {
        if (context == null || imageView == null) {
            android.util.Log.w(TAG, "Context or ImageView is null, cannot load sample image");
            return;
        }
        
        try {
            Glide.with(context)
                .load(drawableResId)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(imageView);
            
            android.util.Log.d(TAG, "Successfully loaded sample image with resource ID: " + drawableResId);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error loading sample image with resource ID: " + drawableResId, e);
            loadPlaceholder(context, imageView);
        }
    }
    
    /**
     * Load placeholder image
     * @param context Android context
     * @param imageView Target ImageView
     */
    public static void loadPlaceholder(Context context, ImageView imageView) {
        if (context == null || imageView == null) {
            android.util.Log.w(TAG, "Context or ImageView is null, cannot load placeholder");
            return;
        }
        
        try {
            imageView.setImageResource(R.drawable.placeholder);
            android.util.Log.d(TAG, "Loaded placeholder image");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error loading placeholder image", e);
        }
    }
    
    /**
     * Load image with custom error handling
     * @param context Android context
     * @param imagePath Path to the image file
     * @param imageView Target ImageView
     * @param errorCallback Callback for error handling
     */
    public static void loadImageWithErrorCallback(Context context, String imagePath, ImageView imageView, 
                                                ImageLoadErrorCallback errorCallback) {
        if (context == null || imageView == null) {
            if (errorCallback != null) {
                errorCallback.onError("Context or ImageView is null");
            }
            return;
        }
        
        if (imagePath == null || imagePath.isEmpty()) {
            loadPlaceholder(context, imageView);
            if (errorCallback != null) {
                errorCallback.onError("Image path is null or empty");
            }
            return;
        }
        
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Glide.with(context)
                    .load(imageFile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, 
                                                 Object model, 
                                                 com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                 boolean isFirstResource) {
                            if (errorCallback != null) {
                                errorCallback.onError("Failed to load image: " + (e != null ? e.getMessage() : "Unknown error"));
                            }
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                                                     Object model, 
                                                     com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                     com.bumptech.glide.load.DataSource dataSource, 
                                                     boolean isFirstResource) {
                            android.util.Log.d(TAG, "Successfully loaded image from path: " + imagePath);
                            return false;
                        }
                    })
                    .into(imageView);
            } else {
                android.util.Log.w(TAG, "Image file does not exist: " + imagePath);
                loadPlaceholder(context, imageView);
                if (errorCallback != null) {
                    errorCallback.onError("Image file does not exist: " + imagePath);
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error loading image from path: " + imagePath, e);
            loadPlaceholder(context, imageView);
            if (errorCallback != null) {
                errorCallback.onError("Exception loading image: " + e.getMessage());
            }
        }
    }
    
    /**
     * Interface for image loading error callbacks
     */
    public interface ImageLoadErrorCallback {
        void onError(String errorMessage);
    }
}

