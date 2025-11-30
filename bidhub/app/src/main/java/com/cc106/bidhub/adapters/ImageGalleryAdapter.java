package com.cc106.bidhub.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.FullScreenImageActivity;
import com.cc106.bidhub.R;
import com.cc106.bidhub.utils.ImageLoader;
import java.util.ArrayList;
import java.util.List;

public class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.ImageViewHolder> {
    
    private List<String> images;
    
    public ImageGalleryAdapter() {
        this.images = new ArrayList<>();
    }
    
    public void setImages(List<String> images) {
        this.images = images != null ? images : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use ViewPager layout for full-screen images (used in ItemDetailActivity)
        // This adapter is primarily used with ViewPager2 for image galleries
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_viewpager_image, parent, false);
        return new ImageViewHolder(view, images);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        if (position < 0 || position >= images.size()) {
            // Load placeholder if position is invalid
            ImageLoader.loadPlaceholder(holder.imageView.getContext(), holder.imageView);
            holder.imageView.setContentDescription("Item image placeholder");
            return;
        }
        
        String imagePath = images.get(position);
        
        // Load actual image using ImageLoader
        ImageLoader.loadImageWithErrorCallback(
            holder.imageView.getContext(),
            imagePath,
            holder.imageView,
            new ImageLoader.ImageLoadErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    // Fallback to placeholder on error
                    ImageLoader.loadPlaceholder(holder.imageView.getContext(), holder.imageView);
                }
            }
        );
        
        // Set content description for accessibility
        holder.imageView.setContentDescription("Item image " + (position + 1) + ". Tap to view full screen");
        
        // Set click listener to open full-screen viewer (only if not already in ViewPager2)
        // ViewPager2 items should be clickable to open full-screen viewer
        holder.imageView.setOnClickListener(v -> {
            if (v.getContext() != null) {
                Intent intent = new Intent(v.getContext(), FullScreenImageActivity.class);
                intent.putStringArrayListExtra("IMAGES", new ArrayList<>(images));
                intent.putExtra("POSITION", position);
                v.getContext().startActivity(intent);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return images != null ? images.size() : 0;
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        
        ImageViewHolder(@NonNull View itemView, List<String> images) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_gallery_image);
        }
    }
}
