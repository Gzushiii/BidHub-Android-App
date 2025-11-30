package com.cc106.bidhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import com.cc106.bidhub.items.Item;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying items in a grid layout for browsing
 */
public class ItemCardAdapter extends RecyclerView.Adapter<ItemCardAdapter.ItemViewHolder> {
    
    private List<Item> items;
    private OnItemClickListener onItemClickListener;
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;
    
    public interface OnItemClickListener {
        void onItemClick(Item item);
    }
    
    public ItemCardAdapter(List<Item> items) {
        this.items = items;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        this.dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ItemViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        try {
            if (position < 0 || position >= items.size()) {
                return;
            }
            
            Item item = items.get(position);
            if (item == null) {
                return;
            }
            
            // Set item data with null safety
            holder.titleText.setText(item.getTitle() != null ? item.getTitle() : "Untitled Item");
            holder.priceText.setText(currencyFormat.format(item.getCurrentPrice()));
            
            // Display seller name - use sellerName if available, otherwise fallback
            String sellerName = item.getSellerName();
            if (sellerName == null || sellerName.isEmpty() || sellerName.equals("Unknown")) {
                // Try to extract from sellerId (email)
                String sellerId = item.getSellerId();
                if (sellerId != null && sellerId.contains("@")) {
                    int atIndex = sellerId.indexOf('@');
                    if (atIndex > 0) {
                        sellerName = sellerId.substring(0, atIndex);
                    } else {
                        sellerName = "Unknown";
                    }
                } else {
                    sellerName = "Unknown";
                }
            }
            holder.sellerText.setText("by " + sellerName);
            
            // Display bid count
            int bidCount = item.getBidCount();
            if (bidCount > 0) {
                holder.bidCountText.setText(bidCount + (bidCount == 1 ? " bid" : " bids"));
            } else {
                holder.bidCountText.setText("No bids yet");
            }
        
        // Set time remaining with enhanced countdown
        boolean isEndingSoon = false;
        if (item.getEndDate() != null) {
            long timeRemaining = item.getEndDate().getTime() - System.currentTimeMillis();
            if (timeRemaining > 0) {
                long days = timeRemaining / (1000 * 60 * 60 * 24);
                long hours = (timeRemaining % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
                long minutes = (timeRemaining % (1000 * 60 * 60)) / (1000 * 60);
                
                // Show "Ending Soon" if less than 24 hours remaining
                isEndingSoon = days == 0 && hours < 24;
                
                // Format time remaining
                if (days > 0) {
                    holder.timeRemainingText.setText(days + "d " + hours + "h left");
                } else if (hours > 0) {
                    holder.timeRemainingText.setText(hours + "h " + minutes + "m left");
                } else {
                    holder.timeRemainingText.setText(minutes + "m left");
                }
                
                // Set color based on urgency
                if (isEndingSoon) {
                    holder.timeRemainingText.setTextColor(holder.itemView.getContext().getColor(R.color.error_red));
                } else {
                    holder.timeRemainingText.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
                }
            } else {
                holder.timeRemainingText.setText("Ended");
                holder.timeRemainingText.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
            }
        } else {
            holder.timeRemainingText.setText("No end date");
            holder.timeRemainingText.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
        }
        
        // Show/hide "Ending Soon" badge
        if (holder.endingSoonText != null) {
            if (isEndingSoon) {
                holder.endingSoonText.setVisibility(View.VISIBLE);
            } else {
                holder.endingSoonText.setVisibility(View.GONE);
            }
        }
        
        // Set item image - load user images with Glide, fallback to placeholder
        if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
            // Load first image from user uploads using Glide
            String firstImagePath = item.getImagePaths().get(0);
            
            // Validate image URL before loading
            if (firstImagePath != null && !firstImagePath.isEmpty() && !firstImagePath.equals("null")) {
                // Show image count badge if multiple images
                if (item.getImagePaths().size() > 1 && holder.imageCountCard != null && holder.imageCountText != null) {
                    holder.imageCountCard.setVisibility(View.VISIBLE);
                    holder.imageCountText.setText(String.valueOf(item.getImagePaths().size()));
                } else if (holder.imageCountCard != null) {
                    holder.imageCountCard.setVisibility(View.GONE);
                }
                
                com.cc106.bidhub.utils.ImageLoader.loadImageWithErrorCallback(
                    holder.itemImage.getContext(),
                    firstImagePath,
                    holder.itemImage,
                    new com.cc106.bidhub.utils.ImageLoader.ImageLoadErrorCallback() {
                        @Override
                        public void onError(String errorMessage) {
                            android.util.Log.w("ItemCardAdapter", "Failed to load image: " + firstImagePath + " - " + errorMessage);
                            // Fallback to placeholder on error
                            com.cc106.bidhub.utils.ImageLoader.loadPlaceholder(holder.itemImage.getContext(), holder.itemImage);
                        }
                    }
                );
            } else {
                // Invalid image path, use placeholder
                android.util.Log.w("ItemCardAdapter", "Invalid image path for item: " + item.getTitle());
                com.cc106.bidhub.utils.ImageLoader.loadPlaceholder(holder.itemImage.getContext(), holder.itemImage);
                if (holder.imageCountCard != null) {
                    holder.imageCountCard.setVisibility(View.GONE);
                }
            }
        } else {
            // No images uploaded, use placeholder
            com.cc106.bidhub.utils.ImageLoader.loadPlaceholder(holder.itemImage.getContext(), holder.itemImage);
            if (holder.imageCountCard != null) {
                holder.imageCountCard.setVisibility(View.GONE);
            }
        }
        
        
            // Set click listener with detailed error handling
            holder.itemView.setOnClickListener(v -> {
                try {
                    if (onItemClickListener != null && item != null) {
                        onItemClickListener.onItemClick(item);
                    } else {
                        com.cc106.bidhub.utils.ErrorHandler.logWarning(
                            "ItemCardAdapter", 
                            "Item click listener or item is null",
                            String.format("Listener: %s, Item: %s", 
                                onItemClickListener != null ? "not null" : "null",
                                item != null ? item.getItemId() : "null")
                        );
                    }
                } catch (Exception e) {
                    com.cc106.bidhub.utils.ErrorHandler.handleAdapterError(
                        holder.itemView.getContext(),
                        "ItemCardAdapter",
                        "item click",
                        e,
                        String.format("ItemID: %s, Title: %s", 
                            item != null ? item.getItemId() : "null",
                            item != null ? item.getTitle() : "null")
                    );
                }
            });
            
        } catch (Exception e) {
            com.cc106.bidhub.utils.ErrorHandler.handleAdapterError(
                holder.itemView.getContext(),
                "ItemCardAdapter",
                "bind view holder",
                e,
                String.format("Position: %d", position)
            );
        }
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    public void updateItems(List<Item> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }
    
    public void addItems(List<Item> newItems) {
        int startPosition = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(startPosition, newItems.size());
    }
    
    public void clearItems() {
        int itemCount = items.size();
        items.clear();
        notifyItemRangeRemoved(0, itemCount);
    }
    
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView titleText;
        TextView priceText;
        TextView sellerText;
        TextView bidCountText;
        TextView timeRemainingText;
        TextView endingSoonText;
        com.google.android.material.card.MaterialCardView imageCountCard;
        TextView imageCountText;
        
        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.iv_item_image);
            titleText = itemView.findViewById(R.id.tv_item_title);
            priceText = itemView.findViewById(R.id.tv_item_price);
            sellerText = itemView.findViewById(R.id.tv_seller_name);
            bidCountText = itemView.findViewById(R.id.tv_bid_count);
            timeRemainingText = itemView.findViewById(R.id.tv_time_remaining);
            endingSoonText = itemView.findViewById(R.id.tv_ending_soon);
            imageCountCard = itemView.findViewById(R.id.card_image_count);
            imageCountText = itemView.findViewById(R.id.tv_image_count);
        }
    }
}
