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
    
    // Adapters
    private ItemImageAdapter imageAdapter;
    private TagsAdapter tagsAdapter;
    
    // Data
    private List<String> selectedImages;
    private List<String> selectedTags;
    private List<Category> categories;
    private boolean isForSale = true;
    private boolean isOptionalDetailsVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get the logged-in user's email from intent
        loggedInUserEmail = getIntent().getStringExtra("USER_EMAIL");
        
        // Initialize managers
        itemManager = ItemManager.getInstance(this);
        categoryManager = CategoryManager.getInstance();
        
        // Initialize data
        selectedImages = new ArrayList<>();
        selectedTags = new ArrayList<>();
        categories = categoryManager.getAllMainCategories();
        
        // Set content view
        setContentView(R.layout.activity_post_content);
        
        // Initialize UI
        initializeViews();
        setupAdapters();
        setupDropdowns();
        setupClickListeners();
    }
    
    private void initializeViews() {
        etItemTitle = findViewById(R.id.et_item_title);
        etItemDescription = findViewById(R.id.et_item_description);
        actvCategory = findViewById(R.id.actv_category);
        actvCondition = findViewById(R.id.actv_condition);
        etStartingPrice = findViewById(R.id.et_starting_price);
        actvSize = findViewById(R.id.actv_size);
        actvFeatures = findViewById(R.id.actv_features);
        actvOrigin = findViewById(R.id.actv_origin);
        
        rvItemImages = findViewById(R.id.rv_item_images);
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
    }
    
    private void setupAdapters() {
        // Image adapter
        imageAdapter = new ItemImageAdapter(selectedImages);
        imageAdapter.setOnImageClickListener(this);
        imageAdapter.setOnImageRemoveListener(this);
        imageAdapter.setOnAddPhotoClickListener(this);
        rvItemImages.setLayoutManager(new GridLayoutManager(this, 4));
        rvItemImages.setAdapter(imageAdapter);
        
        // Tags adapter - removed from new layout
        tagsAdapter = new TagsAdapter(selectedTags);
        tagsAdapter.setOnTagRemoveListener(this);
    }
    
    private void setupDropdowns() {
        // Category dropdown - using CategoryManager
        List<String> categoryNames = categoryManager.getMainCategoryNames();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, categoryNames);
        actvCategory.setAdapter(categoryAdapter);
        
        // Condition dropdown
        String[] conditions = {"New", "Like New", "Good", "Fair", "Poor"};
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, conditions);
        actvCondition.setAdapter(conditionAdapter);
        
        // Size dropdown
        String[] sizes = {"XS", "S", "M", "L", "XL", "XXL", "XXXL", "One Size", "Custom"};
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, sizes);
        actvSize.setAdapter(sizeAdapter);
        
        // Features dropdown
        String[] features = {"Brand New", "Used", "Vintage", "Limited Edition", "Rare", "Collectible", "Custom"};
        ArrayAdapter<String> featuresAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, features);
        actvFeatures.setAdapter(featuresAdapter);
        
        // Origin dropdown
        String[] origins = {"Local", "Imported", "Overseas", "Online Purchase", "Gift", "Unknown"};
        ArrayAdapter<String> originAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, origins);
        actvOrigin.setAdapter(originAdapter);
    }
    
    private void setupClickListeners() {
        btnForSale.setOnClickListener(v -> togglePriceMode(true));
        btnForFree.setOnClickListener(v -> togglePriceMode(false));
        btnToggleOptional.setOnClickListener(v -> toggleOptionalDetails());
        btnPostItem.setOnClickListener(v -> postItem());
    }
    
    private void togglePriceMode(boolean forSale) {
        isForSale = forSale;
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
    
    private void toggleOptionalDetails() {
        isOptionalDetailsVisible = !isOptionalDetailsVisible;
        if (isOptionalDetailsVisible) {
            layoutOptionalDetails.setVisibility(View.VISIBLE);
            btnToggleOptional.setText("Hide ^");
        } else {
            layoutOptionalDetails.setVisibility(View.GONE);
            btnToggleOptional.setText("Show v");
        }
    }
    
    private void openImagePicker() {
        if (selectedImages.size() >= MAX_IMAGES) {
            ToastHelper.showWarning(this, "Maximum " + MAX_IMAGES + " images allowed");
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGE_REQUEST);
    }
    
    // Add photo callback from adapter
    @Override
    public void onAddPhotoClick() {
        openImagePicker();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                if (data.getClipData() != null) {
                    // Multiple images selected
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count && selectedImages.size() < MAX_IMAGES; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        selectedImages.add(imageUri.toString());
                    }
                } else if (data.getData() != null) {
                    // Single image selected
                    Uri imageUri = data.getData();
                    selectedImages.add(imageUri.toString());
                }
                
                imageAdapter.notifyDataSetChanged();
            }
        }
    }
    
    private void addTagsFromInput() {
        String tagsText = etItemDescription.getText().toString().trim();
        if (!TextUtils.isEmpty(tagsText)) {
            String[] tags = tagsText.split(",");
            for (String tag : tags) {
                String trimmedTag = tag.trim();
                if (!TextUtils.isEmpty(trimmedTag) && !selectedTags.contains(trimmedTag)) {
                    selectedTags.add(trimmedTag);
                }
            }
            tagsAdapter.notifyDataSetChanged();
            etItemDescription.setText("");
        }
    }
    
    private void postItem() {
        ItemData itemData = createItemData();
        if (itemData != null) {
            try {
                itemManager.createItem(itemData, loggedInUserEmail);
                ToastHelper.showSuccess(this, "Item posted successfully!");
                clearForm();
            } catch (Exception e) {
                ToastHelper.showError(this, "Failed to post item");
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
            } catch (NumberFormatException e) {
                ToastHelper.showError(this, "Invalid price format");
                return null;
            }
        } else {
            itemData.setStartingPrice(0.0); // Free item
        }
        
        // Additional fields
        String size = actvSize.getText().toString().trim();
        if (!TextUtils.isEmpty(size) && !size.equals("Choose")) {
            itemData.setMetadata("Size: " + size);
        }
        
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
        
        // Images and tags
        itemData.setImagePaths(new ArrayList<>(selectedImages));
        itemData.setTags(new ArrayList<>(selectedTags));
        
        // Set default auction duration (7 days)
        setAuctionDuration(itemData);
        
        return itemData;
    }
    
    private boolean validateForm() {
        if (TextUtils.isEmpty(etItemTitle.getText())) {
            ToastHelper.showError(this, "Listing title is required");
            etItemTitle.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(actvCategory.getText()) || actvCategory.getText().toString().equals("Choose")) {
            ToastHelper.showError(this, "Category is required");
            actvCategory.requestFocus();
            return false;
        }
        
        if (isForSale && TextUtils.isEmpty(etStartingPrice.getText())) {
            ToastHelper.showError(this, "Price is required for items for sale");
            etStartingPrice.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(actvCondition.getText()) || actvCondition.getText().toString().equals("Choose")) {
            ToastHelper.showError(this, "Condition is required");
            actvCondition.requestFocus();
            return false;
        }
        
        if (selectedImages.isEmpty()) {
            ToastHelper.showError(this, "At least one photo is required");
            return false;
        }
        
        return true;
    }
    
    private String getSelectedCategoryId() {
        String selectedCategoryName = actvCategory.getText().toString().trim();
        for (Category category : categories) {
            if (category.getName().equals(selectedCategoryName)) {
                return category.getCategoryId();
            }
        }
        return "";
    }
    
    private void setAuctionDuration(ItemData itemData) {
        // Set default auction duration to 7 days
        Calendar calendar = Calendar.getInstance();
        itemData.setStartDate(calendar.getTime());
        
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        itemData.setEndDate(calendar.getTime());
    }
    
    private void clearForm() {
        etItemTitle.setText("");
        etItemDescription.setText("");
        actvCategory.setText("Choose");
        actvCondition.setText("Choose");
        etStartingPrice.setText("");
        actvSize.setText("Choose");
        actvFeatures.setText("Choose");
        actvOrigin.setText("Choose");
        
        // Reset checkboxes
        cbQuantity.setChecked(false);
        cbContact.setChecked(true);
        cbMeetup.setChecked(false);
        cbDelivery.setChecked(false);
        
        // Reset price mode
        togglePriceMode(true);
        
        // Reset optional details visibility
        isOptionalDetailsVisible = true;
        layoutOptionalDetails.setVisibility(View.VISIBLE);
        btnToggleOptional.setText("Hide ^");
        
        selectedImages.clear();
        selectedTags.clear();
        imageAdapter.notifyDataSetChanged();
        tagsAdapter.notifyDataSetChanged();
    }
    
    // Image adapter callbacks
    @Override
    public void onImageClick(int position, String imagePath) {
        // Handle image click (e.g., show full screen)
        Toast.makeText(this, "Image clicked: " + position, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onImageRemove(int position, String imagePath) {
        selectedImages.remove(position);
        imageAdapter.notifyItemRemoved(position);
    }
    
    // Tags adapter callbacks
    @Override
    public void onTagRemove(int position, String tag) {
        selectedTags.remove(position);
        tagsAdapter.notifyItemRemoved(position);
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