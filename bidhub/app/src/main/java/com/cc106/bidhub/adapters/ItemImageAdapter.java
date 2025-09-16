package com.cc106.bidhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import java.util.List;

/**
 * Adapter for displaying item images in a grid with add photo functionality
 */
public class ItemImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_IMAGE = 0;
    private static final int VIEW_TYPE_ADD_PHOTO = 1;
    private static final int MAX_IMAGES = 10;
    
    private List<String> imagePaths;
    private OnImageClickListener onImageClickListener;
    private OnImageRemoveListener onImageRemoveListener;
    private OnAddPhotoClickListener onAddPhotoClickListener;
    
    public interface OnImageClickListener {
        void onImageClick(int position, String imagePath);
    }
    
    public interface OnImageRemoveListener {
        void onImageRemove(int position, String imagePath);
    }
    
    public interface OnAddPhotoClickListener {
        void onAddPhotoClick();
    }
    
    public ItemImageAdapter(List<String> imagePaths) {
        this.imagePaths = imagePaths;
    }
    
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }
    
    public void setOnImageRemoveListener(OnImageRemoveListener listener) {
        this.onImageRemoveListener = listener;
    }
    
    public void setOnAddPhotoClickListener(OnAddPhotoClickListener listener) {
        this.onAddPhotoClickListener = listener;
    }
    
    @Override
    public int getItemViewType(int position) {
        if (position < imagePaths.size()) {
            return VIEW_TYPE_IMAGE;
        } else {
            return VIEW_TYPE_ADD_PHOTO;
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_IMAGE) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image_card, parent, false);
            return new ImageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_add_photo, parent, false);
            return new AddPhotoViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ImageViewHolder) {
            ImageViewHolder imageHolder = (ImageViewHolder) holder;
            String imagePath = imagePaths.get(position);
            
            // Set image (in a real app, you'd use Glide or Picasso)
            imageHolder.imageView.setImageResource(R.drawable.ic_image_placeholder);
            
            // Set click listeners
            imageHolder.imageView.setOnClickListener(v -> {
                if (onImageClickListener != null) {
                    onImageClickListener.onImageClick(position, imagePath);
                }
            });
            
            imageHolder.removeButton.setOnClickListener(v -> {
                if (onImageRemoveListener != null) {
                    onImageRemoveListener.onImageRemove(position, imagePath);
                }
            });
        } else if (holder instanceof AddPhotoViewHolder) {
            AddPhotoViewHolder addPhotoHolder = (AddPhotoViewHolder) holder;
            addPhotoHolder.itemView.setOnClickListener(v -> {
                if (onAddPhotoClickListener != null) {
                    onAddPhotoClickListener.onAddPhotoClick();
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        // Show add photo button if we haven't reached the limit
        int imageCount = imagePaths.size();
        if (imageCount < MAX_IMAGES) {
            return imageCount + 1; // +1 for add photo button
        }
        return imageCount;
    }
    
    public void addImage(String imagePath) {
        if (imagePaths.size() < MAX_IMAGES) {
            imagePaths.add(imagePath);
            notifyItemInserted(imagePaths.size() - 1);
        }
    }
    
    public void removeImage(int position) {
        if (position >= 0 && position < imagePaths.size()) {
            imagePaths.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    public void updateImages(List<String> newImagePaths) {
        this.imagePaths = newImagePaths;
        notifyDataSetChanged();
    }
    
    public boolean canAddMoreImages() {
        return imagePaths.size() < MAX_IMAGES;
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView removeButton;
        
        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_item_image);
            removeButton = itemView.findViewById(R.id.iv_remove_image);
        }
    }
    
    static class AddPhotoViewHolder extends RecyclerView.ViewHolder {
        
        AddPhotoViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
