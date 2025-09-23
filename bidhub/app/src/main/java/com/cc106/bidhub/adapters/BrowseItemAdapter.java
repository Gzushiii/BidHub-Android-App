package com.cc106.bidhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import com.cc106.bidhub.models.BrowseItem;

import java.util.List;

public class BrowseItemAdapter extends RecyclerView.Adapter<BrowseItemAdapter.BrowseItemViewHolder> {

    private List<BrowseItem> browseItems;
    private OnItemClickListener clickListener;

    public interface OnItemClickListener {
        void onItemClick(BrowseItem item);
    }

    public BrowseItemAdapter(List<BrowseItem> browseItems, OnItemClickListener clickListener) {
        this.browseItems = browseItems;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public BrowseItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_browse, parent, false);
        return new BrowseItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BrowseItemViewHolder holder, int position) {
        BrowseItem item = browseItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return browseItems.size();
    }

    public void updateItems(List<BrowseItem> newItems) {
        this.browseItems = newItems;
        notifyDataSetChanged();
    }

    class BrowseItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView itemImage;
        private TextView itemTitle;
        private TextView itemBid;
        private TextView itemTimeLeft;

        public BrowseItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemTitle = itemView.findViewById(R.id.itemTitle);
            itemBid = itemView.findViewById(R.id.itemBid);
            itemTimeLeft = itemView.findViewById(R.id.itemTimeLeft);
        }

        public void bind(BrowseItem item) {
            itemTitle.setText(item.getTitle());
            itemBid.setText(item.getCurrentBid());
            itemTimeLeft.setText(item.getTimeLeft());

            // Set image based on item type (for now using sample images)
            if (item.getTitle().toLowerCase().contains("camera")) {
                itemImage.setImageResource(R.drawable.sample_camera);
            } else if (item.getTitle().toLowerCase().contains("handbag")) {
                itemImage.setImageResource(R.drawable.sample_handbag);
            } else if (item.getTitle().toLowerCase().contains("sofa")) {
                itemImage.setImageResource(R.drawable.sample_sofa);
            } else if (item.getTitle().toLowerCase().contains("coin")) {
                itemImage.setImageResource(R.drawable.sample_coin);
            } else {
                itemImage.setImageResource(R.drawable.sample_auction_1);
            }

            // Set time left color based on urgency
            if (item.getTimeLeft().toLowerCase().contains("ending soon")) {
                itemTimeLeft.setTextColor(itemView.getContext().getColor(R.color.error_red));
            } else if (item.getTimeLeft().toLowerCase().contains("buy it now")) {
                itemTimeLeft.setTextColor(itemView.getContext().getColor(R.color.success_green));
            } else {
                itemTimeLeft.setTextColor(itemView.getContext().getColor(R.color.error_red));
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(item);
                }
            });
        }
    }
}
