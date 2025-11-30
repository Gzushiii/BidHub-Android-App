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
        private TextView tvCurrentBidLabel;
        private TextView tvCurrentBidAmount;
        private TextView tvItemName;
        private TextView tvTimeLeft;
        private com.google.android.material.button.MaterialButton btnBidNow;
        private android.widget.ImageView ivItemImage;
        
        public ActiveAuctionViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvCurrentBidLabel = itemView.findViewById(R.id.tv_current_bid_label);
            tvCurrentBidAmount = itemView.findViewById(R.id.tv_current_bid_amount);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvTimeLeft = itemView.findViewById(R.id.tv_time_left);
            btnBidNow = itemView.findViewById(R.id.btn_bid_now);
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
            
            btnBidNow.setOnClickListener(v -> {
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
            
            // Set current bid
            double currentBid = item.getCurrentPrice() > 0 ? item.getCurrentPrice() : item.getStartingPrice();
            tvCurrentBidAmount.setText(currencyFormat.format(currentBid));
            
            // Set item name
            tvItemName.setText(item.getTitle());
            
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
            
            // Load image
            if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
                ImageLoader.loadImageWithErrorCallback(
                    context,
                    item.getImagePaths().get(0),
                    ivItemImage,
                    R.drawable.ic_image_placeholder
                );
            } else {
                ivItemImage.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
    }
}

