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
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying lost auctions in RecyclerView
 */
public class LostAuctionsAdapter extends RecyclerView.Adapter<LostAuctionsAdapter.LostAuctionViewHolder> {
    
    private List<Bid> bids;
    private Context context;
    private OnAuctionClickListener onAuctionClickListener;
    private ItemManager itemManager;
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;
    
    public interface OnAuctionClickListener {
        void onAuctionClick(Bid bid);
    }
    
    public LostAuctionsAdapter(List<Bid> bids, OnAuctionClickListener auctionClickListener) {
        this.bids = bids;
        this.onAuctionClickListener = auctionClickListener;
        
        // Initialize formatters
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("PHP"));
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public LostAuctionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        itemManager = ItemManager.getInstance(context);
        
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_lost_auction, parent, false);
        return new LostAuctionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull LostAuctionViewHolder holder, int position) {
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
    
    class LostAuctionViewHolder extends RecyclerView.ViewHolder {
        private TextView itemTitleText;
        private TextView lostDateText;
        private TextView bidAmountText;
        private TextView winningBidText;
        private TextView differenceText;
        private TextView sellerInfoText;
        private TextView statusText;
        
        public LostAuctionViewHolder(@NonNull View itemView) {
            super(itemView);
            
            itemTitleText = itemView.findViewById(R.id.item_title_text);
            lostDateText = itemView.findViewById(R.id.lost_date_text);
            bidAmountText = itemView.findViewById(R.id.bid_amount_text);
            winningBidText = itemView.findViewById(R.id.winning_bid_text);
            differenceText = itemView.findViewById(R.id.difference_text);
            sellerInfoText = itemView.findViewById(R.id.seller_info_text);
            statusText = itemView.findViewById(R.id.status_text);
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (onAuctionClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onAuctionClickListener.onAuctionClick(bids.get(position));
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
            
            // Set lost date
            lostDateText.setText("Lost on " + dateFormat.format(bid.getPlacedAt()));
            
            // Set bid amount
            bidAmountText.setText(currencyFormat.format(bid.getAmount()));
            
            // Set winning bid and difference
            if (item != null) {
                double winningBid = item.getCurrentPrice();
                double difference = winningBid - bid.getAmount();
                
                winningBidText.setText(currencyFormat.format(winningBid));
                
                if (difference > 0) {
                    differenceText.setText("Lost by " + currencyFormat.format(difference));
                    differenceText.setTextColor(context.getResources().getColor(R.color.error_red));
                } else {
                    differenceText.setText("Exact match");
                    differenceText.setTextColor(context.getResources().getColor(R.color.text_secondary));
                }
                
                // Set seller info
                sellerInfoText.setText("Seller: " + item.getSellerId()); // In real app, this would be seller name
            } else {
                winningBidText.setText("N/A");
                differenceText.setText("Unknown");
                sellerInfoText.setText("Seller: Unknown");
            }
            
            // Set status
            setStatus(bid, item);
        }
        
        private void setStatus(Bid bid, Item item) {
            String statusText;
            int statusColor;
            
            if (item != null) {
                double winningBid = item.getCurrentPrice();
                double difference = winningBid - bid.getAmount();
                double percentage = (bid.getAmount() / winningBid) * 100;
                
                if (percentage >= 95) {
                    statusText = "Very Close";
                    statusColor = context.getResources().getColor(R.color.warning_yellow);
                } else if (percentage >= 90) {
                    statusText = "Close Call";
                    statusColor = context.getResources().getColor(R.color.accent_orange);
                } else if (percentage >= 80) {
                    statusText = "Decent Try";
                    statusColor = context.getResources().getColor(R.color.primary_color);
                } else {
                    statusText = "Outbid";
                    statusColor = context.getResources().getColor(R.color.error_red);
                }
            } else {
                statusText = "Unknown";
                statusColor = context.getResources().getColor(R.color.text_secondary);
            }
            
            this.statusText.setText(statusText);
            this.statusText.setTextColor(statusColor);
        }
    }
}
