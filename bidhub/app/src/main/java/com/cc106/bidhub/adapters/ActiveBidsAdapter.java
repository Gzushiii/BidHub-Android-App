package com.cc106.bidhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cc106.bidhub.R;
import com.cc106.bidhub.bidding.Bid;
import com.cc106.bidhub.bidding.BidStatus;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying active bids in RecyclerView
 */
public class ActiveBidsAdapter extends RecyclerView.Adapter<ActiveBidsAdapter.ActiveBidViewHolder> {
    
    private List<Bid> bids;
    private Context context;
    private OnBidClickListener onBidClickListener;
    private OnCancelBidClickListener onCancelBidClickListener;
    private ItemManager itemManager;
    private NumberFormat currencyFormat;
    
    public interface OnBidClickListener {
        void onBidClick(Bid bid);
    }
    
    public interface OnCancelBidClickListener {
        void onCancelBidClick(Bid bid);
    }
    
    public ActiveBidsAdapter(List<Bid> bids, OnBidClickListener bidClickListener, OnCancelBidClickListener cancelClickListener) {
        this.bids = bids;
        this.onBidClickListener = bidClickListener;
        this.onCancelBidClickListener = cancelClickListener;
        
        // Initialize formatters
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("PHP"));
    }
    
    @NonNull
    @Override
    public ActiveBidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        itemManager = ItemManager.getInstance(context);
        
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_active_bid, parent, false);
        return new ActiveBidViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ActiveBidViewHolder holder, int position) {
        Bid bid = bids.get(position);
        holder.bind(bid);
    }
    
    @Override
    public int getItemCount() {
        return bids.size();
    }
    
    public void updateBids(List<Bid> newBids) {
        this.bids = newBids;
        notifyDataSetChanged();
    }
    
    class ActiveBidViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCurrentBidLabel;
        private TextView tvCurrentBidAmount;
        private TextView itemTitleText;
        private TextView timeRemainingText;
        private TextView bidStatusText;
        private com.google.android.material.button.MaterialButton btnBidNow;
        private android.widget.ImageView ivItemImage;
        
        public ActiveBidViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvCurrentBidLabel = itemView.findViewById(R.id.tv_current_bid_label);
            tvCurrentBidAmount = itemView.findViewById(R.id.tv_current_bid_amount);
            itemTitleText = itemView.findViewById(R.id.item_title_text);
            timeRemainingText = itemView.findViewById(R.id.time_remaining_text);
            bidStatusText = itemView.findViewById(R.id.bid_status_text);
            btnBidNow = itemView.findViewById(R.id.btn_bid_now);
            ivItemImage = itemView.findViewById(R.id.iv_item_image);
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (onBidClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onBidClickListener.onBidClick(bids.get(position));
                    }
                }
            });
            
            btnBidNow.setOnClickListener(v -> {
                if (onBidClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onBidClickListener.onBidClick(bids.get(position));
                    }
                }
            });
        }
        
        public void bind(Bid bid) {
            // Get item details
            Item item = itemManager.getItemById(bid.getItemId());
            
            // Set item title
            if (item != null) {
                itemTitleText.setText(item.getTitle());
            } else {
                itemTitleText.setText("Item not found");
            }
            
            // Set current bid (from item)
            if (item != null) {
                double currentBid = item.getCurrentPrice() > 0 ? item.getCurrentPrice() : item.getStartingPrice();
                tvCurrentBidAmount.setText(currencyFormat.format(currentBid));
            } else {
                tvCurrentBidAmount.setText("N/A");
            }
            
            // Set time remaining
            setTimeRemaining(item);
            
            // Set bid status
            setBidStatus(bid, item);
            
            // Load item image
            if (item != null && item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
                com.cc106.bidhub.utils.ImageLoader.loadImageWithErrorCallback(
                    context,
                    item.getImagePaths().get(0),
                    ivItemImage,
                    R.drawable.ic_image_placeholder
                );
            } else {
                ivItemImage.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
        
        private void setTimeRemaining(Item item) {
            if (item != null && item.getEndDate() != null) {
                long timeRemaining = item.getTimeRemaining();
                if (timeRemaining > 0) {
                    int hours = (int) ((timeRemaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000));
                    int minutes = (int) ((timeRemaining % (60 * 60 * 1000)) / (60 * 1000));
                    int seconds = (int) ((timeRemaining % (60 * 1000)) / 1000);
                    
                    String timeLeft = String.format("%02dh %02dm %02ds", hours, minutes, seconds);
                    timeRemainingText.setText(context.getString(R.string.time_left_label) + " " + timeLeft);
                    timeRemainingText.setTextColor(context.getResources().getColor(R.color.header_text_secondary));
                } else {
                    timeRemainingText.setText(context.getString(R.string.time_left_label) + " Ended");
                    timeRemainingText.setTextColor(context.getResources().getColor(R.color.header_text_secondary));
                }
            } else {
                timeRemainingText.setText(context.getString(R.string.time_left_label) + " N/A");
                timeRemainingText.setTextColor(context.getResources().getColor(R.color.header_text_secondary));
            }
        }
        
        private void setBidStatus(Bid bid, Item item) {
            if (item == null) {
                bidStatusText.setVisibility(View.GONE);
                return;
            }
            
            // Check if user is currently winning
            boolean isWinning = bid.getAmount() >= item.getCurrentPrice();
            boolean isHighest = bid.getAmount() == item.getCurrentPrice();
            
            if (isWinning && isHighest) {
                bidStatusText.setText("WINNING");
                bidStatusText.setTextColor(context.getResources().getColor(R.color.success));
                bidStatusText.setVisibility(View.VISIBLE);
            } else if (isWinning) {
                bidStatusText.setText("LEADING");
                bidStatusText.setTextColor(context.getResources().getColor(R.color.success));
                bidStatusText.setVisibility(View.VISIBLE);
            } else {
                bidStatusText.setText("OUTBID");
                bidStatusText.setTextColor(context.getResources().getColor(R.color.error));
                bidStatusText.setVisibility(View.VISIBLE);
            }
        }
    }
}
