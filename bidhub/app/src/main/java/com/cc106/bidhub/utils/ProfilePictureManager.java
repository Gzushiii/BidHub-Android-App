package com.cc106.bidhub.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfilePictureManager {
    
    private static final String TAG = "ProfilePictureManager";
    private static final String PROFILE_PICTURES_DIR = "profile_pictures";
    private static final int MAX_IMAGE_SIZE = 512; // Maximum width/height in pixels
    private static final int COMPRESSION_QUALITY = 85;
    
    /**
     * Save profile picture to internal storage
     */
    public static String saveProfilePicture(Context context, Bitmap bitmap, String userId) {
        try {
            if (context == null || bitmap == null || userId == null) {
                Log.w(TAG, "Context, bitmap, or userId is null");
                return null;
            }
            
            // Create profile pictures directory
            File profileDir = new File(context.getFilesDir(), PROFILE_PICTURES_DIR);
            if (!profileDir.exists()) {
                profileDir.mkdirs();
            }
            
            // Create filename
            String filename = "profile_" + userId + ".jpg";
            File imageFile = new File(profileDir, filename);
            
            // Resize and compress bitmap
            Bitmap resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);
            Bitmap circularBitmap = createCircularBitmap(resizedBitmap);
            
            // Save to file
            FileOutputStream fos = new FileOutputStream(imageFile);
            circularBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, fos);
            fos.close();
            
            Log.d(TAG, "Profile picture saved: " + imageFile.getAbsolutePath());
            return imageFile.getAbsolutePath();
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving profile picture", e);
            return null;
        }
    }
    
    /**
     * Load profile picture from internal storage
     */
    public static Bitmap loadProfilePicture(Context context, String userId) {
        try {
            if (context == null || userId == null) {
                Log.w(TAG, "Context or userId is null");
                return null;
            }
            
            File profileDir = new File(context.getFilesDir(), PROFILE_PICTURES_DIR);
            String filename = "profile_" + userId + ".jpg";
            File imageFile = new File(profileDir, filename);
            
            if (imageFile.exists()) {
                return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading profile picture", e);
        }
        return null;
    }
    
    /**
     * Delete profile picture from internal storage
     */
    public static boolean deleteProfilePicture(Context context, String userId) {
        try {
            File profileDir = new File(context.getFilesDir(), PROFILE_PICTURES_DIR);
            String filename = "profile_" + userId + ".jpg";
            File imageFile = new File(profileDir, filename);
            
            if (imageFile.exists()) {
                boolean deleted = imageFile.delete();
                Log.d(TAG, "Profile picture deleted: " + deleted);
                return deleted;
            }
            return true; // File doesn't exist, consider it deleted
        } catch (Exception e) {
            Log.e(TAG, "Error deleting profile picture", e);
            return false;
        }
    }
    
    /**
     * Resize bitmap to fit within specified dimensions while maintaining aspect ratio
     */
    private static Bitmap resizeBitmap(Bitmap original, int maxSize) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        if (width <= maxSize && height <= maxSize) {
            return original;
        }
        
        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }
    
    /**
     * Create circular bitmap from rectangular bitmap
     */
    private static Bitmap createCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        
        Rect rect = new Rect(0, 0, size, size);
        RectF rectF = new RectF(rect);
        
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        
        // Center the bitmap
        int left = (size - bitmap.getWidth()) / 2;
        int top = (size - bitmap.getHeight()) / 2;
        canvas.drawBitmap(bitmap, left, top, paint);
        
        return output;
    }
    
    /**
     * Convert Uri to Bitmap
     */
    public static Bitmap uriToBitmap(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            Log.e(TAG, "Error converting URI to bitmap", e);
            return null;
        }
    }
    
    /**
     * Get profile picture file path
     */
    public static String getProfilePicturePath(Context context, String userId) {
        File profileDir = new File(context.getFilesDir(), PROFILE_PICTURES_DIR);
        String filename = "profile_" + userId + ".jpg";
        File imageFile = new File(profileDir, filename);
        return imageFile.exists() ? imageFile.getAbsolutePath() : null;
    }
    
    /**
     * Check if profile picture exists
     */
    public static boolean hasProfilePicture(Context context, String userId) {
        File profileDir = new File(context.getFilesDir(), PROFILE_PICTURES_DIR);
        String filename = "profile_" + userId + ".jpg";
        File imageFile = new File(profileDir, filename);
        return imageFile.exists();
    }
}
