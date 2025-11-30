package com.cc106.bidhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import com.cc106.bidhub.utils.ImageLoader;
import java.util.List;

/**
 * Adapter for ViewPager2 to display full-screen images
 * Note: ViewPager2 uses RecyclerView.Adapter internally
 */
public class FullScreenImageAdapter extends RecyclerView.Adapter<FullScreenImageAdapter.ImageViewHolder> {
    
    private List<String> images;
    
    public FullScreenImageAdapter(List<String> images) {
        this.images = images != null ? images : new java.util.ArrayList<>();
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fullscreen_image, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        if (position < 0 || position >= images.size()) {
            return;
        }
        
        String imagePath = images.get(position);
        
        // Load image with ImageView (supports basic scaling)
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
        holder.imageView.setContentDescription("Item image " + (position + 1) + " of " + images.size());
    }
    
    @Override
    public int getItemCount() {
        return images != null ? images.size() : 0;
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        
        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.photo_view);
        }
    }
}

