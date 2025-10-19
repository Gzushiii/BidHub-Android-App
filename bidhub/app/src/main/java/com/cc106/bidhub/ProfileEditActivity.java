package com.cc106.bidhub;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.utils.ProfilePictureManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileEditActivity extends BaseActivity {

    private EditText editTextFirstName, editTextLastName, editTextUsername, editTextEmail, editTextPhone;
    private Button buttonSave, buttonCancel;
    private ImageView imageViewProfilePicture;
    private TextView textViewProfilePictureHint;
    private DatabaseHelper dbHelper;
    private String loggedInUserEmail;
    private String originalUsername, originalEmail;
    private String userId;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int CROP_IMAGE_REQUEST = 1002;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the profile edit content into the content frame
        getLayoutInflater().inflate(R.layout.activity_profile_edit_content, findViewById(R.id.content_frame));
        
        // Get the logged-in user's email from the Intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        dbHelper = new DatabaseHelper(this);
        
        // Animate content in after inflation
        animateContentIn();
        
        // Initialize Views
        initializeViews();
        
        // Load user data and populate fields
        loadUserData();
        
        // Load profile picture
        loadProfilePicture();
        
        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);
        imageViewProfilePicture = findViewById(R.id.imageViewProfilePicture);
        textViewProfilePictureHint = findViewById(R.id.textViewProfilePictureHint);
    }

    private void loadUserData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            new String[]{
                DatabaseHelper.COLUMN_USER_ID,
                DatabaseHelper.COLUMN_USER_FIRST_NAME,
                DatabaseHelper.COLUMN_USER_LAST_NAME,
                DatabaseHelper.COLUMN_USER_USERNAME,
                DatabaseHelper.COLUMN_USER_EMAIL,
                DatabaseHelper.COLUMN_USER_PHONE
            },
            DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
            new String[]{loggedInUserEmail},
            null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            userId = String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)));
            String firstName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_FIRST_NAME));
            String lastName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_LAST_NAME));
            String username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_USERNAME));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_EMAIL));
            String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PHONE));

            // Store original values for validation
            originalUsername = username;
            originalEmail = email;

            // Populate fields
            editTextFirstName.setText(firstName);
            editTextLastName.setText(lastName);
            editTextUsername.setText(username);
            editTextEmail.setText(email);
            editTextPhone.setText(phone);

            cursor.close();
        }
        db.close();
    }

    private void setupClickListeners() {
        buttonSave.setOnClickListener(v -> saveProfile());
        buttonCancel.setOnClickListener(v -> finish());
        
        imageViewProfilePicture.setOnClickListener(v -> {
            openImagePicker();
        });
    }

    private void saveProfile() {
        // Validate input
        if (!validateInput()) {
            return;
        }

        String firstName = editTextFirstName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        // Check if username or email has changed and validate uniqueness
        if (!username.equals(originalUsername) && !isUsernameAvailable(username)) {
            editTextUsername.setError("Username already exists");
            return;
        }

        if (!email.equals(originalEmail) && !isEmailAvailable(email)) {
            editTextEmail.setError("Email already exists");
            return;
        }

        // Update database
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_FIRST_NAME, firstName);
        values.put(DatabaseHelper.COLUMN_USER_LAST_NAME, lastName);
        values.put(DatabaseHelper.COLUMN_USER_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_USER_EMAIL, email);
        values.put(DatabaseHelper.COLUMN_USER_PHONE, phone);

        int rowsAffected = db.update(
            DatabaseHelper.TABLE_USERS,
            values,
            DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
            new String[]{loggedInUserEmail}
        );

        if (rowsAffected > 0) {
            ToastHelper.showSuccess(this, "Profile updated successfully!");
            
            // Update the logged-in user email if email was changed
            if (!email.equals(originalEmail)) {
                loggedInUserEmail = email;
            }
            
            // Return to profile with updated data
            Intent resultIntent = new Intent();
            resultIntent.putExtra("UPDATED_EMAIL", loggedInUserEmail);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            ToastHelper.showError(this, "Failed to update profile");
        }

        db.close();
    }

    private boolean validateInput() {
        boolean isValid = true;

        String firstName = editTextFirstName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        // Validate first name
        if (TextUtils.isEmpty(firstName)) {
            editTextFirstName.setError("First name is required");
            isValid = false;
        } else if (firstName.length() < 2) {
            editTextFirstName.setError("First name must be at least 2 characters");
            isValid = false;
        }

        // Validate last name
        if (TextUtils.isEmpty(lastName)) {
            editTextLastName.setError("Last name is required");
            isValid = false;
        } else if (lastName.length() < 2) {
            editTextLastName.setError("Last name must be at least 2 characters");
            isValid = false;
        }

        // Validate username
        if (TextUtils.isEmpty(username)) {
            editTextUsername.setError("Username is required");
            isValid = false;
        } else if (username.length() < 3) {
            editTextUsername.setError("Username must be at least 3 characters");
            isValid = false;
        } else if (!username.matches("^[a-zA-Z0-9_]+$")) {
            editTextUsername.setError("Username can only contain letters, numbers, and underscores");
            isValid = false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email address");
            isValid = false;
        }

        // Validate phone
        if (TextUtils.isEmpty(phone)) {
            editTextPhone.setError("Phone number is required");
            isValid = false;
        } else if (!phone.matches("^[0-9+\\-\\s()]+$")) {
            editTextPhone.setError("Please enter a valid phone number");
            isValid = false;
        }

        return isValid;
    }

    private boolean isUsernameAvailable(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            new String[]{DatabaseHelper.COLUMN_USER_ID},
            DatabaseHelper.COLUMN_USER_USERNAME + " = ? AND " + DatabaseHelper.COLUMN_USER_EMAIL + " != ?",
            new String[]{username, loggedInUserEmail},
            null, null, null
        );

        boolean isAvailable = cursor.getCount() == 0;
        cursor.close();
        db.close();
        return isAvailable;
    }

    private boolean isEmailAvailable(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            new String[]{DatabaseHelper.COLUMN_USER_ID},
            DatabaseHelper.COLUMN_USER_EMAIL + " = ? AND " + DatabaseHelper.COLUMN_USER_EMAIL + " != ?",
            new String[]{email, loggedInUserEmail},
            null, null, null
        );

        boolean isAvailable = cursor.getCount() == 0;
        cursor.close();
        db.close();
        return isAvailable;
    }

    private void loadProfilePicture() {
        if (userId != null) {
            Bitmap profilePicture = ProfilePictureManager.loadProfilePicture(this, userId);
            if (profilePicture != null) {
                imageViewProfilePicture.setImageBitmap(profilePicture);
                textViewProfilePictureHint.setText("Tap to change photo");
            } else {
                imageViewProfilePicture.setImageResource(R.drawable.ic_profile);
                textViewProfilePictureHint.setText("Tap to add photo");
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    private void startImageCrop(Uri imageUri) {
        try {
            // Create a temporary file for the cropped image
            File tempFile = new File(getCacheDir(), "cropped_profile_" + System.currentTimeMillis() + ".jpg");
            Uri outputUri = Uri.fromFile(tempFile);
            
            // Use Android's built-in crop functionality
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(imageUri, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("outputX", 512);
            cropIntent.putExtra("outputY", 512);
            cropIntent.putExtra("scale", true);
            cropIntent.putExtra("return-data", false);
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
            cropIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            
            // Check if there's an app that can handle cropping
            if (cropIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cropIntent, CROP_IMAGE_REQUEST);
            } else {
                // Fallback: use a simple crop implementation
                performSimpleCrop(imageUri);
            }
        } catch (Exception e) {
            // Fallback: use a simple crop implementation
            performSimpleCrop(imageUri);
        }
    }
    
    private void performSimpleCrop(Uri imageUri) {
        try {
            // Load the image
            Bitmap originalBitmap = ProfilePictureManager.uriToBitmap(this, imageUri);
            if (originalBitmap != null) {
                // Create a square crop from the center
                Bitmap croppedBitmap = createSquareCrop(originalBitmap);
                
                // Save the cropped image
                String savedPath = ProfilePictureManager.saveProfilePicture(this, croppedBitmap, userId);
                if (savedPath != null) {
                    // Update the UI
                    imageViewProfilePicture.setImageBitmap(croppedBitmap);
                    textViewProfilePictureHint.setText("Tap to change photo");
                    ToastHelper.showSuccess(this, "Profile picture updated!");
                } else {
                    ToastHelper.showError(this, "Failed to save profile picture");
                }
            } else {
                ToastHelper.showError(this, "Failed to load selected image");
            }
        } catch (Exception e) {
            ToastHelper.showError(this, "Error processing image: " + e.getMessage());
        }
    }
    
    private Bitmap createSquareCrop(Bitmap originalBitmap) {
        int size = Math.min(originalBitmap.getWidth(), originalBitmap.getHeight());
        int x = (originalBitmap.getWidth() - size) / 2;
        int y = (originalBitmap.getHeight() - size) / 2;
        
        return Bitmap.createBitmap(originalBitmap, x, y, size, size);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                // Start cropping the selected image
                startImageCrop(selectedImageUri);
            }
        } else if (requestCode == CROP_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            // Handle cropped image
            Uri croppedImageUri = data.getData();
            if (croppedImageUri != null) {
                Bitmap bitmap = ProfilePictureManager.uriToBitmap(this, croppedImageUri);
                if (bitmap != null) {
                    // Save the profile picture
                    String savedPath = ProfilePictureManager.saveProfilePicture(this, bitmap, userId);
                    if (savedPath != null) {
                        // Update the UI
                        imageViewProfilePicture.setImageBitmap(bitmap);
                        textViewProfilePictureHint.setText("Tap to change photo");
                        ToastHelper.showSuccess(this, "Profile picture updated!");
                    } else {
                        ToastHelper.showError(this, "Failed to save profile picture");
                    }
                } else {
                    ToastHelper.showError(this, "Failed to load cropped image");
                }
            }
        }
    }

    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false; // This is not a main navigation activity
    }

    @Override
    protected void setCurrentTabSelected() {
        // No tab selection for this activity
    }

    @Override
    public String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
}
