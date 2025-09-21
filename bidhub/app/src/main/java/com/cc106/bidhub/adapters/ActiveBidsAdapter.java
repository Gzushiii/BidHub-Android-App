package com.cc106.bidhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
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
        private TextView itemTitleText;
        private TextView bidAmountText;
        private TextView currentBidText;
        private TextView timeRemainingText;
        private TextView bidStatusText;
        private ProgressBar bidProgressBar;
        private TextView progressText;
        private Button cancelBidButton;
        private View progressLayout;
        
        public ActiveBidViewHolder(@NonNull View itemView) {
            super(itemView);
            
            itemTitleText = itemView.findViewById(R.id.item_title_text);
            bidAmountText = itemView.findViewById(R.id.bid_amount_text);
            currentBidText = itemView.findViewById(R.id.current_bid_text);
            timeRemainingText = itemView.findViewById(R.id.time_remaining_text);
            bidStatusText = itemView.findViewById(R.id.bid_status_text);
            bidProgressBar = itemView.findViewById(R.id.bid_progress_bar);
            progressText = itemView.findViewById(R.id.progress_text);
            cancelBidButton = itemView.findViewById(R.id.cancel_bid_button);
            progressLayout = itemView.findViewById(R.id.progress_layout);
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (onBidClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onBidClickListener.onBidClick(bids.get(position));
                    }
                }
            });
            
            cancelBidButton.setOnClickListener(v -> {
                if (onCancelBidClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onCancelBidClickListener.onCancelBidClick(bids.get(position));
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
            
            // Set bid amount
            bidAmountText.setText(currencyFormat.format(bid.getAmount()));
            
            // Set current bid (from item)
            if (item != null) {
                currentBidText.setText(currencyFormat.format(item.getCurrentPrice()));
            } else {
                currentBidText.setText("N/A");
            }
            
            // Set time remaining
            setTimeRemaining(item);
            
            // Set bid status and progress
            setBidStatus(bid, item);
        }
        
        private void setTimeRemaining(Item item) {
            if (item != null && item.getEndDate() != null) {
                long timeRemaining = item.getTimeRemaining();
                if (timeRemaining > 0) {
                    int days = (int) (timeRemaining / (24 * 60 * 60 * 1000));
                    int hours = (int) ((timeRemaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000));
                    int minutes = (int) ((timeRemaining % (60 * 60 * 1000)) / (60 * 1000));
                    
                    if (days > 0) {
                        timeRemainingText.setText(String.format("%dd %dh", days, hours));
                    } else if (hours > 0) {
                        timeRemainingText.setText(String.format("%dh %dm", hours, minutes));
                    } else {
                        timeRemainingText.setText(String.format("%dm", minutes));
                    }
                    
                    // Set color based on urgency
                    if (timeRemaining < 60 * 60 * 1000) { // Less than 1 hour
                        timeRemainingText.setTextColor(context.getResources().getColor(R.color.error_red));
                    } else if (timeRemaining < 24 * 60 * 60 * 1000) { // Less than 1 day
                        timeRemainingText.setTextColor(context.getResources().getColor(R.color.warning_yellow));
                    } else {
                        timeRemainingText.setTextColor(context.getResources().getColor(R.color.text_primary));
                    }
                } else {
                    timeRemainingText.setText("Ended");
                    timeRemainingText.setTextColor(context.getResources().getColor(R.color.text_secondary));
                }
            } else {
                timeRemainingText.setText("N/A");
                timeRemainingText.setTextColor(context.getResources().getColor(R.color.text_secondary));
            }
        }
        
        private void setBidStatus(Bid bid, Item item) {
            if (item == null) {
                bidStatusText.setText("UNKNOWN");
                bidStatusText.setTextColor(context.getResources().getColor(R.color.text_secondary));
                progressLayout.setVisibility(View.GONE);
                cancelBidButton.setVisibility(View.GONE);
                return;
            }
            
            // Check if user is currently winning
            boolean isWinning = bid.getAmount() >= item.getCurrentPrice();
            boolean isHighest = bid.getAmount() == item.getCurrentPrice();
            
            if (isWinning) {
                if (isHighest) {
                    bidStatusText.setText("WINNING");
                    bidStatusText.setTextColor(context.getResources().getColor(R.color.accent_orange));
                    progressText.setText("You are currently winning this auction");
                    progressText.setTextColor(context.getResources().getColor(R.color.accent_orange));
                } else {
                    bidStatusText.setText("LEADING");
                    bidStatusText.setTextColor(context.getResources().getColor(R.color.success_green));
                    progressText.setText("You are leading this auction");
                    progressText.setTextColor(context.getResources().getColor(R.color.success_green));
                }
                
                // Set progress bar (simplified - in real app, this would be more sophisticated)
                bidProgressBar.setProgress(100);
                progressLayout.setVisibility(View.VISIBLE);
                
            } else {
                bidStatusText.setText("OUTBID");
                bidStatusText.setTextColor(context.getResources().getColor(R.color.error_red));
                progressText.setText("You have been outbid");
                progressText.setTextColor(context.getResources().getColor(R.color.error_red));
                
                // Set progress bar based on how close the bid is
                double percentage = (bid.getAmount() / item.getCurrentPrice()) * 100;
                bidProgressBar.setProgress((int) Math.min(percentage, 100));
                progressLayout.setVisibility(View.VISIBLE);
            }
            
            // Show cancel button only for active bids that can be cancelled
            if (bid.getStatus() == BidStatus.ACTIVE && bid.getStatus().canBeEdited()) {
                cancelBidButton.setVisibility(View.VISIBLE);
            } else {
                cancelBidButton.setVisibility(View.GONE);
            }
        }
    }
}
