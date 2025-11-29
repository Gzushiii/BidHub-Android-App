package com.cc106.bidhub.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Utility class to convert local image file paths to base64 data URLs
 * This allows the backend API to accept images without requiring a separate upload endpoint
 */
public class ImageUrlConverter {
    private static final String TAG = "ImageUrlConverter";
    private static final int MAX_IMAGE_SIZE_KB = 500; // Max 500KB per image
    private static final int COMPRESSION_QUALITY = 85; // JPEG compression quality
    
    /**
     * Convert local image file path to base64 data URL
     * @param context Android context
     * @param imagePath Local file path to the image
     * @return Base64 data URL (e.g., "data:image/jpeg;base64,/9j/4AAQ...") or null if conversion fails
     */
    public static String convertToDataUrl(Context context, String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            Log.w(TAG, "Image path is null or empty");
            return null;
        }
        
        // Check if it's already a URL (starts with http:// or https:// or data:)
        if (imagePath.startsWith("http://") || 
            imagePath.startsWith("https://") || 
            imagePath.startsWith("data:")) {
            Log.d(TAG, "Image path is already a URL: " + imagePath);
            return imagePath;
        }
        
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: " + imagePath);
                return null;
            }
            
            // Check file size
            long fileSizeKB = imageFile.length() / 1024;
            if (fileSizeKB > MAX_IMAGE_SIZE_KB) {
                Log.w(TAG, "Image file too large: " + fileSizeKB + "KB, compressing...");
                return convertToDataUrlWithCompression(context, imagePath);
            }
            
            // Read file and convert to base64
            FileInputStream fileInputStream = new FileInputStream(imageFile);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            
            fileInputStream.close();
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            
            // Determine MIME type from file extension
            String mimeType = getMimeType(imagePath);
            
            // Convert to base64
            String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            
            // Create data URL
            String dataUrl = "data:" + mimeType + ";base64," + base64String;
            
            Log.d(TAG, "Successfully converted image to data URL. Size: " + (base64String.length() / 1024) + "KB");
            return dataUrl;
            
        } catch (IOException e) {
            Log.e(TAG, "Error converting image to data URL: " + imagePath, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error converting image: " + imagePath, e);
            return null;
        }
    }
    
    /**
     * Convert image with compression if file is too large
     */
    private static String convertToDataUrlWithCompression(Context context, String imagePath) {
        try {
            // Load bitmap with sampling
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);
            
            // Calculate sample size to reduce memory usage
            int sampleSize = calculateInSampleSize(options, 1920, 1080);
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from: " + imagePath);
                return null;
            }
            
            // Compress bitmap
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream);
            bitmap.recycle();
            
            byte[] imageBytes = outputStream.toByteArray();
            String mimeType = getMimeType(imagePath);
            String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            
            String dataUrl = "data:" + mimeType + ";base64," + base64String;
            Log.d(TAG, "Successfully converted and compressed image. Final size: " + (base64String.length() / 1024) + "KB");
            return dataUrl;
            
        } catch (Exception e) {
            Log.e(TAG, "Error compressing image: " + imagePath, e);
            return null;
        }
    }
    
    /**
     * Calculate sample size for bitmap decoding
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
    
    /**
     * Get MIME type from file extension
     */
    private static String getMimeType(String filePath) {
        String extension = "";
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filePath.length() - 1) {
            extension = filePath.substring(lastDot + 1).toLowerCase();
        }
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            default:
                return "image/jpeg"; // Default to JPEG
        }
    }
    
    /**
     * Convert multiple image paths to data URLs
     * @param context Android context
     * @param imagePaths List of local file paths
     * @return List of data URLs (null entries are filtered out)
     */
    public static java.util.List<String> convertToDataUrls(Context context, java.util.List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        java.util.List<String> dataUrls = new java.util.ArrayList<>();
        for (String imagePath : imagePaths) {
            String dataUrl = convertToDataUrl(context, imagePath);
            if (dataUrl != null) {
                dataUrls.add(dataUrl);
            } else {
                Log.w(TAG, "Failed to convert image: " + imagePath);
            }
        }
        
        return dataUrls;
    }
}

