package com.cc106.bidhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import java.util.List;

public class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.ImageViewHolder> {
    
    private List<String> images;
    
    public ImageGalleryAdapter() {
        this.images = new java.util.ArrayList<>();
    }
    
    public void setImages(List<String> images) {
        this.images = images != null ? images : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_gallery, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imagePath = images.get(position);
        
        // For now, just set a placeholder
        // In production, this would load the actual image using Glide or Picasso
        holder.imageView.setImageResource(R.drawable.ic_image_placeholder);
        
        // Set content description for accessibility
        holder.imageView.setContentDescription("Item image " + (position + 1));
    }
    
    @Override
    public int getItemCount() {
        return images.size();
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        
        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_gallery_image);
        }
    }
}
