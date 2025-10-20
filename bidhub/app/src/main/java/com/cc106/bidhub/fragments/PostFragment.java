package com.cc106.bidhub.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputLayout;
import com.cc106.bidhub.utils.SharedPreferencesHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cc106.bidhub.R;
import com.cc106.bidhub.adapters.ItemImageAdapter;
import com.cc106.bidhub.adapters.TagsAdapter;
import com.cc106.bidhub.items.Category;
import com.cc106.bidhub.items.CategoryManager;
import com.cc106.bidhub.items.ItemData;
import com.cc106.bidhub.items.ItemManager;
import com.cc106.bidhub.items.ItemStatus;
import com.cc106.bidhub.toast.ToastHelper;
import com.cc106.bidhub.MainActivity;
import com.cc106.bidhub.utils.ErrorHandler;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PostFragment extends Fragment implements 
        ItemImageAdapter.OnImageClickListener, 
        ItemImageAdapter.OnImageRemoveListener,
        ItemImageAdapter.OnAddPhotoClickListener,
        TagsAdapter.OnTagRemoveListener {

    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int MAX_IMAGES = 10;

    private String loggedInUserEmail;
    private ItemManager itemManager;
    private CategoryManager categoryManager;
    
    // UI Components
    private TextInputEditText etItemTitle;
    private TextInputEditText etItemDescription;
    private AutoCompleteTextView actvCategory;
    private AutoCompleteTextView actvSubcategory;
    private AutoCompleteTextView actvCondition;
    private TextInputEditText etStartingPrice;
    private TextInputEditText etBuyNowPrice;
    private AutoCompleteTextView actvAuctionDuration;
    private TextInputEditText etDonationReason;
    private TextInputLayout layoutDonationReason;
    // Tags functionality removed from new layout
    // private TextInputEditText etTags;
    private AutoCompleteTextView actvSize;
    private AutoCompleteTextView actvOrigin;
    
    private RecyclerView rvItemImages;
    private RecyclerView rvTags;
    private Button btnForSale;
    private Button btnForFree;
    private Button btnToggleOptional;
    private Button btnSaveDraft;
    private Button btnPostItem;
    private ProgressBar progressBar;
    private LinearLayout layoutImageProgress;
    private ProgressBar progressImageUpload;
    private TextView tvImageProgress;
    
    // Checkboxes
    private CheckBox cbQuantity;
    private CheckBox cbContact;
    
    // Layouts
    private LinearLayout layoutOptionalDetails;
    private LinearLayout layoutSubcategory;
    private LinearLayout layoutSize;
    
    // Adapters
    private ItemImageAdapter imageAdapter;
    private TagsAdapter tagsAdapter;
    
    // Data
    private List<String> selectedImages;
    private List<String> selectedTags;
    private List<Category> categories;
    private List<Category> subcategories;
    private String selectedMainCategoryId;
    private boolean isForSale = true;
    private boolean isOptionalDetailsVisible = true;
    
    // Auto-save functionality
    private Timer autoSaveTimer;
    private static final long AUTO_SAVE_INTERVAL = 30000; // 30 seconds
    private boolean hasUnsavedChanges = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post, container, false);
        
        // Get the logged-in user's email from arguments
        if (getArguments() != null) {
            loggedInUserEmail = getArguments().getString("USER_EMAIL");
        }
        
        // Initialize managers
        try {
            itemManager = ItemManager.getInstance(requireContext());
            categoryManager = CategoryManager.getInstance();
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error initializing managers: " + e.getMessage());
            return view;
        }
        
        // Initialize data
        selectedImages = new ArrayList<>();
        selectedTags = new ArrayList<>();
        subcategories = new ArrayList<>();
        selectedMainCategoryId = null;
        try {
            categories = categoryManager.getAllMainCategories();
            if (categories == null) {
                categories = new ArrayList<>();
            }
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error loading categories: " + e.getMessage());
            categories = new ArrayList<>();
        }
        
        // Initialize UI
        try {
            initializeViews(view);
            setupAdapters();
            setupDropdowns();
            setupClickListeners(view);
            setupAutoSave();
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error initializing UI: " + e.getMessage());
            e.printStackTrace();
        }
        
        return view;
    }
    
    private void initializeViews(View view) {
        etItemTitle = view.findViewById(R.id.et_item_title);
        etItemDescription = view.findViewById(R.id.et_item_description);
        actvCategory = view.findViewById(R.id.actv_category);
        actvSubcategory = view.findViewById(R.id.actv_subcategory);
        actvCondition = view.findViewById(R.id.actv_condition);
        etStartingPrice = view.findViewById(R.id.et_starting_price);
        etBuyNowPrice = view.findViewById(R.id.et_buy_now_price);
        actvAuctionDuration = view.findViewById(R.id.actv_auction_duration);
        etDonationReason = view.findViewById(R.id.et_donation_reason);
        layoutDonationReason = view.findViewById(R.id.layout_donation_reason);
        actvSize = view.findViewById(R.id.actv_size);
        actvOrigin = view.findViewById(R.id.actv_origin);
        
        rvItemImages = view.findViewById(R.id.rv_item_images);
        btnForSale = view.findViewById(R.id.btn_for_sale);
        btnForFree = view.findViewById(R.id.btn_for_free);
        btnToggleOptional = view.findViewById(R.id.btn_toggle_optional);
        btnSaveDraft = view.findViewById(R.id.btn_save_draft);
        btnPostItem = view.findViewById(R.id.btn_post_item);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutImageProgress = view.findViewById(R.id.layout_image_progress);
        progressImageUpload = view.findViewById(R.id.progress_image_upload);
        tvImageProgress = view.findViewById(R.id.tv_image_progress);
        
        // Back button
        ImageView btnBack = view.findViewById(R.id.btn_back);
        
        // Checkboxes
        cbQuantity = view.findViewById(R.id.cb_quantity);
        cbContact = view.findViewById(R.id.cb_contact);
        
        // Layouts
        layoutOptionalDetails = view.findViewById(R.id.layout_optional_details);
        layoutSubcategory = view.findViewById(R.id.layout_subcategory);
        layoutSize = view.findViewById(R.id.layout_size);
    }
    
    private void setupAdapters() {
        try {
            // Image adapter
            if (rvItemImages != null) {
                imageAdapter = new ItemImageAdapter(selectedImages);
                imageAdapter.setOnImageClickListener(this);
                imageAdapter.setOnImageRemoveListener(this);
                imageAdapter.setOnAddPhotoClickListener(this);
                rvItemImages.setLayoutManager(new GridLayoutManager(getContext(), 4));
                rvItemImages.setAdapter(imageAdapter);
            }
            
            // Tags adapter - removed from new layout
            tagsAdapter = new TagsAdapter(selectedTags);
            tagsAdapter.setOnTagRemoveListener(this);
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error setting up adapters: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupDropdowns() {
        try {
            // Main Category dropdown - only show main categories
            if (actvCategory != null) {
                try {
                    List<String> mainCategoryNames = categoryManager.getMainCategoryNames();
                    if (mainCategoryNames == null) {
                        mainCategoryNames = new ArrayList<>();
                    }
                    mainCategoryNames.add(0, "Choose"); // Add "Choose" at the beginning
                    
                    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), 
                            android.R.layout.simple_dropdown_item_1line, mainCategoryNames);
                    actvCategory.setAdapter(categoryAdapter);
                    actvCategory.setThreshold(0); // Show dropdown immediately
                    setupMainCategoryDropdownListener(actvCategory);
                } catch (Exception e) {
                    ToastHelper.showError(getContext(), "Error setting up category dropdown: " + e.getMessage());
                    // Set a basic adapter as fallback
                    String[] fallbackCategories = {"Choose", "Fashion", "Electronics", "Home & Living"};
                    ArrayAdapter<String> fallbackAdapter = new ArrayAdapter<>(getContext(), 
                            android.R.layout.simple_dropdown_item_1line, fallbackCategories);
                    actvCategory.setAdapter(fallbackAdapter);
                }
            }
            
            // Subcategory dropdown - initially hidden
            if (actvSubcategory != null) {
                setupSubcategoryDropdownListener(actvSubcategory);
            }
            
            // Condition dropdown
            if (actvCondition != null) {
                String[] conditions = {"New", "Like New", "Good", "Fair", "Poor"};
                ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(getContext(), 
                        android.R.layout.simple_dropdown_item_1line, conditions);
                actvCondition.setAdapter(conditionAdapter);
                actvCondition.setThreshold(0); // Show dropdown immediately
                setupDropdownClickListener(actvCondition);
            }
            
            // Size dropdown
            if (actvSize != null) {
                String[] sizes = {"XS", "S", "M", "L", "XL", "XXL", "XXXL", "One Size", "Custom"};
                ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(getContext(), 
                        android.R.layout.simple_dropdown_item_1line, sizes);
                actvSize.setAdapter(sizeAdapter);
                actvSize.setThreshold(0); // Show dropdown immediately
                setupDropdownClickListener(actvSize);
            }
            
            
            // Origin dropdown
            if (actvOrigin != null) {
                String[] origins = {"Local", "Imported", "Overseas", "Online Purchase", "Gift", "Unknown"};
                ArrayAdapter<String> originAdapter = new ArrayAdapter<>(getContext(), 
                        android.R.layout.simple_dropdown_item_1line, origins);
                actvOrigin.setAdapter(originAdapter);
                actvOrigin.setThreshold(0); // Show dropdown immediately
                setupDropdownClickListener(actvOrigin);
            }
            
            // Auction duration dropdown
            if (actvAuctionDuration != null) {
                String[] durations = {"1 Day", "3 Days", "5 Days", "7 Days", "10 Days", "14 Days"};
                ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(getContext(), 
                        android.R.layout.simple_dropdown_item_1line, durations);
                actvAuctionDuration.setAdapter(durationAdapter);
                actvAuctionDuration.setThreshold(0); // Show dropdown immediately
                setupDropdownClickListener(actvAuctionDuration);
            }
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error setting up dropdowns: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupDropdownClickListener(AutoCompleteTextView autoCompleteTextView) {
        try {
            // Prevent text editing
            autoCompleteTextView.setKeyListener(null);
            
            // Show dropdown on click
            autoCompleteTextView.setOnClickListener(v -> {
                autoCompleteTextView.showDropDown();
            });
            
            // Show dropdown on focus
            autoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    autoCompleteTextView.showDropDown();
                }
            });
            
            // Handle item selection
            autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedItem = (String) parent.getItemAtPosition(position);
                autoCompleteTextView.setText(selectedItem, false);
                autoCompleteTextView.dismissDropDown();
            });
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error setting up dropdown listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupMainCategoryDropdownListener(AutoCompleteTextView autoCompleteTextView) {
        try {
            // Prevent text editing
            autoCompleteTextView.setKeyListener(null);
            
            // Show dropdown on click - ensure it works even after image uploads
            autoCompleteTextView.setOnClickListener(v -> {
                // Force refresh the dropdown to ensure it's visible
                autoCompleteTextView.post(() -> {
                    autoCompleteTextView.showDropDown();
                });
            });
            
            // Show dropdown on focus
            autoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // Use post to ensure UI is ready
                    autoCompleteTextView.post(() -> {
                        autoCompleteTextView.showDropDown();
                    });
                }
            });
            
            // Handle item selection
            autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedItem = (String) parent.getItemAtPosition(position);
                autoCompleteTextView.setText(selectedItem, false);
                autoCompleteTextView.dismissDropDown();
                
                // Handle main category selection
                handleMainCategorySelection(selectedItem);
            });
            
            // Additional touch listener to ensure dropdown works
            autoCompleteTextView.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    autoCompleteTextView.post(() -> {
                        autoCompleteTextView.showDropDown();
                    });
                }
                return false; // Allow other touch events to continue
            });
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error setting up main category dropdown listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupSubcategoryDropdownListener(AutoCompleteTextView autoCompleteTextView) {
        try {
            // Prevent text editing
            autoCompleteTextView.setKeyListener(null);
            
            // Show dropdown on click
            autoCompleteTextView.setOnClickListener(v -> {
                if (layoutSubcategory.getVisibility() == View.VISIBLE) {
                    autoCompleteTextView.showDropDown();
                }
            });
            
            // Show dropdown on focus
            autoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && layoutSubcategory.getVisibility() == View.VISIBLE) {
                    autoCompleteTextView.showDropDown();
                }
            });
            
            // Handle item selection
            autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedItem = (String) parent.getItemAtPosition(position);
                autoCompleteTextView.setText(selectedItem, false);
                autoCompleteTextView.dismissDropDown();
            });
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error setting up subcategory dropdown listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleMainCategorySelection(String selectedCategoryName) {
        try {
            if (selectedCategoryName.equals("Choose")) {
                // Hide subcategory dropdown and size dropdown, reset
                hideSubcategoryDropdown();
                hideSizeDropdown();
                selectedMainCategoryId = null;
                return;
            }
            
            // Find the selected main category
            Category selectedCategory = null;
            for (Category category : categories) {
                if (category.getName().equals(selectedCategoryName)) {
                    selectedCategory = category;
                    break;
                }
            }
            
            if (selectedCategory != null) {
                selectedMainCategoryId = selectedCategory.getCategoryId();
                
                // Show/hide size dropdown based on Fashion category
                if (selectedCategoryName.equals("Fashion")) {
                    showSizeDropdown();
                } else {
                    hideSizeDropdown();
                }
                
                // Handle "Others" category - no subcategories
                if (selectedCategoryName.equals("Others")) {
                    hideSubcategoryDropdown();
                } else {
                    // Get subcategories for the selected main category
                    subcategories = categoryManager.getSubCategories(selectedMainCategoryId);
                    
                    if (subcategories != null && !subcategories.isEmpty()) {
                        // Show subcategory dropdown with subcategories
                        showSubcategoryDropdown(subcategories);
                    } else {
                        // No subcategories available, hide the dropdown
                        hideSubcategoryDropdown();
                    }
                }
            }
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error handling main category selection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showSubcategoryDropdown(List<Category> subcategories) {
        try {
            if (layoutSubcategory != null && actvSubcategory != null) {
                // Create subcategory names list
                List<String> subcategoryNames = new ArrayList<>();
                subcategoryNames.add("Choose Subcategory");
                
                for (Category subcategory : subcategories) {
                    subcategoryNames.add(subcategory.getName());
                }
                
                // Set up subcategory adapter
                ArrayAdapter<String> subcategoryAdapter = new ArrayAdapter<>(getContext(), 
                        android.R.layout.simple_dropdown_item_1line, subcategoryNames);
                actvSubcategory.setAdapter(subcategoryAdapter);
                actvSubcategory.setThreshold(0);
                
                // Show the subcategory layout
                layoutSubcategory.setVisibility(View.VISIBLE);
                actvSubcategory.setText("Choose Subcategory", false);
            }
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error showing subcategory dropdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void hideSubcategoryDropdown() {
        try {
            if (layoutSubcategory != null && actvSubcategory != null) {
                layoutSubcategory.setVisibility(View.GONE);
                actvSubcategory.setText("", false);
                actvSubcategory.setAdapter(null);
            }
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error hiding subcategory dropdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showSizeDropdown() {
        try {
            if (layoutSize != null) {
                layoutSize.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error showing size dropdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void hideSizeDropdown() {
        try {
            if (layoutSize != null && actvSize != null) {
                layoutSize.setVisibility(View.GONE);
                actvSize.setText("Choose", false);
            }
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error hiding size dropdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupClickListeners(View view) {
        try {
            if (view == null) {
                if (getContext() != null) {
                    ToastHelper.showError(getContext(), "Error: View is null in setupClickListeners");
                }
                return;
            }
            
            // Back button
            ImageView btnBack = view.findViewById(R.id.btn_back);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                });
            }
            
            if (btnForSale != null) {
                btnForSale.setOnClickListener(v -> togglePriceMode(true));
            }
            if (btnForFree != null) {
                btnForFree.setOnClickListener(v -> togglePriceMode(false));
            }
            if (btnToggleOptional != null) {
                btnToggleOptional.setOnClickListener(v -> toggleOptionalDetails());
            }
            if (btnSaveDraft != null) {
                btnSaveDraft.setOnClickListener(v -> saveDraftItem());
            }
            if (btnPostItem != null) {
                btnPostItem.setOnClickListener(v -> postItem());
            }
            
            // Tags functionality removed from new layout
            // etTags.setOnEditorActionListener((v, actionId, event) -> {
            //     addTagsFromInput();
            //     return true;
            // });
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error setting up click listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void togglePriceMode(boolean forSale) {
        isForSale = forSale;
        if (forSale) {
            btnForSale.setBackgroundResource(R.drawable.button_primary);
            btnForSale.setTextColor(getResources().getColor(R.color.white));
            btnForFree.setBackgroundResource(R.drawable.button_secondary);
            btnForFree.setTextColor(getResources().getColor(R.color.text_primary));
            // Hide donation reason field for sale items
            if (layoutDonationReason != null) {
                layoutDonationReason.setVisibility(View.GONE);
            }
        } else {
            btnForSale.setBackgroundResource(R.drawable.button_secondary);
            btnForSale.setTextColor(getResources().getColor(R.color.text_primary));
            btnForFree.setBackgroundResource(R.drawable.button_primary);
            btnForFree.setTextColor(getResources().getColor(R.color.white));
            // Show donation reason field for donation items
            if (layoutDonationReason != null) {
                layoutDonationReason.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void toggleOptionalDetails() {
        if (layoutOptionalDetails != null && btnToggleOptional != null) {
            isOptionalDetailsVisible = !isOptionalDetailsVisible;
            if (isOptionalDetailsVisible) {
                layoutOptionalDetails.setVisibility(View.VISIBLE);
                btnToggleOptional.setText("Hide ^");
            } else {
                layoutOptionalDetails.setVisibility(View.GONE);
                btnToggleOptional.setText("Show v");
            }
        }
    }
    
    private void openImagePicker() {
        if (selectedImages.size() >= MAX_IMAGES) {
            ToastHelper.showWarning(getContext(), "Maximum " + MAX_IMAGES + " images allowed");
            return;
        }
        
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error opening image picker: " + e.getMessage());
        }
    }
    
    // Add photo callback from adapter
    @Override
    public void onAddPhotoClick() {
        openImagePicker();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                try {
                    // Show progress indicator
                    showImageProgress(true);
                    
                    if (data.getClipData() != null) {
                        // Multiple images selected
                        int count = data.getClipData().getItemCount();
                        int processedCount = 0;
                        
                        for (int i = 0; i < count && selectedImages.size() < MAX_IMAGES; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            updateImageProgress("Processing image " + (i + 1) + " of " + count);
                            
                            String compressedImagePath = compressImage(imageUri);
                            if (compressedImagePath != null) {
                                selectedImages.add(compressedImagePath);
                                processedCount++;
                            }
                        }
                        
                        ToastHelper.showSuccess(getContext(), "Images added: " + processedCount);
                    } else if (data.getData() != null) {
                        // Single image selected
                        Uri imageUri = data.getData();
                        updateImageProgress("Processing image...");
                        
                        String compressedImagePath = compressImage(imageUri);
                        if (compressedImagePath != null) {
                            selectedImages.add(compressedImagePath);
                            ToastHelper.showSuccess(getContext(), "Image added successfully");
                        }
                    }
                    
                    imageAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    ToastHelper.showError(getContext(), "Error processing images: " + e.getMessage());
                } finally {
                    // Hide progress indicator
                    showImageProgress(false);
                }
            }
        }
    }
    
    private void showImageProgress(boolean show) {
        if (layoutImageProgress != null) {
            layoutImageProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    private void updateImageProgress(String message) {
        if (tvImageProgress != null) {
            tvImageProgress.setText(message);
        }
    }
    
    private String compressImage(Uri imageUri) {
        try {
            // Get input stream from URI
            android.content.ContentResolver contentResolver = getContext().getContentResolver();
            java.io.InputStream inputStream = contentResolver.openInputStream(imageUri);
            
            // Decode image with options to reduce memory usage
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            // Calculate sample size for compression
            int maxWidth = 1920;
            int maxHeight = 1080;
            int sampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
            
            // Decode image with calculated sample size
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Reduce memory usage
            
            inputStream = contentResolver.openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            if (bitmap == null) {
                return null;
            }
            
            // Rotate image if needed
            bitmap = rotateImageIfRequired(bitmap, imageUri);
            
            // Compress and save
            String fileName = "compressed_" + System.currentTimeMillis() + ".jpg";
            java.io.File outputDir = new java.io.File(getContext().getCacheDir(), "compressed_images");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            java.io.File outputFile = new java.io.File(outputDir, fileName);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(outputFile);
            
            // Compress with quality 85%
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
            outputStream.close();
            bitmap.recycle();
            
            return outputFile.getAbsolutePath();
            
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error compressing image: " + e.getMessage());
            return null;
        }
    }
    
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
    
    private Bitmap rotateImageIfRequired(Bitmap bitmap, Uri imageUri) {
        try {
            android.content.ContentResolver contentResolver = getContext().getContentResolver();
            java.io.InputStream inputStream = contentResolver.openInputStream(imageUri);
            ExifInterface exifInterface = new ExifInterface(inputStream);
            inputStream.close();
            
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }
            
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            return bitmap;
        }
    }
    
    private void addTagsFromInput() {
        // Tags functionality removed from new layout
        // String tagsInput = etTags.getText().toString().trim();
        // if (!TextUtils.isEmpty(tagsInput)) {
        //     String[] tags = tagsInput.split(",");
        //     for (String tag : tags) {
        //         String trimmedTag = tag.trim();
        //         if (!TextUtils.isEmpty(trimmedTag) && !selectedTags.contains(trimmedTag)) {
        //             selectedTags.add(trimmedTag);
        //         }
        //     }
        //     etTags.setText("");
        //     tagsAdapter.notifyDataSetChanged();
        // }
    }
    
    private void saveDraftItem() {
        try {
            // Get user email from SharedPreferences if not set
            if (TextUtils.isEmpty(loggedInUserEmail)) {
                SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
                loggedInUserEmail = prefsHelper.getUserEmail();
                if (TextUtils.isEmpty(loggedInUserEmail)) {
                    ErrorHandler.showDetailedError(getContext(), "User not logged in. Please log in again.");
                    return;
                }
            }
            
            ItemData itemData = createItemData();
            if (itemData != null) {
                // Show loading state
                btnSaveDraft.setEnabled(false);
                btnSaveDraft.setText("Saving...");
                
                boolean success = itemManager.saveDraftItem(itemData, loggedInUserEmail);
                if (success) {
                    ErrorHandler.showSuccess(getContext(), "Item saved as draft");
                    clearForm();
                } else {
                    ErrorHandler.showDetailedError(getContext(), "Failed to save draft");
                }
            } else {
                ErrorHandler.showDetailedError(getContext(), "Please fill in all required fields correctly.");
            }
        } catch (Exception e) {
            ErrorHandler.handleInitError(getContext(), "SaveDraft", e, "Saving draft item");
            e.printStackTrace();
        } finally {
            btnSaveDraft.setEnabled(true);
            btnSaveDraft.setText("Save as Draft");
        }
    }
    
    private void postItem() {
        try {
            // Get user email from SharedPreferences if not set
            if (TextUtils.isEmpty(loggedInUserEmail)) {
                SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
                loggedInUserEmail = prefsHelper.getUserEmail();
                if (TextUtils.isEmpty(loggedInUserEmail)) {
                    ErrorHandler.showDetailedError(getContext(), "User not logged in. Please log in again.");
                    return;
                }
            }
            
            // Debug: Log form data before validation
            android.util.Log.d("PostFragment", "Form data before validation:");
            android.util.Log.d("PostFragment", "Title: " + etItemTitle.getText().toString().trim());
            android.util.Log.d("PostFragment", "Category: " + actvCategory.getText().toString().trim());
            android.util.Log.d("PostFragment", "Origin: " + actvOrigin.getText().toString().trim());
            android.util.Log.d("PostFragment", "Images count: " + selectedImages.size());
            
            ItemData itemData = createItemData();
            if (itemData != null) {
                // Debug: Log item data before posting
                android.util.Log.d("PostFragment", "ItemData created successfully:");
                android.util.Log.d("PostFragment", "Title: " + itemData.getTitle());
                android.util.Log.d("PostFragment", "Description: " + itemData.getDescription());
                android.util.Log.d("PostFragment", "CategoryId: " + itemData.getCategoryId());
                android.util.Log.d("PostFragment", "StartingPrice: " + itemData.getStartingPrice());
                android.util.Log.d("PostFragment", "Condition: " + itemData.getCondition());
                android.util.Log.d("PostFragment", "Images count: " + (itemData.getImagePaths() != null ? itemData.getImagePaths().size() : 0));
                
                // Show loading state
                btnPostItem.setEnabled(false);
                btnPostItem.setText("Posting...");
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                
                boolean success = itemManager.createItem(itemData, loggedInUserEmail);
                if (success) {
                    ErrorHandler.showSuccess(getContext(), "Item posted successfully!");
                    clearForm();
                    
                    // Debug: Log item count after creation
                    android.util.Log.d("PostFragment", "Item created successfully. Total items in manager: " + itemManager.getAllBrowsableItems().size());
                    
                    // User stays on Post tab to create more listings
                } else {
                    android.util.Log.e("PostFragment", "ItemManager.createItem returned false");
                    ErrorHandler.showDetailedError(getContext(), "Failed to post item. Please check all required fields and try again.");
                }
            } else {
                android.util.Log.e("PostFragment", "createItemData returned null - validation failed");
                ErrorHandler.showDetailedError(getContext(), "Please fill in all required fields correctly.");
            }
        } catch (Exception e) {
            ErrorHandler.handleInitError(getContext(), "PostItem", e, "Posting item");
            e.printStackTrace();
        } finally {
            // Reset button state
            btnPostItem.setEnabled(true);
            btnPostItem.setText("Post Item");
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }
    
    private ItemData createItemData() {
        // Validate required fields
        if (!validateForm()) {
            return null;
        }
        
        ItemData itemData = new ItemData();
        
        // Basic information
        itemData.setTitle(etItemTitle.getText().toString().trim());
        itemData.setDescription(etItemDescription.getText().toString().trim());
        itemData.setCategoryId(getSelectedCategoryId());
        itemData.setCondition(actvCondition.getText().toString().trim());
        
        // Pricing
        if (isForSale) {
            try {
                double startingPrice = Double.parseDouble(etStartingPrice.getText().toString().trim());
                itemData.setStartingPrice(startingPrice);
                
                // Handle Buy Now price
                String buyNowPriceText = etBuyNowPrice.getText().toString().trim();
                if (!TextUtils.isEmpty(buyNowPriceText)) {
                    try {
                        double buyNowPrice = Double.parseDouble(buyNowPriceText);
                        if (buyNowPrice > startingPrice) {
                            itemData.setBuyNowPrice(buyNowPrice);
                        } else {
                            ToastHelper.showError(getContext(), "Buy Now price must be higher than starting price");
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        ToastHelper.showError(getContext(), "Invalid Buy Now price format");
                        return null;
                    }
                }
            } catch (NumberFormatException e) {
                ToastHelper.showError(getContext(), "Invalid price format");
                return null;
            }
        } else {
            itemData.setStartingPrice(0.0); // Donation item
            // Store donation reason in metadata
            String donationReason = etDonationReason.getText().toString().trim();
            if (!TextUtils.isEmpty(donationReason)) {
                String metadata = itemData.getMetadata();
                if (TextUtils.isEmpty(metadata)) {
                    metadata = "Donation Reason: " + donationReason;
                } else {
                    metadata += ", Donation Reason: " + donationReason;
                }
                itemData.setMetadata(metadata);
            }
        }
        
        // Additional fields
        String size = actvSize.getText().toString().trim();
        if (!TextUtils.isEmpty(size) && !size.equals("Choose")) {
            itemData.setMetadata("Size: " + size);
        }
        
        
        // Handle origin field (optional)
        String origin = actvOrigin.getText().toString().trim();
        if (!TextUtils.isEmpty(origin) && !origin.equals("Choose")) {
            String metadata = itemData.getMetadata();
            if (TextUtils.isEmpty(metadata)) {
                metadata = "Origin: " + origin;
            } else {
                metadata += ", Origin: " + origin;
            }
            itemData.setMetadata(metadata);
        }
        // Note: Origin field is optional, so no validation is needed if it's empty or "Choose"
        
        // Images and tags
        itemData.setImagePaths(new ArrayList<>(selectedImages));
        itemData.setTags(new ArrayList<>(selectedTags));
        
        
        // Set default auction duration (7 days)
        setAuctionDuration(itemData);
        
        return itemData;
    }
    
    private boolean validateForm() {
        // Validate title
        String title = etItemTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            ToastHelper.showError(getContext(), "Listing title is required");
            etItemTitle.requestFocus();
            return false;
        }
        if (title.length() < 3) {
            ToastHelper.showError(getContext(), "Title must be at least 3 characters long");
            etItemTitle.requestFocus();
            return false;
        }
        if (title.length() > 100) {
            ToastHelper.showError(getContext(), "Title must be less than 100 characters");
            etItemTitle.requestFocus();
            return false;
        }
        
        // Validate category
        String category = actvCategory.getText().toString().trim();
        if (TextUtils.isEmpty(category) || category.equals("Choose")) {
            ToastHelper.showError(getContext(), "Category is required");
            actvCategory.requestFocus();
            return false;
        }
        
        // Validate donation reason for donation items
        if (!isForSale) {
            String donationReason = etDonationReason.getText().toString().trim();
            if (TextUtils.isEmpty(donationReason)) {
                ToastHelper.showError(getContext(), "Donation reason is required");
                etDonationReason.requestFocus();
                return false;
            }
            if (donationReason.length() < 10) {
                ToastHelper.showError(getContext(), "Please provide a more detailed donation reason (at least 10 characters)");
                etDonationReason.requestFocus();
                return false;
            }
        }
        
        // Validate price for sale items
        if (isForSale) {
            String priceText = etStartingPrice.getText().toString().trim();
            if (TextUtils.isEmpty(priceText)) {
                ToastHelper.showError(getContext(), "Starting price is required for items for sale");
                etStartingPrice.requestFocus();
                return false;
            }
            try {
                double price = Double.parseDouble(priceText);
                if (price < 1) {
                    ToastHelper.showError(getContext(), "Starting price must be at least ₱1");
                    etStartingPrice.requestFocus();
                    return false;
                }
                if (price > 1000000) {
                    ToastHelper.showError(getContext(), "Price seems too high. Please verify the amount");
                    etStartingPrice.requestFocus();
                    return false;
                }
                
                // Validate Buy Now price if provided
                String buyNowPriceText = etBuyNowPrice.getText().toString().trim();
                if (!TextUtils.isEmpty(buyNowPriceText)) {
                    try {
                        double buyNowPrice = Double.parseDouble(buyNowPriceText);
                        if (buyNowPrice <= price) {
                            ToastHelper.showError(getContext(), "Buy Now price must be higher than starting price");
                            etBuyNowPrice.requestFocus();
                            return false;
                        }
                        if (buyNowPrice > 1000000) {
                            ToastHelper.showError(getContext(), "Buy Now price seems too high. Please verify the amount");
                            etBuyNowPrice.requestFocus();
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        ToastHelper.showError(getContext(), "Please enter a valid Buy Now price");
                        etBuyNowPrice.requestFocus();
                        return false;
                    }
                }
            } catch (NumberFormatException e) {
                ToastHelper.showError(getContext(), "Please enter a valid starting price");
                etStartingPrice.requestFocus();
                return false;
            }
        }
        
        // Validate condition
        String condition = actvCondition.getText().toString().trim();
        if (TextUtils.isEmpty(condition) || condition.equals("Choose")) {
            ToastHelper.showError(getContext(), "Condition is required");
            actvCondition.requestFocus();
            return false;
        }
        
        // Validate images
        if (selectedImages == null || selectedImages.isEmpty()) {
            ToastHelper.showError(getContext(), "At least one photo is required");
            return false;
        }
        if (selectedImages.size() > MAX_IMAGES) {
            ToastHelper.showError(getContext(), "Maximum " + MAX_IMAGES + " photos allowed");
            return false;
        }
        
        // Validate description length
        String description = etItemDescription.getText().toString().trim();
        if (!TextUtils.isEmpty(description) && description.length() > 1000) {
            ToastHelper.showError(getContext(), "Description must be less than 1000 characters");
            etItemDescription.requestFocus();
            return false;
        }
        
        // Validate auction duration
        String duration = actvAuctionDuration.getText().toString().trim();
        if (TextUtils.isEmpty(duration) || duration.equals("Choose")) {
            ToastHelper.showError(getContext(), "Auction duration is required");
            actvAuctionDuration.requestFocus();
            return false;
        }
        
        // Origin field is optional - no validation needed
        // This ensures the field is truly optional and doesn't cause validation failures
        
        return true;
    }
    
    private String getSelectedCategoryId() {
        // First check if a subcategory is selected
        String selectedSubcategoryName = actvSubcategory.getText().toString().trim();
        if (!TextUtils.isEmpty(selectedSubcategoryName) && 
            !selectedSubcategoryName.equals("Choose Subcategory") && 
            !selectedSubcategoryName.equals("Choose")) {
            
            // Find the subcategory ID
            for (Category subcategory : subcategories) {
                if (subcategory.getName().equals(selectedSubcategoryName)) {
                    return subcategory.getCategoryId();
                }
            }
        }
        
        // If no subcategory selected, use main category
        String selectedCategoryName = actvCategory.getText().toString().trim();
        if (!TextUtils.isEmpty(selectedCategoryName) && !selectedCategoryName.equals("Choose")) {
            return selectedMainCategoryId;
        }
        
        return null;
    }
    
    private void setAuctionDuration(ItemData itemData) {
        String duration = actvAuctionDuration.getText().toString().trim();
        Calendar calendar = Calendar.getInstance();
        itemData.setStartDate(calendar.getTime());
        
        int days = 7; // Default
        if (!TextUtils.isEmpty(duration)) {
            switch (duration) {
                case "1 Day":
                    days = 1;
                    break;
                case "3 Days":
                    days = 3;
                    break;
                case "5 Days":
                    days = 5;
                    break;
                case "7 Days":
                    days = 7;
                    break;
                case "10 Days":
                    days = 10;
                    break;
                case "14 Days":
                    days = 14;
                    break;
                default:
                    days = 7; // Default fallback
                    break;
            }
        }
        
        calendar.add(Calendar.DAY_OF_MONTH, days);
        itemData.setEndDate(calendar.getTime());
    }
    
    private void clearForm() {
        etItemTitle.setText("");
        etItemDescription.setText("");
        actvCategory.setText("Choose");
        actvCondition.setText("Choose");
        etStartingPrice.setText("");
        etBuyNowPrice.setText("");
        actvAuctionDuration.setText("7 Days");
        etDonationReason.setText("");
        actvSize.setText("Choose");
        actvOrigin.setText("Choose");
        
        // Reset subcategory dropdown and size dropdown
        hideSubcategoryDropdown();
        hideSizeDropdown();
        selectedMainCategoryId = null;
        subcategories.clear();
        
        // Reset checkboxes
        if (cbQuantity != null) cbQuantity.setChecked(false);
        if (cbContact != null) cbContact.setChecked(true);
        
        // Reset price mode
        togglePriceMode(true);
        
        // Reset optional details visibility
        isOptionalDetailsVisible = true;
        if (layoutOptionalDetails != null) {
            layoutOptionalDetails.setVisibility(View.VISIBLE);
        }
        if (btnToggleOptional != null) {
            btnToggleOptional.setText("Hide ^");
        }
        
        selectedImages.clear();
        selectedTags.clear();
        imageAdapter.notifyDataSetChanged();
    }
    
    // Image adapter callbacks
    @Override
    public void onImageClick(int position, String imagePath) {
        // Handle image click (e.g., show full screen)
        ToastHelper.showInfo(getContext(), "Image clicked: " + position);
    }
    
    @Override
    public void onImageRemove(int position, String imagePath) {
        selectedImages.remove(position);
        imageAdapter.notifyItemRemoved(position);
        ToastHelper.showInfo(getContext(), "Image removed");
    }
    
    // Tags adapter callbacks
    @Override
    public void onTagRemove(int position, String tag) {
        selectedTags.remove(position);
        tagsAdapter.notifyItemRemoved(position);
    }
    
    public void updateUserEmail(String email) {
        this.loggedInUserEmail = email;
    }
    
    // Auto-save functionality
    private void setupAutoSave() {
        try {
            // Set up text change listeners for auto-save
            if (etItemTitle != null) {
                etItemTitle.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        hasUnsavedChanges = true;
                        scheduleAutoSave();
                    }
                    
                    @Override
                    public void afterTextChanged(android.text.Editable s) {}
                });
            }
            
            if (etItemDescription != null) {
                etItemDescription.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        hasUnsavedChanges = true;
                        scheduleAutoSave();
                    }
                    
                    @Override
                    public void afterTextChanged(android.text.Editable s) {}
                });
            }
            
            if (etStartingPrice != null) {
                etStartingPrice.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        hasUnsavedChanges = true;
                        scheduleAutoSave();
                    }
                    
                    @Override
                    public void afterTextChanged(android.text.Editable s) {}
                });
            }
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error setting up auto-save: " + e.getMessage());
        }
    }
    
    private void scheduleAutoSave() {
        try {
            if (autoSaveTimer != null) {
                autoSaveTimer.cancel();
            }
            
            autoSaveTimer = new Timer();
            autoSaveTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (hasUnsavedChanges && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            saveDraftItem();
                            hasUnsavedChanges = false;
                        });
                    }
                }
            }, AUTO_SAVE_INTERVAL);
        } catch (Exception e) {
            ToastHelper.showError(getContext(), "Error scheduling auto-save: " + e.getMessage());
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (autoSaveTimer != null) {
                autoSaveTimer.cancel();
                autoSaveTimer = null;
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Navigate to Browse tab after successful item posting
     */
    private void navigateToBrowseTab() {
        try {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.switchToBrowseTab();
            }
        } catch (Exception e) {
            android.util.Log.e("PostFragment", "Error navigating to browse tab", e);
        }
    }
}
