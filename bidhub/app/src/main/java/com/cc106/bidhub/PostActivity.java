package com.cc106.bidhub;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cc106.bidhub.adapters.ItemImageAdapter;
import com.cc106.bidhub.adapters.TagsAdapter;
import com.cc106.bidhub.items.Category;
import com.cc106.bidhub.items.CategoryManager;
import com.cc106.bidhub.items.ItemData;
import com.cc106.bidhub.items.ItemManager;
import com.cc106.bidhub.items.ItemStatus;
import com.cc106.bidhub.toast.ToastHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PostActivity extends BaseActivity implements 
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
    private AutoCompleteTextView actvSize;
    private AutoCompleteTextView actvFeatures;
    private AutoCompleteTextView actvOrigin;
    
    private RecyclerView rvItemImages;
    private RecyclerView rvTags;
    private Button btnForSale;
    private Button btnForFree;
    private Button btnToggleOptional;
    private Button btnPostItem;
    
    // Checkboxes
    private CheckBox cbQuantity;
    private CheckBox cbContact;
    private CheckBox cbMeetup;
    private CheckBox cbDelivery;
    
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Set content view FIRST - this must happen before any findViewById calls
            setContentView(R.layout.activity_post_content);
            
            // Get the logged-in user's email from intent
            loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
            
            // Initialize data collections FIRST (before managers)
            selectedImages = new ArrayList<>();
            selectedTags = new ArrayList<>();
            subcategories = new ArrayList<>();
            selectedMainCategoryId = null;
            categories = new ArrayList<>(); // Initialize with empty list first
            
            // Initialize managers AFTER setContentView
            try {
                itemManager = ItemManager.getInstance(this);
                categoryManager = CategoryManager.getInstance();
            } catch (Exception e) {
                android.util.Log.e("PostActivity", "Error initializing managers: " + e.getMessage(), e);
                // Continue even if managers fail - we'll handle it gracefully
            }
            
            // Initialize categories with null check
            try {
                if (categoryManager != null) {
                    categories = categoryManager.getAllMainCategories();
                    if (categories == null) {
                        categories = new ArrayList<>();
                    }
                } else {
                    android.util.Log.w("PostActivity", "CategoryManager is null, using empty categories list");
                    categories = new ArrayList<>();
                }
            } catch (Exception e) {
                android.util.Log.e("PostActivity", "Error loading categories: " + e.getMessage(), e);
                categories = new ArrayList<>();
            }
            
            // Initialize UI with error handling
            try {
                initializeViews();
                setupAdapters();
                setupDropdowns();
                setupClickListeners();
            } catch (Exception e) {
                android.util.Log.e("PostActivity", "Error initializing UI: " + e.getMessage(), e);
                ToastHelper.showError(this, "Error initializing UI: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Critical error in onCreate: " + e.getMessage(), e);
            e.printStackTrace();
            // Show error and finish activity to prevent further crashes
            ToastHelper.showError(this, "Failed to initialize Post screen. Please try again.");
            finish();
        }
    }
    
    private void initializeViews() {
        try {
            // Initialize all views with null safety - findViewById can return null if view doesn't exist
            etItemTitle = findViewById(R.id.et_item_title);
            etItemDescription = findViewById(R.id.et_item_description);
            actvCategory = findViewById(R.id.actv_category);
            actvSubcategory = findViewById(R.id.actv_subcategory);
            actvCondition = findViewById(R.id.actv_condition);
            etStartingPrice = findViewById(R.id.et_starting_price);
            actvSize = findViewById(R.id.actv_size);
            actvFeatures = findViewById(R.id.actv_features);
            actvOrigin = findViewById(R.id.actv_origin);
            
            rvItemImages = findViewById(R.id.rv_item_images);
            // rvTags is not in the layout, so it will remain null - that's okay
            btnForSale = findViewById(R.id.btn_for_sale);
            btnForFree = findViewById(R.id.btn_for_free);
            btnToggleOptional = findViewById(R.id.btn_toggle_optional);
            btnPostItem = findViewById(R.id.btn_post_item);
            
            // Checkboxes
            cbQuantity = findViewById(R.id.cb_quantity);
            cbContact = findViewById(R.id.cb_contact);
            cbMeetup = findViewById(R.id.cb_meetup);
            cbDelivery = findViewById(R.id.cb_delivery);
            
            // Layouts
            layoutOptionalDetails = findViewById(R.id.layout_optional_details);
            layoutSubcategory = findViewById(R.id.layout_subcategory);
            layoutSize = findViewById(R.id.layout_size);
            
            // Log if critical views are missing
            if (etItemTitle == null || actvCategory == null || btnPostItem == null) {
                android.util.Log.e("PostActivity", "Critical views are null! etItemTitle=" + (etItemTitle != null) + 
                    ", actvCategory=" + (actvCategory != null) + ", btnPostItem=" + (btnPostItem != null));
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error in initializeViews: " + e.getMessage(), e);
            throw e; // Re-throw to be caught by onCreate's try-catch
        }
    }
    
    private void setupAdapters() {
        try {
            // Image adapter
            if (rvItemImages != null && selectedImages != null) {
                imageAdapter = new ItemImageAdapter(selectedImages);
                imageAdapter.setOnImageClickListener(this);
                imageAdapter.setOnImageRemoveListener(this);
                imageAdapter.setOnAddPhotoClickListener(this);
                rvItemImages.setLayoutManager(new GridLayoutManager(this, 4));
                rvItemImages.setAdapter(imageAdapter);
            }
            
            // Tags adapter - removed from new layout
            if (selectedTags != null) {
                tagsAdapter = new TagsAdapter(selectedTags);
                tagsAdapter.setOnTagRemoveListener(this);
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error setting up adapters: " + e.getMessage(), e);
            ToastHelper.showError(this, "Error setting up adapters: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupDropdowns() {
        try {
            // Main Category dropdown - only show main categories
            if (actvCategory != null && categoryManager != null) {
                try {
                    List<String> mainCategoryNames = categoryManager.getMainCategoryNames();
                    if (mainCategoryNames == null) {
                        mainCategoryNames = new ArrayList<>();
                    }
                    mainCategoryNames.add(0, "Choose"); // Add "Choose" at the beginning
                    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, 
                            android.R.layout.simple_dropdown_item_1line, mainCategoryNames);
                    actvCategory.setAdapter(categoryAdapter);
                    setupMainCategoryDropdownListener(actvCategory);
                } catch (Exception e) {
                    android.util.Log.e("PostActivity", "Error setting up category dropdown: " + e.getMessage(), e);
                    // Set a basic adapter as fallback
                    String[] fallbackCategories = {"Choose", "Fashion", "Electronics", "Home & Living"};
                    ArrayAdapter<String> fallbackAdapter = new ArrayAdapter<>(this, 
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
                ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(this, 
                        android.R.layout.simple_dropdown_item_1line, conditions);
                actvCondition.setAdapter(conditionAdapter);
                setupDropdownClickListener(actvCondition);
            }
            
            // Size dropdown
            if (actvSize != null) {
                String[] sizes = {"XS", "S", "M", "L", "XL", "XXL", "XXXL", "One Size", "Custom"};
                ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, 
                        android.R.layout.simple_dropdown_item_1line, sizes);
                actvSize.setAdapter(sizeAdapter);
                setupDropdownClickListener(actvSize);
            }
            
            // Features dropdown
            if (actvFeatures != null) {
                String[] features = {"Brand New", "Used", "Vintage", "Limited Edition", "Rare", "Collectible", "Custom"};
                ArrayAdapter<String> featuresAdapter = new ArrayAdapter<>(this, 
                        android.R.layout.simple_dropdown_item_1line, features);
                actvFeatures.setAdapter(featuresAdapter);
                setupDropdownClickListener(actvFeatures);
            }
            
            // Origin dropdown
            if (actvOrigin != null) {
                String[] origins = {"Local", "Imported", "Overseas", "Online Purchase", "Gift", "Unknown"};
                ArrayAdapter<String> originAdapter = new ArrayAdapter<>(this, 
                        android.R.layout.simple_dropdown_item_1line, origins);
                actvOrigin.setAdapter(originAdapter);
                setupDropdownClickListener(actvOrigin);
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error setting up dropdowns: " + e.getMessage(), e);
            ToastHelper.showError(this, "Error setting up dropdowns: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupClickListeners() {
        try {
            if (btnForSale != null) {
                btnForSale.setOnClickListener(v -> togglePriceMode(true));
            }
            if (btnForFree != null) {
                btnForFree.setOnClickListener(v -> togglePriceMode(false));
            }
            if (btnToggleOptional != null) {
                btnToggleOptional.setOnClickListener(v -> toggleOptionalDetails());
            }
            if (btnPostItem != null) {
                btnPostItem.setOnClickListener(v -> postItem());
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error setting up click listeners: " + e.getMessage(), e);
            ToastHelper.showError(this, "Error setting up click listeners: " + e.getMessage());
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
            ToastHelper.showError(this, "Error setting up dropdown listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupMainCategoryDropdownListener(AutoCompleteTextView autoCompleteTextView) {
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
                
                // Handle main category selection
                handleMainCategorySelection(selectedItem);
            });
        } catch (Exception e) {
            ToastHelper.showError(this, "Error setting up main category dropdown listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupSubcategoryDropdownListener(AutoCompleteTextView autoCompleteTextView) {
        try {
            // Prevent text editing
            autoCompleteTextView.setKeyListener(null);
            
            // Show dropdown on click
            autoCompleteTextView.setOnClickListener(v -> {
                if (layoutSubcategory != null && layoutSubcategory.getVisibility() == View.VISIBLE) {
                    autoCompleteTextView.showDropDown();
                }
            });
            
            // Show dropdown on focus
            autoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && layoutSubcategory != null && layoutSubcategory.getVisibility() == View.VISIBLE) {
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
            android.util.Log.e("PostActivity", "Error setting up subcategory dropdown listener: " + e.getMessage(), e);
            ToastHelper.showError(this, "Error setting up subcategory dropdown listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleMainCategorySelection(String selectedCategoryName) {
        try {
            if (selectedCategoryName == null || selectedCategoryName.equals("Choose")) {
                // Hide subcategory dropdown and size dropdown, reset
                hideSubcategoryDropdown();
                hideSizeDropdown();
                selectedMainCategoryId = null;
                return;
            }
            
            // Find the selected main category
            Category selectedCategory = null;
            if (categories != null) {
                for (Category category : categories) {
                    if (category != null && category.getName() != null && category.getName().equals(selectedCategoryName)) {
                        selectedCategory = category;
                        break;
                    }
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
                    if (categoryManager != null && selectedMainCategoryId != null) {
                        subcategories = categoryManager.getSubCategories(selectedMainCategoryId);
                        
                        if (subcategories != null && !subcategories.isEmpty()) {
                            // Show subcategory dropdown with subcategories
                            showSubcategoryDropdown(subcategories);
                        } else {
                            // No subcategories available, hide the dropdown
                            hideSubcategoryDropdown();
                        }
                    } else {
                        hideSubcategoryDropdown();
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error handling main category selection: " + e.getMessage(), e);
            ToastHelper.showError(this, "Error handling main category selection: " + e.getMessage());
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
                ArrayAdapter<String> subcategoryAdapter = new ArrayAdapter<>(this, 
                        android.R.layout.simple_dropdown_item_1line, subcategoryNames);
                actvSubcategory.setAdapter(subcategoryAdapter);
                actvSubcategory.setThreshold(0);
                
                // Show the subcategory layout
                layoutSubcategory.setVisibility(View.VISIBLE);
                actvSubcategory.setText("Choose Subcategory", false);
            }
        } catch (Exception e) {
            ToastHelper.showError(this, "Error showing subcategory dropdown: " + e.getMessage());
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
            ToastHelper.showError(this, "Error hiding subcategory dropdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showSizeDropdown() {
        try {
            if (layoutSize != null) {
                layoutSize.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            ToastHelper.showError(this, "Error showing size dropdown: " + e.getMessage());
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
            ToastHelper.showError(this, "Error hiding size dropdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void togglePriceMode(boolean forSale) {
        try {
            isForSale = forSale;
            if (btnForSale != null && btnForFree != null) {
                if (forSale) {
                    btnForSale.setBackgroundResource(R.drawable.button_primary);
                    btnForSale.setTextColor(getResources().getColor(R.color.white));
                    btnForFree.setBackgroundResource(R.drawable.button_secondary);
                    btnForFree.setTextColor(getResources().getColor(R.color.text_primary));
                } else {
                    btnForSale.setBackgroundResource(R.drawable.button_secondary);
                    btnForSale.setTextColor(getResources().getColor(R.color.text_primary));
                    btnForFree.setBackgroundResource(R.drawable.button_primary);
                    btnForFree.setTextColor(getResources().getColor(R.color.white));
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error toggling price mode: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    private void toggleOptionalDetails() {
        try {
            isOptionalDetailsVisible = !isOptionalDetailsVisible;
            if (layoutOptionalDetails != null && btnToggleOptional != null) {
                if (isOptionalDetailsVisible) {
                    layoutOptionalDetails.setVisibility(View.VISIBLE);
                    btnToggleOptional.setText("Hide ^");
                } else {
                    layoutOptionalDetails.setVisibility(View.GONE);
                    btnToggleOptional.setText("Show v");
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error toggling optional details: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    private void openImagePicker() {
        try {
            if (selectedImages == null) {
                selectedImages = new ArrayList<>();
            }
            
            if (selectedImages.size() >= MAX_IMAGES) {
                ToastHelper.showWarning(this, "Maximum " + MAX_IMAGES + " images allowed");
                return;
            }
            
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error opening image picker: " + e.getMessage(), e);
            ToastHelper.showError(this, "Error opening image picker: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Add photo callback from adapter
    @Override
    public void onAddPhotoClick() {
        openImagePicker();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        try {
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (selectedImages == null) {
                        selectedImages = new ArrayList<>();
                    }
                    
                    if (data.getClipData() != null) {
                        // Multiple images selected
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count && selectedImages.size() < MAX_IMAGES; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            if (imageUri != null) {
                                selectedImages.add(imageUri.toString());
                            }
                        }
                    } else if (data.getData() != null) {
                        // Single image selected
                        Uri imageUri = data.getData();
                        if (imageUri != null) {
                            selectedImages.add(imageUri.toString());
                        }
                    }
                    
                    if (imageAdapter != null) {
                        imageAdapter.notifyDataSetChanged();
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error handling image selection: " + e.getMessage(), e);
            ToastHelper.showError(this, "Error selecting images: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void addTagsFromInput() {
        try {
            if (etItemDescription == null || selectedTags == null) {
                return;
            }
            
            String tagsText = etItemDescription.getText().toString().trim();
            if (!TextUtils.isEmpty(tagsText)) {
                String[] tags = tagsText.split(",");
                for (String tag : tags) {
                    String trimmedTag = tag.trim();
                    if (!TextUtils.isEmpty(trimmedTag) && !selectedTags.contains(trimmedTag)) {
                        selectedTags.add(trimmedTag);
                    }
                }
                if (tagsAdapter != null) {
                    tagsAdapter.notifyDataSetChanged();
                }
                etItemDescription.setText("");
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error adding tags from input: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    private void postItem() {
        try {
            // Check if required views are initialized
            if (btnPostItem == null || itemManager == null) {
                ToastHelper.showError(this, "Form not ready. Please try again.");
                return;
            }
            
            // Validate user email
            if (TextUtils.isEmpty(loggedInUserEmail)) {
                ToastHelper.showError(this, "User not logged in. Please log in again.");
                return;
            }
            
            ItemData itemData = createItemData();
            if (itemData != null) {
                // Show loading state
                btnPostItem.setEnabled(false);
                btnPostItem.setText("Posting...");
                
                boolean success = itemManager.createItem(itemData, loggedInUserEmail);
                if (success) {
                    ToastHelper.showSuccess(this, "Item posted successfully!");
                    clearForm();
                } else {
                    ToastHelper.showError(this, "Failed to post item. Please try again.");
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error posting item: " + e.getMessage(), e);
            ToastHelper.showError(this, "Error posting item: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Reset button state
            if (btnPostItem != null) {
                btnPostItem.setEnabled(true);
                btnPostItem.setText("List it!");
            }
        }
    }
    
    private ItemData createItemData() {
        // Check if required views are initialized
        if (etItemTitle == null || actvCategory == null) {
            android.util.Log.e("PostActivity", "Required views not initialized in createItemData");
            return null;
        }
        
        // Validate required fields
        if (!validateForm()) {
            return null;
        }
        
        ItemData itemData = new ItemData();
        
        // Basic information
        itemData.setTitle(etItemTitle.getText().toString().trim());
        if (etItemDescription != null) {
            itemData.setDescription(etItemDescription.getText().toString().trim());
        }
        itemData.setCategoryId(getSelectedCategoryId());
        if (actvCondition != null) {
            itemData.setCondition(actvCondition.getText().toString().trim());
        }
        
        // Pricing
        if (isForSale) {
            if (etStartingPrice != null) {
                try {
                    double startingPrice = Double.parseDouble(etStartingPrice.getText().toString().trim());
                    itemData.setStartingPrice(startingPrice);
                } catch (NumberFormatException e) {
                    ToastHelper.showError(this, "Invalid price format");
                    return null;
                }
            } else {
                ToastHelper.showError(this, "Starting price field not available");
                return null;
            }
        } else {
            itemData.setStartingPrice(0.0); // Free item
        }
        
        // Additional fields
        if (actvSize != null) {
            String size = actvSize.getText().toString().trim();
            if (!TextUtils.isEmpty(size) && !size.equals("Choose")) {
                itemData.setMetadata("Size: " + size);
            }
        }
        
        if (actvFeatures != null) {
            String features = actvFeatures.getText().toString().trim();
            if (!TextUtils.isEmpty(features) && !features.equals("Choose")) {
                String metadata = itemData.getMetadata();
                if (TextUtils.isEmpty(metadata)) {
                    metadata = "Features: " + features;
                } else {
                    metadata += ", Features: " + features;
                }
                itemData.setMetadata(metadata);
            }
        }
        
        if (actvOrigin != null) {
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
        }
        
        // Images and tags
        if (selectedImages != null) {
            itemData.setImagePaths(new ArrayList<>(selectedImages));
        } else {
            itemData.setImagePaths(new ArrayList<>());
        }
        if (selectedTags != null) {
            itemData.setTags(new ArrayList<>(selectedTags));
        } else {
            itemData.setTags(new ArrayList<>());
        }
        
        // Set default auction duration (7 days)
        setAuctionDuration(itemData);
        
        return itemData;
    }
    
    private boolean validateForm() {
        // Check if required views are initialized
        if (etItemTitle == null || actvCategory == null) {
            android.util.Log.e("PostActivity", "Required views not initialized in validateForm");
            return false;
        }
        
        // Validate title
        String title = etItemTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            ToastHelper.showError(this, "Listing title is required");
            etItemTitle.requestFocus();
            return false;
        }
        if (title.length() < 3) {
            ToastHelper.showError(this, "Title must be at least 3 characters long");
            etItemTitle.requestFocus();
            return false;
        }
        if (title.length() > 100) {
            ToastHelper.showError(this, "Title must be less than 100 characters");
            etItemTitle.requestFocus();
            return false;
        }
        
        // Validate category
        String category = actvCategory.getText().toString().trim();
        if (TextUtils.isEmpty(category) || category.equals("Choose")) {
            ToastHelper.showError(this, "Category is required");
            actvCategory.requestFocus();
            return false;
        }
        
        // Validate price for sale items
        if (isForSale) {
            if (etStartingPrice == null) {
                android.util.Log.e("PostActivity", "etStartingPrice is null");
                return false;
            }
            String priceText = etStartingPrice.getText().toString().trim();
            if (TextUtils.isEmpty(priceText)) {
                ToastHelper.showError(this, "Price is required for items for sale");
                etStartingPrice.requestFocus();
                return false;
            }
            try {
                double price = Double.parseDouble(priceText);
                if (price < 0) {
                    ToastHelper.showError(this, "Price cannot be negative");
                    etStartingPrice.requestFocus();
                    return false;
                }
                if (price > 1000000) {
                    ToastHelper.showError(this, "Price seems too high. Please verify the amount");
                    etStartingPrice.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                ToastHelper.showError(this, "Please enter a valid price");
                etStartingPrice.requestFocus();
                return false;
            }
        }
        
        // Validate condition
        if (actvCondition != null) {
            String condition = actvCondition.getText().toString().trim();
            if (TextUtils.isEmpty(condition) || condition.equals("Choose")) {
                ToastHelper.showError(this, "Condition is required");
                actvCondition.requestFocus();
                return false;
            }
        }
        
        // Validate images
        if (selectedImages == null || selectedImages.isEmpty()) {
            ToastHelper.showError(this, "At least one photo is required");
            return false;
        }
        if (selectedImages.size() > MAX_IMAGES) {
            ToastHelper.showError(this, "Maximum " + MAX_IMAGES + " photos allowed");
            return false;
        }
        
        // Validate description length
        if (etItemDescription != null) {
            String description = etItemDescription.getText().toString().trim();
            if (!TextUtils.isEmpty(description) && description.length() > 1000) {
                ToastHelper.showError(this, "Description must be less than 1000 characters");
                etItemDescription.requestFocus();
                return false;
            }
        }
        
        return true;
    }
    
    private String getSelectedCategoryId() {
        // First check if a subcategory is selected
        if (actvSubcategory != null) {
            String selectedSubcategoryName = actvSubcategory.getText().toString().trim();
            if (!TextUtils.isEmpty(selectedSubcategoryName) && 
                !selectedSubcategoryName.equals("Choose Subcategory") && 
                !selectedSubcategoryName.equals("Choose")) {
                
                // Find the subcategory ID
                if (subcategories != null) {
                    for (Category subcategory : subcategories) {
                        if (subcategory != null && subcategory.getName().equals(selectedSubcategoryName)) {
                            return subcategory.getCategoryId();
                        }
                    }
                }
            }
        }
        
        // If no subcategory selected, use main category
        if (actvCategory != null) {
            String selectedCategoryName = actvCategory.getText().toString().trim();
            if (!TextUtils.isEmpty(selectedCategoryName) && !selectedCategoryName.equals("Choose")) {
                return selectedMainCategoryId;
            }
        }
        
        return null;
    }
    
    private void setAuctionDuration(ItemData itemData) {
        // Set default auction duration to 7 days
        Calendar calendar = Calendar.getInstance();
        itemData.setStartDate(calendar.getTime());
        
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        itemData.setEndDate(calendar.getTime());
    }
    
    private void clearForm() {
        try {
            // Clear text fields with null checks
            if (etItemTitle != null) etItemTitle.setText("");
            if (etItemDescription != null) etItemDescription.setText("");
            if (actvCategory != null) actvCategory.setText("Choose");
            if (actvCondition != null) actvCondition.setText("Choose");
            if (etStartingPrice != null) etStartingPrice.setText("");
            if (actvSize != null) actvSize.setText("Choose");
            if (actvFeatures != null) actvFeatures.setText("Choose");
            if (actvOrigin != null) actvOrigin.setText("Choose");
            
            // Reset subcategory dropdown and size dropdown
            hideSubcategoryDropdown();
            hideSizeDropdown();
            selectedMainCategoryId = null;
            if (subcategories != null) {
                subcategories.clear();
            }
            
            // Reset checkboxes
            if (cbQuantity != null) cbQuantity.setChecked(false);
            if (cbContact != null) cbContact.setChecked(true);
            if (cbMeetup != null) cbMeetup.setChecked(false);
            if (cbDelivery != null) cbDelivery.setChecked(false);
            
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
            
            // Clear images and tags
            if (selectedImages != null) {
                selectedImages.clear();
            }
            if (selectedTags != null) {
                selectedTags.clear();
            }
            if (imageAdapter != null) {
                imageAdapter.notifyDataSetChanged();
            }
            if (tagsAdapter != null) {
                tagsAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error clearing form: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    // Image adapter callbacks
    @Override
    public void onImageClick(int position, String imagePath) {
        try {
            // Handle image click (e.g., show full screen)
            if (selectedImages != null && position >= 0 && position < selectedImages.size()) {
                Toast.makeText(this, "Image clicked: " + position, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error handling image click: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    public void onImageRemove(int position, String imagePath) {
        try {
            if (selectedImages != null && position >= 0 && position < selectedImages.size()) {
                selectedImages.remove(position);
                if (imageAdapter != null) {
                    imageAdapter.notifyItemRemoved(position);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error removing image: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    // Tags adapter callbacks
    @Override
    public void onTagRemove(int position, String tag) {
        try {
            if (selectedTags != null && position >= 0 && position < selectedTags.size()) {
                selectedTags.remove(position);
                if (tagsAdapter != null) {
                    tagsAdapter.notifyItemRemoved(position);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PostActivity", "Error removing tag: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    public String getCurrentUserEmail() {
        return loggedInUserEmail;
    }
    
    @Override
    protected void setCurrentTabSelected() {
        // Not needed for PostActivity
}

    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false; // PostActivity is not part of bottom navigation
    }
}