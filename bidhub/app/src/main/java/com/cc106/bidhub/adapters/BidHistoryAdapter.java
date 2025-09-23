package com.cc106.bidhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import com.cc106.bidhub.bidding.Bid;

import java.util.List;

public class BidHistoryAdapter extends RecyclerView.Adapter<BidHistoryAdapter.BidHistoryViewHolder> {

    private List<Bid> bidHistoryList;
    private OnBidItemClickListener clickListener;

    public interface OnBidItemClickListener {
        void onBidItemClick(Bid bid);
    }

    public BidHistoryAdapter(List<Bid> bidHistoryList, OnBidItemClickListener clickListener) {
        this.bidHistoryList = bidHistoryList;
        this.clickListener = clickListener;
    }

    public void updateBids(List<Bid> newBids) {
        this.bidHistoryList = newBids;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BidHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bid_history, parent, false);
        return new BidHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BidHistoryViewHolder holder, int position) {
        Bid bid = bidHistoryList.get(position);
        holder.bind(bid);
    }

    @Override
    public int getItemCount() {
        return bidHistoryList.size();
    }

    static class BidHistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvBidderName, tvBidTime, tvBidAmount;

        public BidHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBidderName = itemView.findViewById(R.id.tv_bidder_name);
            tvBidTime = itemView.findViewById(R.id.tv_bid_time);
            tvBidAmount = itemView.findViewById(R.id.tv_bid_amount);
        }

        public void bind(Bid bid) {
            tvBidderName.setText(bid.getBidderAlias());
            tvBidTime.setText(formatBidTime(bid.getPlacedAt()));
            tvBidAmount.setText("$" + String.format("%.0f", bid.getAmount()));
        }

        private String formatBidTime(java.util.Date date) {
            if (date == null) return "Unknown time";
            
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
            return timeFormat.format(date);
        }
    }
}