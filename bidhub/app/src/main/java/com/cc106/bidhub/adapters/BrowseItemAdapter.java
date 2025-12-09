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
        private TextView itemStatusBadge;

        public BrowseItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemTitle = itemView.findViewById(R.id.itemTitle);
            itemBid = itemView.findViewById(R.id.itemBid);
            itemTimeLeft = itemView.findViewById(R.id.itemTimeLeft);
            itemStatusBadge = itemView.findViewById(R.id.itemStatusBadge);
        }

        public void bind(BrowseItem item) {
            itemTitle.setText(item.getTitle());
            itemBid.setText(item.getCurrentBid());
            itemTimeLeft.setText(item.getTimeLeft());
            
            // Handle status badge
            if (itemStatusBadge != null) {
                if (item.isBuyNow()) {
                    itemStatusBadge.setText("Buy Now");
                    itemStatusBadge.setVisibility(View.VISIBLE);
                    itemStatusBadge.setBackgroundColor(itemView.getContext().getColor(R.color.success));
                } else if (item.getStatus() != null && item.getStatus().equals("ending_soon")) {
                    itemStatusBadge.setText("Ending Soon");
                    itemStatusBadge.setVisibility(View.VISIBLE);
                    itemStatusBadge.setBackgroundColor(itemView.getContext().getColor(R.color.warning));
                } else {
                    itemStatusBadge.setVisibility(View.GONE);
                }
            }

            // Load image - use user image if available, otherwise fallback to sample images
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                // Load user-uploaded image using Glide
                com.cc106.bidhub.utils.ImageLoader.loadImageWithErrorCallback(
                    itemImage.getContext(),
                    item.getImageUrl(),
                    itemImage,
                    new com.cc106.bidhub.utils.ImageLoader.ImageLoadErrorCallback() {
                        @Override
                        public void onError(String errorMessage) {
                            com.cc106.bidhub.utils.ErrorHandler.handleImageError(
                                itemImage.getContext(),
                                item.getImageUrl(),
                                null,
                                "BrowseItemAdapter image load"
                            );
                        }
                    }
                );
            } else {
                // Fallback to sample images based on item type
                int sampleImageRes = getSampleImageForTitle(item.getTitle());
                com.cc106.bidhub.utils.ImageLoader.loadSampleImage(
                    itemImage.getContext(),
                    sampleImageRes,
                    itemImage
                );
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
                try {
                    if (clickListener != null) {
                        clickListener.onItemClick(item);
                    } else {
                        com.cc106.bidhub.utils.ErrorHandler.logWarning(
                            "BrowseItemAdapter", 
                            "Click listener is null",
                            String.format("ItemID: %s, Title: %s", 
                                item.getItemId(), item.getTitle())
                        );
                    }
                } catch (Exception e) {
                    com.cc106.bidhub.utils.ErrorHandler.handleAdapterError(
                        itemView.getContext(),
                        "BrowseItemAdapter",
                        "item click",
                        e,
                        String.format("ItemID: %s, Title: %s", 
                            item.getItemId(), item.getTitle())
                    );
                }
            });
        }
        
        /**
         * Get sample image resource based on item title
         */
        private int getSampleImageForTitle(String title) {
            if (title == null) {
                return R.drawable.sample_auction_1;
            }
            
            String lowerTitle = title.toLowerCase();
            if (lowerTitle.contains("camera")) {
                return R.drawable.sample_camera;
            } else if (lowerTitle.contains("handbag")) {
                return R.drawable.sample_handbag;
            } else if (lowerTitle.contains("sofa")) {
                return R.drawable.sample_sofa;
            } else if (lowerTitle.contains("coin")) {
                return R.drawable.sample_coin;
            } else {
                return R.drawable.sample_auction_1;
            }
        }
    }
}
