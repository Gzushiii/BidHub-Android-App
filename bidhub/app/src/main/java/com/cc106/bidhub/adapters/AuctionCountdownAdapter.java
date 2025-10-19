package com.cc106.bidhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cc106.bidhub.R;
import com.cc106.bidhub.bidding.Bid;
import com.cc106.bidhub.bidding.BiddingEngine;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying auction countdown timers in RecyclerView
 */
public class AuctionCountdownAdapter extends RecyclerView.Adapter<AuctionCountdownAdapter.CountdownViewHolder> {
    
    private List<Item> items;
    private Context context;
    private OnAuctionClickListener onAuctionClickListener;
    private ItemManager itemManager;
    private BiddingEngine biddingEngine;
    private NumberFormat currencyFormat;
    private String userId;
    
    public interface OnAuctionClickListener {
        void onAuctionClick(Item item);
    }
    
    public AuctionCountdownAdapter(List<Item> items, OnAuctionClickListener auctionClickListener) {
        this.items = items;
        this.onAuctionClickListener = auctionClickListener;
        
        // Initialize formatters
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("PHP"));
    }
    
    @NonNull
    @Override
    public CountdownViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        itemManager = ItemManager.getInstance(context);
        biddingEngine = BiddingEngine.getInstance(context);
        
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_auction_countdown, parent, false);
        return new CountdownViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CountdownViewHolder holder, int position) {
        Item item = items.get(position);
        holder.bind(item);
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    public void updateItems(List<Item> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    class CountdownViewHolder extends RecyclerView.ViewHolder {
        private TextView itemTitleText;
        private TextView currentBidText;
        private TextView timeRemainingText;
        private TextView bidderCountText;
        private TextView statusText;
        private ProgressBar countdownProgressBar;
        private TextView myBidStatusText;
        
        public CountdownViewHolder(@NonNull View itemView) {
            super(itemView);
            
            itemTitleText = itemView.findViewById(R.id.item_title_text);
            currentBidText = itemView.findViewById(R.id.current_bid_text);
            timeRemainingText = itemView.findViewById(R.id.time_remaining_text);
            bidderCountText = itemView.findViewById(R.id.bidder_count_text);
            statusText = itemView.findViewById(R.id.status_text);
            countdownProgressBar = itemView.findViewById(R.id.countdown_progress_bar);
            myBidStatusText = itemView.findViewById(R.id.my_bid_status_text);
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (onAuctionClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onAuctionClickListener.onAuctionClick(items.get(position));
                    }
                }
            });
        }
        
        public void bind(Item item) {
            // Set item title
            itemTitleText.setText(item.getTitle());
            
            // Set current bid
            currentBidText.setText(currencyFormat.format(item.getCurrentPrice()));
            
            // Set bidder count
            bidderCountText.setText(String.valueOf(item.getBidCount()) + " bidders");
            
            // Set time remaining and status
            setTimeRemaining(item);
            
            // Set user's bid status
            setMyBidStatus(item);
        }
        
        private void setTimeRemaining(Item item) {
            long timeRemaining = item.getTimeRemaining();
            
            if (timeRemaining > 0) {
                // Calculate time components
                int days = (int) (timeRemaining / (24 * 60 * 60 * 1000));
                int hours = (int) ((timeRemaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000));
                int minutes = (int) ((timeRemaining % (60 * 60 * 1000)) / (60 * 1000));
                int seconds = (int) ((timeRemaining % (60 * 1000)) / 1000);
                
                // Format time string
                String timeString;
                if (days > 0) {
                    timeString = String.format("%dd %02dh %02dm", days, hours, minutes);
                } else if (hours > 0) {
                    timeString = String.format("%02dh %02dm %02ds", hours, minutes, seconds);
                } else if (minutes > 0) {
                    timeString = String.format("%02dm %02ds", minutes, seconds);
                } else {
                    timeString = String.format("%02ds", seconds);
                }
                
                timeRemainingText.setText(timeString);
                
                // Set color based on urgency
                if (timeRemaining < 60 * 60 * 1000) { // Less than 1 hour
                    timeRemainingText.setTextColor(context.getResources().getColor(R.color.error_red));
                    statusText.setText("ENDING SOON!");
                    statusText.setTextColor(context.getResources().getColor(R.color.error_red));
                } else if (timeRemaining < 24 * 60 * 60 * 1000) { // Less than 1 day
                    timeRemainingText.setTextColor(context.getResources().getColor(R.color.warning_yellow));
                    statusText.setText("Ending Today");
                    statusText.setTextColor(context.getResources().getColor(R.color.warning_yellow));
                } else {
                    timeRemainingText.setTextColor(context.getResources().getColor(R.color.text_primary));
                    statusText.setText("Active");
                    statusText.setTextColor(context.getResources().getColor(R.color.success_green));
                }
                
                // Set progress bar (simplified - in real app, this would be more sophisticated)
                long totalTime = item.getEndDate().getTime() - item.getCreatedAt().getTime();
                int progress = (int) ((totalTime - timeRemaining) * 100 / totalTime);
                countdownProgressBar.setProgress(Math.min(progress, 100));
                
            } else {
                timeRemainingText.setText("ENDED");
                timeRemainingText.setTextColor(context.getResources().getColor(R.color.text_secondary));
                statusText.setText("Auction Ended");
                statusText.setTextColor(context.getResources().getColor(R.color.text_secondary));
                countdownProgressBar.setProgress(100);
            }
        }
        
        private void setMyBidStatus(Item item) {
            if (userId == null) {
                myBidStatusText.setVisibility(View.GONE);
                return;
            }
            
            // Check if user has bid on this item
            List<Bid> userBids = biddingEngine.getUserBids(userId);
            Bid userBid = null;
            
            for (Bid bid : userBids) {
                if (bid.getItemId().equals(item.getItemId()) && bid.isActive()) {
                    userBid = bid;
                    break;
                }
            }
            
            if (userBid != null) {
                myBidStatusText.setVisibility(View.VISIBLE);
                
                // Check if user is currently winning
                if (userBid.getAmount() >= item.getCurrentPrice()) {
                    myBidStatusText.setText("You are winning!");
                    myBidStatusText.setTextColor(context.getResources().getColor(R.color.success_green));
                } else {
                    myBidStatusText.setText("You have been outbid");
                    myBidStatusText.setTextColor(context.getResources().getColor(R.color.error_red));
                }
            } else {
                myBidStatusText.setVisibility(View.GONE);
            }
        }
    }
}
