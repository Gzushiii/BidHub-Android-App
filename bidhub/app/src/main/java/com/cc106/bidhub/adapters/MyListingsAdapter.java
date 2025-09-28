package com.cc106.bidhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import com.cc106.bidhub.items.Item;
import com.cc106.bidhub.items.ItemStatus;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class MyListingsAdapter extends RecyclerView.Adapter<MyListingsAdapter.ViewHolder> {

    public interface OnListingActionListener {
        void onViewBids(Item item);
        void onEditListing(Item item);
        void onMarkAsSold(Item item);
        void onItemClick(Item item);
    }

    private List<Item> listings;
    private OnListingActionListener listener;
    private Context context;

    public MyListingsAdapter(List<Item> listings, Context context) {
        this.listings = listings;
        this.context = context;
    }

    public void setOnListingActionListener(OnListingActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_listing_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = listings.get(position);
        if (item == null) return;

        // Set item details
        holder.tvItemTitle.setText(item.getTitle() != null ? item.getTitle() : "Untitled Item");
        
        // Set item image (use placeholder for now)
        holder.ivItemImage.setImageResource(R.drawable.placeholder);

        // Determine item status and set appropriate UI
        String status = getStatusString(item.getStatus());
        holder.tvStatus.setText(status);
        
        // Set status color
        int statusColor = getStatusColor(status);
        holder.tvStatus.setTextColor(context.getResources().getColor(statusColor));

        // Set price information based on status
        String priceInfo = getPriceInfo(item, status);
        holder.tvPriceInfo.setText(priceInfo);

        // Set time information
        String timeInfo = getTimeInfo(item, status);
        holder.tvTimeInfo.setText(timeInfo);
        holder.tvTimeInfo.setVisibility(timeInfo != null ? View.VISIBLE : View.GONE);

        // Configure action buttons based on status
        setupActionButtons(holder, item, status);
    }

    private String getStatusString(ItemStatus status) {
        if (status == null) return "Active";
        
        switch (status) {
            case ACTIVE:
                return "Active";
            case PAUSED:
                return "Pending";
            case SOLD:
                return "Sold";
            case DRAFT:
                return "Draft";
            case ENDED:
                return "Ended";
            case CANCELLED:
                return "Cancelled";
            default:
                return "Active";
        }
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "Active":
                return R.color.primary;
            case "Pending":
                return R.color.warning;
            case "Sold":
                return R.color.success;
            case "Draft":
                return R.color.text_secondary;
            default:
                return R.color.text_primary;
        }
    }

    private String getPriceInfo(Item item, String status) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        currencyFormat.setCurrency(Currency.getInstance("USD"));

        switch (status) {
            case "Active":
                return "Current Bid: " + currencyFormat.format(item.getCurrentPrice());
            case "Pending":
                return "Final Price: " + currencyFormat.format(item.getCurrentPrice());
            case "Sold":
                return "Sold for: " + currencyFormat.format(item.getCurrentPrice());
            case "Draft":
                return "Last edited: 2 days ago";
            default:
                return "Price: " + currencyFormat.format(item.getCurrentPrice());
        }
    }

    private String getTimeInfo(Item item, String status) {
        switch (status) {
            case "Active":
                return "12h 45m left";
            case "Pending":
            case "Sold":
            case "Draft":
            default:
                return null;
        }
    }

    private void setupActionButtons(ViewHolder holder, Item item, String status) {
        // Hide all buttons first
        holder.btnAction1.setVisibility(View.GONE);
        holder.btnAction2.setVisibility(View.GONE);

        switch (status) {
            case "Active":
                holder.btnAction1.setVisibility(View.VISIBLE);
                holder.btnAction1.setText("View Bids");
                holder.btnAction1.setBackgroundResource(R.drawable.button_primary);
                holder.btnAction1.setTextColor(context.getResources().getColor(R.color.white));
                holder.btnAction1.setOnClickListener(v -> {
                    if (listener != null) listener.onViewBids(item);
                });

                holder.btnAction2.setVisibility(View.VISIBLE);
                holder.btnAction2.setText("Edit");
                holder.btnAction2.setBackgroundResource(R.drawable.button_secondary);
                holder.btnAction2.setTextColor(context.getResources().getColor(R.color.text_primary));
                holder.btnAction2.setOnClickListener(v -> {
                    if (listener != null) listener.onEditListing(item);
                });
                break;

            case "Pending":
                holder.btnAction1.setVisibility(View.VISIBLE);
                holder.btnAction1.setText("Mark as Sold");
                holder.btnAction1.setBackgroundResource(R.drawable.button_success_light);
                holder.btnAction1.setTextColor(context.getResources().getColor(R.color.success));
                holder.btnAction1.setOnClickListener(v -> {
                    if (listener != null) listener.onMarkAsSold(item);
                });
                break;

            case "Draft":
                holder.btnAction1.setVisibility(View.VISIBLE);
                holder.btnAction1.setText("Edit Listing");
                holder.btnAction1.setBackgroundResource(R.drawable.button_secondary);
                holder.btnAction1.setTextColor(context.getResources().getColor(R.color.text_primary));
                holder.btnAction1.setOnClickListener(v -> {
                    if (listener != null) listener.onEditListing(item);
                });
                break;

            case "Sold":
                // No action buttons for sold items
                break;
        }
    }

    @Override
    public int getItemCount() {
        return listings != null ? listings.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage;
        TextView tvStatus, tvItemTitle, tvPriceInfo, tvTimeInfo;
        Button btnAction1, btnAction2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.iv_item_image);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvItemTitle = itemView.findViewById(R.id.tv_item_title);
            tvPriceInfo = itemView.findViewById(R.id.tv_price_info);
            tvTimeInfo = itemView.findViewById(R.id.tv_time_info);
            btnAction1 = itemView.findViewById(R.id.btn_action_1);
            btnAction2 = itemView.findViewById(R.id.btn_action_2);

            // Set click listener for the entire card
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(listings.get(position));
                }
            });
        }
    }
}
