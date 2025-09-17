package com.cc106.bidhub.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.cc106.bidhub.R;
import com.cc106.bidhub.items.FilterCriteria;
import com.cc106.bidhub.items.ItemManager;
import com.cc106.bidhub.items.Category;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class FilterDialogFragment extends DialogFragment {
    
    private FilterCriteria filterCriteria;
    private OnFilterAppliedListener listener;
    
    // UI Components
    private Spinner spinnerCategory;
    private TextInputEditText etMinPrice;
    private TextInputEditText etMaxPrice;
    private Spinner spinnerCondition;
    private Spinner spinnerLocation;
    private CheckBox cbFeatured;
    private CheckBox cbTrending;
    private Button btnApply;
    private Button btnClear;
    private Button btnCancel;
    
    private ItemManager itemManager;
    
    public interface OnFilterAppliedListener {
        void onFilterApplied(FilterCriteria filterCriteria);
    }
    
    public void setFilterCriteria(FilterCriteria filterCriteria) {
        this.filterCriteria = filterCriteria;
    }
    
    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogTheme);
        itemManager = ItemManager.getInstance(getContext());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_filter, container, false);
        
        initializeViews(view);
        setupSpinners();
        populateFields();
        setupClickListeners();
        
        return view;
    }
    
    private void initializeViews(View view) {
        spinnerCategory = view.findViewById(R.id.spinner_category);
        etMinPrice = view.findViewById(R.id.et_min_price);
        etMaxPrice = view.findViewById(R.id.et_max_price);
        spinnerCondition = view.findViewById(R.id.spinner_condition);
        spinnerLocation = view.findViewById(R.id.spinner_location);
        cbFeatured = view.findViewById(R.id.cb_featured);
        cbTrending = view.findViewById(R.id.cb_trending);
        btnApply = view.findViewById(R.id.btn_apply);
        btnClear = view.findViewById(R.id.btn_clear);
        btnCancel = view.findViewById(R.id.btn_cancel);
    }
    
    private void setupSpinners() {
        // Category Spinner
        List<Category> categories = itemManager.getAllCategories();
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("All Categories");
        for (Category category : categories) {
            categoryNames.add(category.getName());
        }
        
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        
        // Condition Spinner
        String[] conditions = {"Any Condition", "New", "Like New", "Very Good", "Good", "Fair", "Poor"};
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, conditions);
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondition.setAdapter(conditionAdapter);
        
        // Location Spinner
        String[] locations = {"Any Location", "Manila", "Quezon City", "Makati", "Taguig", "Pasig", 
                             "Marikina", "Mandaluyong", "San Juan", "Other"};
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, locations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);
    }
    
    private void populateFields() {
        if (filterCriteria == null) {
            filterCriteria = new FilterCriteria();
            return;
        }
        
        // Set category
        if (filterCriteria.getCategoryId() != null) {
            Category category = itemManager.getCategoryById(filterCriteria.getCategoryId());
            if (category != null) {
                List<Category> categories = itemManager.getAllCategories();
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).getCategoryId().equals(filterCriteria.getCategoryId())) {
                        spinnerCategory.setSelection(i + 1); // +1 because first item is "All Categories"
                        break;
                    }
                }
            }
        }
        
        // Set price range
        if (filterCriteria.getMinPrice() != null) {
            etMinPrice.setText(String.format("%.0f", filterCriteria.getMinPrice()));
        }
        if (filterCriteria.getMaxPrice() != null) {
            etMaxPrice.setText(String.format("%.0f", filterCriteria.getMaxPrice()));
        }
        
        // Set condition
        if (filterCriteria.getCondition() != null) {
            String[] conditions = {"Any Condition", "New", "Like New", "Very Good", "Good", "Fair", "Poor"};
            for (int i = 0; i < conditions.length; i++) {
                if (conditions[i].equals(filterCriteria.getCondition())) {
                    spinnerCondition.setSelection(i);
                    break;
                }
            }
        }
        
        // Set location
        if (filterCriteria.getLocation() != null) {
            String[] locations = {"Any Location", "Manila", "Quezon City", "Makati", "Taguig", "Pasig", 
                                 "Marikina", "Mandaluyong", "San Juan", "Other"};
            for (int i = 0; i < locations.length; i++) {
                if (locations[i].equals(filterCriteria.getLocation())) {
                    spinnerLocation.setSelection(i);
                    break;
                }
            }
        }
        
        // Set checkboxes
        cbFeatured.setChecked(filterCriteria.getIsFeatured() != null && filterCriteria.getIsFeatured());
        cbTrending.setChecked(filterCriteria.getIsTrending() != null && filterCriteria.getIsTrending());
    }
    
    private void setupClickListeners() {
        btnApply.setOnClickListener(v -> applyFilters());
        btnClear.setOnClickListener(v -> clearFilters());
        btnCancel.setOnClickListener(v -> dismiss());
    }
    
    private void applyFilters() {
        FilterCriteria newFilter = new FilterCriteria();
        
        // Category
        int categoryIndex = spinnerCategory.getSelectedItemPosition();
        if (categoryIndex > 0) {
            List<Category> categories = itemManager.getAllCategories();
            newFilter.setCategoryId(categories.get(categoryIndex - 1).getCategoryId());
        }
        
        // Price range
        String minPriceText = etMinPrice.getText().toString().trim();
        if (!minPriceText.isEmpty()) {
            try {
                newFilter.setMinPrice(Double.parseDouble(minPriceText));
            } catch (NumberFormatException e) {
                // Invalid price, ignore
            }
        }
        
        String maxPriceText = etMaxPrice.getText().toString().trim();
        if (!maxPriceText.isEmpty()) {
            try {
                newFilter.setMaxPrice(Double.parseDouble(maxPriceText));
            } catch (NumberFormatException e) {
                // Invalid price, ignore
            }
        }
        
        // Condition
        int conditionIndex = spinnerCondition.getSelectedItemPosition();
        if (conditionIndex > 0) {
            String[] conditions = {"Any Condition", "New", "Like New", "Very Good", "Good", "Fair", "Poor"};
            newFilter.setCondition(conditions[conditionIndex]);
        }
        
        // Location
        int locationIndex = spinnerLocation.getSelectedItemPosition();
        if (locationIndex > 0) {
            String[] locations = {"Any Location", "Manila", "Quezon City", "Makati", "Taguig", "Pasig", 
                                 "Marikina", "Mandaluyong", "San Juan", "Other"};
            newFilter.setLocation(locations[locationIndex]);
        }
        
        // Featured and Trending
        newFilter.setIsFeatured(cbFeatured.isChecked() ? true : null);
        newFilter.setIsTrending(cbTrending.isChecked() ? true : null);
        
        // Preserve existing query and sorting
        newFilter.setQuery(filterCriteria.getQuery());
        newFilter.setSortBy(filterCriteria.getSortBy());
        newFilter.setSortOrder(filterCriteria.getSortOrder());
        
        if (listener != null) {
            listener.onFilterApplied(newFilter);
        }
        
        dismiss();
    }
    
    private void clearFilters() {
        spinnerCategory.setSelection(0);
        etMinPrice.setText("");
        etMaxPrice.setText("");
        spinnerCondition.setSelection(0);
        spinnerLocation.setSelection(0);
        cbFeatured.setChecked(false);
        cbTrending.setChecked(false);
    }
}
