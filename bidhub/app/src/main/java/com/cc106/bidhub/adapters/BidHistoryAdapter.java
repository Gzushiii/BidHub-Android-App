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
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying bid history in RecyclerView
 */
public class BidHistoryAdapter extends RecyclerView.Adapter<BidHistoryAdapter.BidViewHolder> {
    
    private List<Bid> bids;
    private Context context;
    private OnBidClickListener onBidClickListener;
    private ItemManager itemManager;
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;
    
    public interface OnBidClickListener {
        void onBidClick(Bid bid);
    }
    
    public BidHistoryAdapter(List<Bid> bids, OnBidClickListener listener) {
        this.bids = bids;
        this.onBidClickListener = listener;
        
        // Initialize formatters
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("PHP"));
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public BidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        itemManager = ItemManager.getInstance(context);
        
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_bid_history, parent, false);
        return new BidViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull BidViewHolder holder, int position) {
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
    
    class BidViewHolder extends RecyclerView.ViewHolder {
        private TextView itemTitleText;
        private TextView bidDateText;
        private TextView bidStatusText;
        private TextView bidAmountText;
        private TextView currentBidText;
        private TextView timeRemainingText;
        private View progressLayout;
        private TextView progressText;
        
        public BidViewHolder(@NonNull View itemView) {
            super(itemView);
            
            itemTitleText = itemView.findViewById(R.id.item_title_text);
            bidDateText = itemView.findViewById(R.id.bid_date_text);
            bidStatusText = itemView.findViewById(R.id.bid_status_text);
            bidAmountText = itemView.findViewById(R.id.bid_amount_text);
            currentBidText = itemView.findViewById(R.id.current_bid_text);
            timeRemainingText = itemView.findViewById(R.id.time_remaining_text);
            progressLayout = itemView.findViewById(R.id.progress_layout);
            progressText = itemView.findViewById(R.id.progress_text);
            
            // Set click listener
            itemView.setOnClickListener(v -> {
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
            
            // Set bid date
            bidDateText.setText(dateFormat.format(bid.getPlacedAt()));
            
            // Set bid amount
            bidAmountText.setText(currencyFormat.format(bid.getAmount()));
            
            // Set current bid (from item)
            if (item != null) {
                currentBidText.setText(currencyFormat.format(item.getCurrentPrice()));
            } else {
                currentBidText.setText("N/A");
            }
            
            // Set status
            setBidStatus(bid);
            
            // Set time remaining
            setTimeRemaining(item);
            
            // Set progress indicator for active bids
            setProgressIndicator(bid, item);
        }
        
        private void setBidStatus(Bid bid) {
            BidStatus status = bid.getStatus();
            String statusText;
            int statusColor;
            int statusBackground;
            
            switch (status) {
                case ACTIVE:
                    statusText = "ACTIVE";
                    statusColor = context.getResources().getColor(R.color.success_green);
                    statusBackground = R.drawable.status_badge_active;
                    break;
                case WINNING:
                    statusText = "WINNING";
                    statusColor = context.getResources().getColor(R.color.accent_orange);
                    statusBackground = R.drawable.status_badge_winning;
                    break;
                case OUTBID:
                    statusText = "OUTBID";
                    statusColor = context.getResources().getColor(R.color.error_red);
                    statusBackground = R.drawable.status_badge_outbid;
                    break;
                case CANCELLED:
                    statusText = "CANCELLED";
                    statusColor = context.getResources().getColor(R.color.text_secondary);
                    statusBackground = R.drawable.status_badge_cancelled;
                    break;
                default:
                    statusText = "PENDING";
                    statusColor = context.getResources().getColor(R.color.warning_yellow);
                    statusBackground = R.drawable.status_badge_pending;
                    break;
            }
            
            bidStatusText.setText(statusText);
            bidStatusText.setTextColor(statusColor);
            bidStatusText.setBackgroundResource(statusBackground);
        }
        
        private void setTimeRemaining(Item item) {
            if (item != null && item.getEndDate() != null) {
                long timeRemaining = item.getTimeRemaining();
                if (timeRemaining > 0) {
                    int days = (int) (timeRemaining / (24 * 60 * 60 * 1000));
                    int hours = (int) ((timeRemaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000));
                    
                    if (days > 0) {
                        timeRemainingText.setText(String.format("%dd %dh", days, hours));
                    } else {
                        timeRemainingText.setText(String.format("%dh", hours));
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
        
        private void setProgressIndicator(Bid bid, Item item) {
            if (bid.isActive() && item != null) {
                progressLayout.setVisibility(View.VISIBLE);
                
                // Check if user is currently winning
                if (bid.getAmount() >= item.getCurrentPrice()) {
                    progressText.setText("You are currently winning");
                    progressText.setTextColor(context.getResources().getColor(R.color.success_green));
                } else {
                    progressText.setText("You have been outbid");
                    progressText.setTextColor(context.getResources().getColor(R.color.error_red));
                }
            } else {
                progressLayout.setVisibility(View.GONE);
            }
        }
    }
}
