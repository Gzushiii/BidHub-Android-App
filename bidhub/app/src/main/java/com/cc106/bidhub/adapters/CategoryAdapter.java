package com.cc106.bidhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import com.cc106.bidhub.items.Category;
import java.util.List;

/**
 * Adapter for displaying categories in filter selection
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    
    private List<Category> categories;
    private OnCategoryClickListener onCategoryClickListener;
    private String selectedCategoryId;
    
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }
    
    public CategoryAdapter(List<Category> categories) {
        this.categories = categories;
    }
    
    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.onCategoryClickListener = listener;
    }
    
    public void setSelectedCategoryId(String categoryId) {
        this.selectedCategoryId = categoryId;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        
        holder.nameText.setText(category.getName());
        holder.itemCountText.setText(category.getItemCount() + " items");
        
        // Set selection state
        boolean isSelected = category.getCategoryId().equals(selectedCategoryId);
        holder.itemView.setSelected(isSelected);
        
        if (isSelected) {
            holder.nameText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
            holder.itemCountText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.primary_blue));
        } else {
            holder.nameText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_primary));
            holder.itemCountText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.transparent));
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onCategoryClickListener != null) {
                onCategoryClickListener.onCategoryClick(category);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    public void updateCategories(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }
    
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView itemCountText;
        
        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_category_name);
            itemCountText = itemView.findViewById(R.id.tv_category_count);
        }
    }
}
