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
            holder.sellerText.setText("by " + (item.getSellerName() != null ? item.getSellerName() : "Unknown"));
            holder.bidCountText.setText(item.getBidCount() + " bids");
        
        // Set time remaining
        if (item.getEndDate() != null) {
            long timeRemaining = item.getEndDate().getTime() - System.currentTimeMillis();
            if (timeRemaining > 0) {
                long days = timeRemaining / (1000 * 60 * 60 * 24);
                long hours = (timeRemaining % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
                
                if (days > 0) {
                    holder.timeRemainingText.setText(days + "d " + hours + "h left");
                } else {
                    holder.timeRemainingText.setText(hours + "h left");
                }
            } else {
                holder.timeRemainingText.setText("Ended");
            }
        } else {
            holder.timeRemainingText.setText("No end date");
        }
        
        // Set item image - use placeholder if no images uploaded
        if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
            // TODO: Load actual image from path when image loading is implemented
            // For now, use placeholder even if images exist
            holder.itemImage.setImageResource(R.drawable.placeholder);
        } else {
            // No images uploaded, use placeholder
            holder.itemImage.setImageResource(R.drawable.placeholder);
        }
        
        // Set featured/trending indicators
        if (item.isFeatured()) {
            holder.featuredBadge.setVisibility(View.VISIBLE);
            holder.featuredBadge.setText("FEATURED");
        } else {
            holder.featuredBadge.setVisibility(View.GONE);
        }
        
        if (item.isTrending()) {
            holder.trendingBadge.setVisibility(View.VISIBLE);
            holder.trendingBadge.setText("TRENDING");
        } else {
            holder.trendingBadge.setVisibility(View.GONE);
        }
        
        // Set DRAFT status indicator
        if (item.getStatus() != null && item.getStatus().toString().equals("DRAFT")) {
            holder.draftBadge.setVisibility(View.VISIBLE);
            holder.draftBadge.setText("PENDING UPLOAD");
        } else {
            holder.draftBadge.setVisibility(View.GONE);
        }
        
            // Set click listener with error handling
            holder.itemView.setOnClickListener(v -> {
                try {
                    if (onItemClickListener != null && item != null) {
                        onItemClickListener.onItemClick(item);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Log error but don't crash the app
                    android.util.Log.e("ItemCardAdapter", "Error handling item click", e);
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("ItemCardAdapter", "Error binding view holder", e);
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
        TextView featuredBadge;
        TextView trendingBadge;
        TextView draftBadge;
        
        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.iv_item_image);
            titleText = itemView.findViewById(R.id.tv_item_title);
            priceText = itemView.findViewById(R.id.tv_item_price);
            sellerText = itemView.findViewById(R.id.tv_seller_name);
            bidCountText = itemView.findViewById(R.id.tv_bid_count);
            timeRemainingText = itemView.findViewById(R.id.tv_time_remaining);
            featuredBadge = itemView.findViewById(R.id.tv_featured_badge);
            trendingBadge = itemView.findViewById(R.id.tv_trending_badge);
            draftBadge = itemView.findViewById(R.id.tv_draft_badge);
        }
    }
}
