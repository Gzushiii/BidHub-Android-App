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
     * Check if the image path is a URL (HTTP/HTTPS)
     * @param imagePath Path to check
     * @return true if it's a URL, false if it's a local file path
     */
    private static boolean isUrl(String imagePath) {
        return imagePath != null && (imagePath.startsWith("http://") || imagePath.startsWith("https://"));
    }
    
    /**
     * Check if the image path is a sample image name (e.g., "sample_watch_1")
     * @param imagePath Path to check
     * @return true if it's a sample image name, false otherwise
     */
    private static boolean isSampleImageName(String imagePath) {
        return imagePath != null && imagePath.startsWith("sample_");
    }
    
    /**
     * Get drawable resource ID for a sample image name
     * @param sampleName Sample image name (e.g., "sample_watch_1", "sample_watch_2")
     * @return Drawable resource ID, or R.drawable.placeholder if not found
     */
    private static int getSampleImageResourceId(String sampleName) {
        if (sampleName == null) {
            return R.drawable.placeholder;
        }
        
        // Map sample image names to drawable resources
        // All sample_watch variants map to sample_watch since only one exists
        if (sampleName.startsWith("sample_watch")) {
            return R.drawable.sample_watch;
        } else if (sampleName.startsWith("sample_sofa")) {
            return R.drawable.sample_sofa;
        } else if (sampleName.startsWith("sample_handbag")) {
            return R.drawable.sample_handbag;
        } else if (sampleName.startsWith("sample_coin")) {
            return R.drawable.sample_coin;
        } else if (sampleName.startsWith("sample_camera")) {
            return R.drawable.sample_camera;
        } else if (sampleName.startsWith("sample_auction_1")) {
            return R.drawable.sample_auction_1;
        } else if (sampleName.startsWith("sample_auction_2")) {
            return R.drawable.sample_auction_2;
        } else if (sampleName.startsWith("sample_auction_3")) {
            return R.drawable.sample_auction_3;
        } else if (sampleName.startsWith("sample_bid_1")) {
            return R.drawable.sample_bid_1;
        } else if (sampleName.startsWith("sample_bid_2")) {
            return R.drawable.sample_bid_2;
        } else if (sampleName.startsWith("sample_avatar")) {
            return R.drawable.sample_avatar;
        } else {
            // Default fallback
            return R.drawable.placeholder;
        }
    }
    
    /**
     * Load image from file path or URL into ImageView
     * @param context Android context
     * @param imagePath Path to the image file or URL
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
            // Check if it's a URL, sample image name, or local file path
            if (isUrl(imagePath)) {
                // Load image from URL using Glide with error handling
                Glide.with(context)
                    .load(imagePath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, 
                                                 Object model, 
                                                 com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                 boolean isFirstResource) {
                            // Log at debug level for 404 errors (expected for missing images)
                            if (e != null && e.getCause() instanceof java.io.FileNotFoundException) {
                                android.util.Log.d(TAG, "Image not found (404): " + imagePath + " - showing placeholder");
                            } else {
                                android.util.Log.w(TAG, "Failed to load image from URL: " + imagePath, e);
                            }
                            // Return false to allow Glide to show the error placeholder
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                                                     Object model, 
                                                     com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                     com.bumptech.glide.load.DataSource dataSource, 
                                                     boolean isFirstResource) {
                            android.util.Log.d(TAG, "Successfully loaded image from URL: " + imagePath);
                            return false;
                        }
                    })
                    .into(imageView);
                
                android.util.Log.d(TAG, "Loading image from URL: " + imagePath);
            } else if (isSampleImageName(imagePath)) {
                // Load sample image from drawable resource
                int drawableResId = getSampleImageResourceId(imagePath);
                loadSampleImage(context, drawableResId, imageView);
                android.util.Log.d(TAG, "Loading sample image: " + imagePath + " -> resource ID: " + drawableResId);
            } else {
                // Load from local file path
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
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error loading image from path: " + imagePath, e);
            loadPlaceholder(context, imageView);
        }
    }
    
    /**
     * Load circular image from file path or URL
     * @param context Android context
     * @param imagePath Path to the image file or URL
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
            // Check if it's a URL, sample image name, or local file path
            if (isUrl(imagePath)) {
                // Load circular image from URL using Glide with error handling
                Glide.with(context)
                    .load(imagePath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .apply(RequestOptions.circleCropTransform())
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, 
                                                 Object model, 
                                                 com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                 boolean isFirstResource) {
                            // Log at debug level for 404 errors (expected for missing images)
                            if (e != null && e.getCause() instanceof java.io.FileNotFoundException) {
                                android.util.Log.d(TAG, "Image not found (404): " + imagePath + " - showing placeholder");
                            } else {
                                android.util.Log.w(TAG, "Failed to load circular image from URL: " + imagePath, e);
                            }
                            // Return false to allow Glide to show the error placeholder
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                                                     Object model, 
                                                     com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                     com.bumptech.glide.load.DataSource dataSource, 
                                                     boolean isFirstResource) {
                            android.util.Log.d(TAG, "Successfully loaded circular image from URL: " + imagePath);
                            return false;
                        }
                    })
                    .into(imageView);
                
                android.util.Log.d(TAG, "Loading circular image from URL: " + imagePath);
            } else if (isSampleImageName(imagePath)) {
                // Load sample image from drawable resource with circular crop
                int drawableResId = getSampleImageResourceId(imagePath);
                Glide.with(context)
                    .load(drawableResId)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .apply(RequestOptions.circleCropTransform())
                    .into(imageView);
                android.util.Log.d(TAG, "Loading circular sample image: " + imagePath + " -> resource ID: " + drawableResId);
            } else {
                // Load from local file path
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
     * @param imagePath Path to the image file or URL
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
            // Check if it's a URL, sample image name, or local file path
            if (isUrl(imagePath)) {
                // Load image from URL using Glide
                Glide.with(context)
                    .load(imagePath)
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
                                errorCallback.onError("Failed to load image from URL: " + (e != null ? e.getMessage() : "Unknown error"));
                            }
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                                                     Object model, 
                                                     com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                     com.bumptech.glide.load.DataSource dataSource, 
                                                     boolean isFirstResource) {
                            android.util.Log.d(TAG, "Successfully loaded image from URL: " + imagePath);
                            return false;
                        }
                    })
                    .into(imageView);
                
                android.util.Log.d(TAG, "Loading image from URL: " + imagePath);
            } else if (isSampleImageName(imagePath)) {
                // Load sample image from drawable resource
                int drawableResId = getSampleImageResourceId(imagePath);
                Glide.with(context)
                    .load(drawableResId)
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
                                errorCallback.onError("Failed to load sample image: " + (e != null ? e.getMessage() : "Unknown error"));
                            }
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                                                     Object model, 
                                                     com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                     com.bumptech.glide.load.DataSource dataSource, 
                                                     boolean isFirstResource) {
                            android.util.Log.d(TAG, "Successfully loaded sample image: " + imagePath);
                            return false;
                        }
                    })
                    .into(imageView);
                android.util.Log.d(TAG, "Loading sample image: " + imagePath + " -> resource ID: " + drawableResId);
            } else {
                // Load from local file path
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

