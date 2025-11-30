package com.cc106.bidhub.adapters;

import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
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
        return new FAQViewHolder(view, this);
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
    
    public class FAQViewHolder extends RecyclerView.ViewHolder {
        private TextView tvQuestion;
        private TextView tvAnswer;
        private TextView tvExpandIcon;
        private FAQAdapter adapter;
        
        public FAQViewHolder(@NonNull View itemView, FAQAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            tvQuestion = itemView.findViewById(R.id.tv_question);
            tvAnswer = itemView.findViewById(R.id.tv_answer);
            tvExpandIcon = itemView.findViewById(R.id.tv_expand_icon);
            
            // Set content description for accessibility
            itemView.setContentDescription("FAQ question");
        }
        
        public void bind(FAQItem faqItem) {
            tvQuestion.setText(faqItem.getQuestion());
            tvAnswer.setText(faqItem.getAnswer());
            
            // Update accessibility content description
            String contentDesc = faqItem.getQuestion() + 
                (faqItem.isExpanded() ? ". Expanded. " + faqItem.getAnswer() : ". Tap to expand");
            itemView.setContentDescription(contentDesc);
            
            // Set visibility based on expanded state with animation
            if (faqItem.isExpanded()) {
                tvAnswer.setVisibility(View.VISIBLE);
                tvAnswer.setAlpha(1f);
                tvExpandIcon.setText("−");
            } else {
                tvAnswer.setVisibility(View.GONE);
                tvAnswer.setAlpha(0f);
                tvExpandIcon.setText("+");
            }
            
            // Set click listener to toggle expansion
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < adapter.faqItems.size()) {
                    FAQItem item = adapter.faqItems.get(position);
                    boolean wasExpanded = item.isExpanded();
                    item.toggleExpanded();
                    
                    // Animate the change
                    if (item.isExpanded() && !wasExpanded) {
                        // Expanding
                        tvAnswer.setVisibility(View.VISIBLE);
                        tvAnswer.setAlpha(0f);
                        ObjectAnimator animator = ObjectAnimator.ofFloat(tvAnswer, "alpha", 0f, 1f);
                        animator.setDuration(200);
                        animator.setInterpolator(new AccelerateDecelerateInterpolator());
                        animator.start();
                        tvExpandIcon.setText("−");
                    } else if (!item.isExpanded() && wasExpanded) {
                        // Collapsing
                        ObjectAnimator animator = ObjectAnimator.ofFloat(tvAnswer, "alpha", 1f, 0f);
                        animator.setDuration(200);
                        animator.setInterpolator(new AccelerateDecelerateInterpolator());
                        animator.start();
                        tvAnswer.postDelayed(() -> {
                            int currentPosition = getAdapterPosition();
                            if (currentPosition != RecyclerView.NO_POSITION && 
                                currentPosition < adapter.faqItems.size() &&
                                !adapter.faqItems.get(currentPosition).isExpanded()) {
                                tvAnswer.setVisibility(View.GONE);
                            }
                        }, 200);
                        tvExpandIcon.setText("+");
                    }
                    
                    // Update content description
                    String newContentDesc = item.getQuestion() + 
                        (item.isExpanded() ? ". Expanded. " + item.getAnswer() : ". Tap to expand");
                    itemView.setContentDescription(newContentDesc);
                }
            });
        }
    }
}
