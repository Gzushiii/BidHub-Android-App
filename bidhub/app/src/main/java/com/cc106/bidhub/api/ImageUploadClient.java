package com.cc106.bidhub.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.cc106.bidhub.utils.SharedPreferencesHelper;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Client for uploading images to the backend server
 * Handles image compression and multipart/form-data upload
 */
public class ImageUploadClient {
    private static final String TAG = "ImageUploadClient";
    private static final String BASE_URL = "https://bidhub-android-app.onrender.com/api";
    private static final String UPLOAD_ENDPOINT = BASE_URL + "/upload";
    
    private Context context;
    private SharedPreferencesHelper prefsHelper;
    
    public ImageUploadClient(Context context) {
        this.context = context;
        this.prefsHelper = new SharedPreferencesHelper(context);
    }
    
    /**
     * Upload a single image file
     * @param imagePath Local file path to the image
     * @return UploadResponse with success status and URL, or null on error
     */
    public UploadResponse uploadImage(String imagePath) {
        Log.i(TAG, "Uploading image: " + imagePath);
        
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: " + imagePath);
                return new UploadResponse(false, "Image file does not exist", null);
            }
            
            // Compress image if needed (optional but recommended)
            File compressedFile = compressImageIfNeeded(imageFile);
            File fileToUpload = compressedFile != null ? compressedFile : imageFile;
            
            // Get auth token
            String authToken = prefsHelper.getAuthToken();
            if (authToken == null || authToken.isEmpty()) {
                return new UploadResponse(false, "Authentication token not found", null);
            }
            
            // Create multipart/form-data request
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            URL url = new URL(UPLOAD_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            
            // Write multipart form data
            OutputStream outputStream = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
            
            // Determine MIME type from file extension
            String mimeType = getMimeType(fileToUpload.getName());
            
            // Add file field
            writer.append("--" + boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"" + fileToUpload.getName() + "\"").append("\r\n");
            writer.append("Content-Type: " + mimeType).append("\r\n");
            writer.append("\r\n");
            writer.flush();
            
            // Write file data
            FileInputStream fileInputStream = new FileInputStream(fileToUpload);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            fileInputStream.close();
            
            // End multipart
            writer.append("\r\n");
            writer.append("--" + boundary + "--").append("\r\n");
            writer.flush();
            writer.close();
            
            // Get response
            int responseCode = connection.getResponseCode();
            BufferedReader reader;
            
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.getBoolean("success")) {
                    String imageUrl = jsonResponse.getString("url");
                    Log.i(TAG, "Image uploaded successfully: " + imageUrl);
                    return new UploadResponse(true, "Image uploaded successfully", imageUrl);
                } else {
                    String error = jsonResponse.optString("error", "Upload failed");
                    Log.e(TAG, "Upload failed: " + error);
                    return new UploadResponse(false, error, null);
                }
            } else {
                // Parse error response
                String errorMessage = "Upload failed: " + responseCode;
                try {
                    JSONObject errorJson = new JSONObject(response.toString());
                    errorMessage = errorJson.optString("error", errorMessage);
                } catch (Exception e) {
                    // Use default error message
                }
                Log.e(TAG, "Upload error: " + errorMessage);
                return new UploadResponse(false, errorMessage, null);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading image", e);
            return new UploadResponse(false, "Upload error: " + e.getMessage(), null);
        }
    }
    
    /**
     * Compress image if it's too large
     * @param imageFile Original image file
     * @return Compressed file, or null if compression not needed
     */
    private File compressImageIfNeeded(File imageFile) {
        try {
            long fileSizeKB = imageFile.length() / 1024;
            if (fileSizeKB <= 500) {
                // File is already small enough
                return null;
            }
            
            Log.d(TAG, "Compressing image: " + fileSizeKB + "KB");
            
            // Decode with sampling
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            
            // Calculate sample size
            int sampleSize = calculateInSampleSize(options, 1920, 1080);
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            
            // Decode and compress
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            if (bitmap == null) {
                return null;
            }
            
            // Save compressed image
            File compressedFile = new File(context.getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(compressedFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
            outputStream.close();
            bitmap.recycle();
            
            Log.d(TAG, "Image compressed: " + (compressedFile.length() / 1024) + "KB");
            return compressedFile;
            
        } catch (Exception e) {
            Log.e(TAG, "Error compressing image", e);
            return null;
        }
    }
    
    /**
     * Calculate sample size for bitmap decoding
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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
    private String getMimeType(String filename) {
        String extension = "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            extension = filename.substring(lastDot + 1).toLowerCase();
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
     * Response class for image upload
     */
    public static class UploadResponse {
        private boolean success;
        private String message;
        private String url;
        
        public UploadResponse(boolean success, String message, String url) {
            this.success = success;
            this.message = message;
            this.url = url;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getUrl() { return url; }
    }
}

