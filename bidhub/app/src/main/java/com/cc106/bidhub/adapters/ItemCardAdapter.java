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
import java.util.ArrayList;
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
            
            // FIX: Display correct price based on bid status
            // If no bids yet, show starting price; otherwise show current highest bid
            int bidCount = item.getBidCount();
            double displayPrice;
            if (bidCount > 0 && item.getCurrentPrice() > 0) {
                // Active bids present - show current highest bid
                displayPrice = item.getCurrentPrice();
            } else {
                // No bids yet - show starting bid
                displayPrice = item.getStartingPrice() > 0 ? item.getStartingPrice() : 0.0;
            }
            holder.priceText.setText(currencyFormat.format(displayPrice));
            
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
            if (holder.sellerText != null) {
                holder.sellerText.setText(sellerName);
            }
            
            // FIX: Display bid count in new format (reuse bidCount from above)
            if (holder.bidCountText != null) {
                if (bidCount > 0) {
                    holder.bidCountText.setText(bidCount + (bidCount == 1 ? " bidder" : " bidders"));
                } else {
                    holder.bidCountText.setText("No Bids Yet");
                }
            }
        
        // FIX: Set status badge and duration with new layout
        boolean isEndingSoon = false;
        int progressPercentage = 0;
        String durationText = "";
        
        if (item.getEndDate() != null) {
            long timeRemaining = item.getEndDate().getTime() - System.currentTimeMillis();
            long totalDuration = item.getEndDate().getTime() - (item.getCreatedAt() != null ? item.getCreatedAt().getTime() : System.currentTimeMillis());
            
            if (timeRemaining > 0 && totalDuration > 0) {
                // Calculate progress percentage
                progressPercentage = (int) ((1.0 - (double) timeRemaining / totalDuration) * 100);
                progressPercentage = Math.max(0, Math.min(100, progressPercentage)); // Clamp between 0-100
                
                long days = timeRemaining / (1000 * 60 * 60 * 24);
                long hours = (timeRemaining % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
                long minutes = (timeRemaining % (1000 * 60 * 60)) / (1000 * 60);
                
                // Show "Ending Soon" if less than 24 hours remaining
                isEndingSoon = days == 0 && hours < 24;
                
                // Format duration text for new layout
                if (days > 0) {
                    durationText = days + "d " + hours + "h " + minutes + "m Left";
                } else if (hours > 0) {
                    durationText = hours + "h " + minutes + "m Left";
                } else {
                    durationText = minutes + "m Left";
                }
                
                // Show time remaining badge if ending soon
                if (holder.timeRemainingBadge != null) {
                    if (isEndingSoon) {
                        holder.timeRemainingBadge.setVisibility(View.VISIBLE);
                        if (holder.timeRemainingText != null) {
                            holder.timeRemainingText.setText(durationText);
                        }
                    } else {
                        holder.timeRemainingBadge.setVisibility(View.GONE);
                    }
                }
            } else {
                durationText = "Ended";
                progressPercentage = 100;
            }
        } else {
            durationText = "No end date";
        }
        
        // Set duration text in new layout
        if (holder.durationLeftText != null) {
            holder.durationLeftText.setText(durationText);
        }
        
        // Set progress bar
        if (holder.progressBar != null) {
            holder.progressBar.setProgress(progressPercentage);
        }
        
        // Set status badge
        if (holder.statusBadge != null && holder.statusBadgeText != null) {
            if (item.getStatus() == com.cc106.bidhub.items.ItemStatus.ACTIVE) {
                holder.statusBadge.setVisibility(View.VISIBLE);
                holder.statusBadgeText.setText("Active");
            } else {
                holder.statusBadge.setVisibility(View.GONE);
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
        // FIX: Ensure we update the list reference and notify properly
        if (newItems == null) {
            this.items = new ArrayList<>();
        } else {
            this.items = newItems;
        }
        // Use notifyDataSetChanged to ensure all views are updated
        notifyDataSetChanged();
        android.util.Log.d("ItemCardAdapter", "Updated adapter with " + this.items.size() + " items");
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
        TextView durationLeftText;
        TextView timeRemainingText;
        TextView statusBadgeText;
        android.widget.ProgressBar progressBar;
        com.google.android.material.card.MaterialCardView statusBadge;
        com.google.android.material.card.MaterialCardView timeRemainingBadge;
        com.google.android.material.button.MaterialButton viewListingButton;
        com.google.android.material.card.MaterialCardView imageCountCard;
        TextView imageCountText;
        
        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.iv_item_image);
            titleText = itemView.findViewById(R.id.tv_item_title);
            priceText = itemView.findViewById(R.id.tv_item_price);
            sellerText = itemView.findViewById(R.id.tv_seller_name);
            bidCountText = itemView.findViewById(R.id.tv_bid_count);
            durationLeftText = itemView.findViewById(R.id.tv_duration_left);
            timeRemainingText = itemView.findViewById(R.id.tv_time_remaining);
            statusBadgeText = itemView.findViewById(R.id.tv_status_badge);
            progressBar = itemView.findViewById(R.id.progress_duration);
            statusBadge = itemView.findViewById(R.id.card_status_badge);
            timeRemainingBadge = itemView.findViewById(R.id.card_time_badge);
            viewListingButton = itemView.findViewById(R.id.btn_view_listing);
            // Image count views are optional - may not exist in layout
            imageCountCard = null; // Not in current layout
            imageCountText = null; // Not in current layout
            
            // Hide the view listing button - card itself is clickable
            if (viewListingButton != null) {
                viewListingButton.setVisibility(View.GONE);
            }
        }
    }
}
