package com.cc106.bidhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import java.util.List;

/**
 * Adapter for displaying tags in a horizontal layout
 */
public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.TagViewHolder> {
    
    private List<String> tags;
    private OnTagRemoveListener onTagRemoveListener;
    
    public interface OnTagRemoveListener {
        void onTagRemove(int position, String tag);
    }
    
    public TagsAdapter(List<String> tags) {
        this.tags = tags;
    }
    
    public void setOnTagRemoveListener(OnTagRemoveListener listener) {
        this.onTagRemoveListener = listener;
    }
    
    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag_chip, parent, false);
        return new TagViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        String tag = tags.get(position);
        holder.tagText.setText(tag);
        
        holder.removeButton.setOnClickListener(v -> {
            if (onTagRemoveListener != null) {
                onTagRemoveListener.onTagRemove(position, tag);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return tags.size();
    }
    
    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
            notifyItemInserted(tags.size() - 1);
        }
    }
    
    public void removeTag(int position) {
        if (position >= 0 && position < tags.size()) {
            tags.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    public void updateTags(List<String> newTags) {
        this.tags = newTags;
        notifyDataSetChanged();
    }
    
    static class TagViewHolder extends RecyclerView.ViewHolder {
        TextView tagText;
        TextView removeButton;
        
        TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tagText = itemView.findViewById(R.id.tv_tag_text);
            removeButton = itemView.findViewById(R.id.tv_remove_tag);
        }
    }
}

