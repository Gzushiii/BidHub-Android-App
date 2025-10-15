package com.cc106.bidhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import com.cc106.bidhub.models.FAQItem;
import java.util.List;

public class FAQAdapter extends RecyclerView.Adapter<FAQAdapter.FAQViewHolder> {
    
    private List<FAQItem> faqItems;
    
    public FAQAdapter(List<FAQItem> faqItems) {
        this.faqItems = faqItems;
    }
    
    @NonNull
    @Override
    public FAQViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faq, parent, false);
        return new FAQViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FAQViewHolder holder, int position) {
        FAQItem faqItem = faqItems.get(position);
        holder.bind(faqItem);
    }
    
    @Override
    public int getItemCount() {
        return faqItems.size();
    }
    
    public static class FAQViewHolder extends RecyclerView.ViewHolder {
        private TextView tvQuestion;
        private TextView tvAnswer;
        private TextView tvExpandIcon;
        
        public FAQViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tv_question);
            tvAnswer = itemView.findViewById(R.id.tv_answer);
            tvExpandIcon = itemView.findViewById(R.id.tv_expand_icon);
        }
        
        public void bind(FAQItem faqItem) {
            tvQuestion.setText(faqItem.getQuestion());
            tvAnswer.setText(faqItem.getAnswer());
            
            // Set visibility based on expanded state
            if (faqItem.isExpanded()) {
                tvAnswer.setVisibility(View.VISIBLE);
                tvExpandIcon.setText("−");
            } else {
                tvAnswer.setVisibility(View.GONE);
                tvExpandIcon.setText("+");
            }
            
            // Set click listener to toggle expansion
            itemView.setOnClickListener(v -> {
                faqItem.toggleExpanded();
                notifyItemChanged(getAdapterPosition());
            });
        }
    }
}
