package com.cc106.bidhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
 * Adapter for displaying won auctions in RecyclerView
 */
public class WonAuctionsAdapter extends RecyclerView.Adapter<WonAuctionsAdapter.WonAuctionViewHolder> {
    
    private List<Bid> bids;
    private Context context;
    private OnAuctionClickListener onAuctionClickListener;
    private OnContactSellerClickListener onContactSellerClickListener;
    private ItemManager itemManager;
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;
    
    public interface OnAuctionClickListener {
        void onAuctionClick(Bid bid);
    }
    
    public interface OnContactSellerClickListener {
        void onContactSellerClick(Bid bid);
    }
    
    public WonAuctionsAdapter(List<Bid> bids, OnAuctionClickListener auctionClickListener, OnContactSellerClickListener contactClickListener) {
        this.bids = bids;
        this.onAuctionClickListener = auctionClickListener;
        this.onContactSellerClickListener = contactClickListener;
        
        // Initialize formatters
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("PHP"));
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public WonAuctionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        itemManager = ItemManager.getInstance(context);
        
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_won_auction, parent, false);
        return new WonAuctionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull WonAuctionViewHolder holder, int position) {
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
    
    class WonAuctionViewHolder extends RecyclerView.ViewHolder {
        private TextView itemTitleText;
        private TextView winDateText;
        private TextView winAmountText;
        private TextView sellerInfoText;
        private TextView statusText;
        private Button contactSellerButton;
        private Button viewItemButton;
        
        public WonAuctionViewHolder(@NonNull View itemView) {
            super(itemView);
            
            itemTitleText = itemView.findViewById(R.id.item_title_text);
            winDateText = itemView.findViewById(R.id.win_date_text);
            winAmountText = itemView.findViewById(R.id.win_amount_text);
            sellerInfoText = itemView.findViewById(R.id.seller_info_text);
            statusText = itemView.findViewById(R.id.status_text);
            contactSellerButton = itemView.findViewById(R.id.contact_seller_button);
            viewItemButton = itemView.findViewById(R.id.view_item_button);
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (onAuctionClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onAuctionClickListener.onAuctionClick(bids.get(position));
                    }
                }
            });
            
            contactSellerButton.setOnClickListener(v -> {
                if (onContactSellerClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onContactSellerClickListener.onContactSellerClick(bids.get(position));
                    }
                }
            });
            
            viewItemButton.setOnClickListener(v -> {
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
            
            // Set win date
            winDateText.setText("Won on " + dateFormat.format(bid.getPlacedAt()));
            
            // Set win amount
            winAmountText.setText(currencyFormat.format(bid.getAmount()));
            
            // Set seller info
            if (item != null) {
                sellerInfoText.setText("Seller: " + item.getSellerId()); // In real app, this would be seller name
            } else {
                sellerInfoText.setText("Seller: Unknown");
            }
            
            // Set status
            setStatus(bid, item);
        }
        
        private void setStatus(Bid bid, Item item) {
            // In a real app, this would check the actual status from database
            boolean hasContacted = hasContactedSeller(bid);
            boolean isDelivered = isDelivered(bid);
            
            String statusText;
            int statusColor;
            
            if (isDelivered) {
                statusText = "Delivered";
                statusColor = context.getResources().getColor(R.color.success_green);
            } else if (hasContacted) {
                statusText = "Contacted Seller";
                statusColor = context.getResources().getColor(R.color.primary_color);
            } else {
                statusText = "Pending Contact";
                statusColor = context.getResources().getColor(R.color.warning_yellow);
            }
            
            this.statusText.setText(statusText);
            this.statusText.setTextColor(statusColor);
            
            // Update contact button visibility
            if (hasContacted) {
                contactSellerButton.setText("View Messages");
                contactSellerButton.setEnabled(true);
            } else {
                contactSellerButton.setText("Contact Seller");
                contactSellerButton.setEnabled(true);
            }
        }
        
        private boolean hasContactedSeller(Bid bid) {
            // In a real app, this would check the contact status from database
            return false;
        }
        
        private boolean isDelivered(Bid bid) {
            // In a real app, this would check the delivery status from database
            return false;
        }
    }
}
