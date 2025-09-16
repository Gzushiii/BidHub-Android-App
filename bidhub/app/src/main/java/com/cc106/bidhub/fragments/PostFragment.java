package com.cc106.bidhub.fragments;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.Toast;

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
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    private AutoCompleteTextView actvCondition;
    private TextInputEditText etStartingPrice;
    private TextInputEditText etBuyNowPrice;
    private AutoCompleteTextView actvAuctionDuration;
    private TextInputEditText etLocation;
    private TextInputEditText etShippingInfo;
    // Tags functionality removed from new layout
    // private TextInputEditText etTags;
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
        actvCondition = view.findViewById(R.id.actv_condition);
        etStartingPrice = view.findViewById(R.id.et_starting_price);
        actvSize = view.findViewById(R.id.actv_size);
        actvFeatures = view.findViewById(R.id.actv_features);
        actvOrigin = view.findViewById(R.id.actv_origin);
        
        rvItemImages = view.findViewById(R.id.rv_item_images);
        btnForSale = view.findViewById(R.id.btn_for_sale);
        btnForFree = view.findViewById(R.id.btn_for_free);
        btnToggleOptional = view.findViewById(R.id.btn_toggle_optional);
        btnPostItem = view.findViewById(R.id.btn_post_item);
        
        // Back button
        ImageView btnBack = view.findViewById(R.id.btn_back);
        
        // Checkboxes
        cbQuantity = view.findViewById(R.id.cb_quantity);
        cbContact = view.findViewById(R.id.cb_contact);
        cbMeetup = view.findViewById(R.id.cb_meetup);
        cbDelivery = view.findViewById(R.id.cb_delivery);
        
        // Layouts
        layoutOptionalDetails = view.findViewById(R.id.layout_optional_details);
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
            // Category dropdown - using CategoryManager with hierarchical structure
            if (actvCategory != null) {
                try {
                    List<String> categoryNames = categoryManager.getCategoryNames();
                    if (categoryNames == null) {
                        categoryNames = new ArrayList<>();
                        categoryNames.add("Choose");
                    }
                    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), 
                            android.R.layout.simple_dropdown_item_1line, categoryNames);
                    actvCategory.setAdapter(categoryAdapter);
                    actvCategory.setThreshold(0); // Show dropdown immediately
                    setupDropdownClickListener(actvCategory);
                } catch (Exception e) {
                    ToastHelper.showError(getContext(), "Error setting up category dropdown: " + e.getMessage());
                    // Set a basic adapter as fallback
                    String[] fallbackCategories = {"Choose", "Fashion", "Electronics", "Home & Living"};
                    ArrayAdapter<String> fallbackAdapter = new ArrayAdapter<>(getContext(), 
                            android.R.layout.simple_dropdown_item_1line, fallbackCategories);
                    actvCategory.setAdapter(fallbackAdapter);
                }
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
            
            // Features dropdown
            if (actvFeatures != null) {
                String[] features = {"Brand New", "Used", "Vintage", "Limited Edition", "Rare", "Collectible", "Custom"};
                ArrayAdapter<String> featuresAdapter = new ArrayAdapter<>(getContext(), 
                        android.R.layout.simple_dropdown_item_1line, features);
                actvFeatures.setAdapter(featuresAdapter);
                actvFeatures.setThreshold(0); // Show dropdown immediately
                setupDropdownClickListener(actvFeatures);
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
        } else {
            btnForSale.setBackgroundResource(R.drawable.button_secondary);
            btnForSale.setTextColor(getResources().getColor(R.color.text_primary));
            btnForFree.setBackgroundResource(R.drawable.button_primary);
            btnForFree.setTextColor(getResources().getColor(R.color.white));
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
                ToastHelper.showSuccess(getContext(), "Images added: " + selectedImages.size());
            }
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
    
    private void saveAsDraft() {
        ItemData itemData = createItemData();
        if (itemData != null) {
            boolean success = itemManager.createItem(itemData, loggedInUserEmail);
            if (success) {
                ToastHelper.showSuccess(getContext(), "Item saved as draft");
                clearForm();
            } else {
                ToastHelper.showError(getContext(), "Failed to save draft");
            }
        }
    }
    
    private void postItem() {
        ItemData itemData = createItemData();
        if (itemData != null) {
            boolean success = itemManager.createItem(itemData, loggedInUserEmail);
            if (success) {
                // Set item status to active
                // This would require updating the ItemManager to support status changes
                ToastHelper.showSuccess(getContext(), "Item posted successfully!");
                clearForm();
            } else {
                ToastHelper.showError(getContext(), "Failed to post item");
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
                ToastHelper.showError(getContext(), "Invalid price format");
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
        
        // Delivery options
        StringBuilder deliveryOptions = new StringBuilder();
        if (cbMeetup != null && cbMeetup.isChecked()) {
            deliveryOptions.append("Meet-up");
        }
        if (cbDelivery != null && cbDelivery.isChecked()) {
            if (deliveryOptions.length() > 0) {
                deliveryOptions.append(", ");
            }
            deliveryOptions.append("Mailing & Delivery");
        }
        
        if (deliveryOptions.length() > 0) {
            String metadata = itemData.getMetadata();
            if (TextUtils.isEmpty(metadata)) {
                metadata = "Delivery: " + deliveryOptions.toString();
            } else {
                metadata += ", Delivery: " + deliveryOptions.toString();
            }
            itemData.setMetadata(metadata);
        }
        
        // Set default auction duration (7 days)
        setAuctionDuration(itemData);
        
        return itemData;
    }
    
    private boolean validateForm() {
        if (TextUtils.isEmpty(etItemTitle.getText())) {
            ToastHelper.showError(getContext(), "Listing title is required");
            etItemTitle.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(actvCategory.getText()) || actvCategory.getText().toString().equals("Choose")) {
            ToastHelper.showError(getContext(), "Category is required");
            actvCategory.requestFocus();
            return false;
        }
        
        if (isForSale && TextUtils.isEmpty(etStartingPrice.getText())) {
            ToastHelper.showError(getContext(), "Price is required for items for sale");
            etStartingPrice.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(actvCondition.getText()) || actvCondition.getText().toString().equals("Choose")) {
            ToastHelper.showError(getContext(), "Condition is required");
            actvCondition.requestFocus();
            return false;
        }
        
        if (selectedImages.isEmpty()) {
            ToastHelper.showError(getContext(), "At least one photo is required");
            return false;
        }
        
        // Validate delivery options
        if (cbMeetup != null && cbDelivery != null) {
            if (!cbMeetup.isChecked() && !cbDelivery.isChecked()) {
                ToastHelper.showError(getContext(), "Please select at least one delivery option");
                return false;
            }
        }
        
        return true;
    }
    
    private String getSelectedCategoryId() {
        String selectedCategoryName = actvCategory.getText().toString().trim();
        // Use the CategoryManager's method to get category ID from display name
        return categoryManager.getCategoryIdFromDisplayName(selectedCategoryName);
    }
    
    private void setAuctionDuration(ItemData itemData) {
        String duration = actvAuctionDuration.getText().toString().trim();
        Calendar calendar = Calendar.getInstance();
        itemData.setStartDate(calendar.getTime());
        
        int days = 7; // Default
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
        actvSize.setText("Choose");
        actvFeatures.setText("Choose");
        actvOrigin.setText("Choose");
        
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
}
