package com.cc106.bidhub.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cc106.bidhub.ItemDetailActivity;
import com.cc106.bidhub.R;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.utils.ImageLoader;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying active auctions in RecyclerView
 * Shows all active auctions (not just user's bids)
 */
public class ActiveAuctionsAdapter extends RecyclerView.Adapter<ActiveAuctionsAdapter.ActiveAuctionViewHolder> {
    
    private List<Item> items;
    private Context context;
    private OnAuctionClickListener onAuctionClickListener;
    private NumberFormat currencyFormat;
    
    public interface OnAuctionClickListener {
        void onAuctionClick(Item item);
    }
    
    public ActiveAuctionsAdapter(List<Item> items, OnAuctionClickListener clickListener) {
        this.items = items;
        this.onAuctionClickListener = clickListener;
        
        // Initialize currency formatter
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("PHP"));
    }
    
    @NonNull
    @Override
    public ActiveAuctionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_active_auction, parent, false);
        return new ActiveAuctionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ActiveAuctionViewHolder holder, int position) {
        Item item = items.get(position);
        holder.bind(item);
    }
    
    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
    
    public void updateItems(List<Item> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }
    
    class ActiveAuctionViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCurrentBidAmount;
        private TextView tvItemName;
        private TextView tvSellerName;
        private TextView tvBidCount;
        private TextView tvTimeLeft;
        private android.widget.ImageView ivItemImage;
        
        public ActiveAuctionViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvCurrentBidAmount = itemView.findViewById(R.id.tv_current_bid_amount);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvSellerName = itemView.findViewById(R.id.tv_seller_name);
            tvBidCount = itemView.findViewById(R.id.tv_bid_count);
            tvTimeLeft = itemView.findViewById(R.id.tv_time_left);
            ivItemImage = itemView.findViewById(R.id.iv_item_image);
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (onAuctionClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && items != null) {
                        onAuctionClickListener.onAuctionClick(items.get(position));
                    }
                }
            });
            
        }
        
        public void bind(Item item) {
            if (item == null) {
                return;
            }
            
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
            tvCurrentBidAmount.setText(currencyFormat.format(displayPrice));
            
            // Set item name
            tvItemName.setText(item.getTitle() != null ? item.getTitle() : "Untitled Item");
            
            // FIX: Display seller name
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
            if (tvSellerName != null) {
                tvSellerName.setText("by " + sellerName);
            }
            
            // FIX: Display bid count (bidCount already declared above)
            if (tvBidCount != null) {
                if (bidCount > 0) {
                    tvBidCount.setText(bidCount + (bidCount == 1 ? " bid" : " bids"));
                } else {
                    tvBidCount.setText("No bids yet");
                }
            }
            
            // Set time left
            if (item.getEndDate() != null) {
                long timeRemaining = item.getTimeRemaining();
                if (timeRemaining > 0) {
                    int hours = (int) ((timeRemaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000));
                    int minutes = (int) ((timeRemaining % (60 * 60 * 1000)) / (60 * 1000));
                    int seconds = (int) ((timeRemaining % (60 * 1000)) / 1000);
                    
                    String timeLeft = String.format("%02dh %02dm %02ds", hours, minutes, seconds);
                    tvTimeLeft.setText(context.getString(R.string.time_left_label) + " " + timeLeft);
                } else {
                    tvTimeLeft.setText(context.getString(R.string.time_left_label) + " Ended");
                }
            } else {
                tvTimeLeft.setText(context.getString(R.string.time_left_label) + " N/A");
            }
            
            // FIX: Load image with proper fallback handling
            if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
                String firstImagePath = item.getImagePaths().get(0);
                // Validate image URL before loading
                if (firstImagePath != null && !firstImagePath.isEmpty() && !firstImagePath.equals("null")) {
                    ImageLoader.loadImageWithErrorCallback(
                        context,
                        firstImagePath,
                        ivItemImage,
                        new ImageLoader.ImageLoadErrorCallback() {
                            @Override
                            public void onError(String errorMessage) {
                                android.util.Log.w("ActiveAuctionsAdapter", "Failed to load image: " + firstImagePath);
                                // Fallback to placeholder on error
                                ImageLoader.loadPlaceholder(context, ivItemImage);
                            }
                        }
                    );
                } else {
                    // Invalid image path, use placeholder
                    ImageLoader.loadPlaceholder(context, ivItemImage);
                }
            } else {
                // No images uploaded, use placeholder
                ImageLoader.loadPlaceholder(context, ivItemImage);
            }
        }
    }
}

