package com.cc106.bidhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import com.cc106.bidhub.credits.CreditTransaction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Adapter for displaying credit transaction history with proper date formatting
 */
public class TransactionHistoryAdapter extends RecyclerView.Adapter<TransactionHistoryAdapter.TransactionViewHolder> {
    
    private List<CreditTransaction> transactions;
    private SimpleDateFormat fullDateFormat;
    
    public TransactionHistoryAdapter(List<CreditTransaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        
        // Full format: "Dec 15, 2023 2:30 PM"
        fullDateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
        fullDateFormat.setTimeZone(TimeZone.getDefault());
    }
    
    public void updateTransactions(List<CreditTransaction> newTransactions) {
        this.transactions = newTransactions != null ? newTransactions : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        if (position < 0 || position >= transactions.size()) {
            return;
        }
        
        CreditTransaction transaction = transactions.get(position);
        if (transaction == null) {
            return;
        }
        
        // Set transaction type
        String type = transaction.getType() != null ? transaction.getType().toUpperCase() : "UNKNOWN";
        holder.transactionType.setText(type);
        
        // Set description
        String description = transaction.getDescription();
        if (description == null || description.isEmpty()) {
            // Generate description based on type
            if ("purchase".equalsIgnoreCase(transaction.getType())) {
                description = "Credit Top-up";
            } else if ("bid".equalsIgnoreCase(transaction.getType())) {
                description = "Bid Placed";
            } else if ("refund".equalsIgnoreCase(transaction.getType())) {
                description = "Refund";
            } else {
                description = type;
            }
        }
        holder.transactionDescription.setText(description);
        
        // Format and set date with proper timezone handling
        Date createdAt = transaction.getCreatedAt();
        if (createdAt != null) {
            // Format date and time
            String formattedDate = fullDateFormat.format(createdAt);
            holder.transactionDate.setText(formattedDate);
        } else {
            holder.transactionDate.setText("Date not available");
        }
        
        // Format amount
        double amount = transaction.getAmount();
        String amountText;
        if (amount > 0) {
            amountText = "+₱" + String.format(Locale.getDefault(), "%.2f", amount);
            holder.transactionAmount.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.success_color));
            holder.transactionIcon.setText("+");
        } else {
            amountText = "₱" + String.format(Locale.getDefault(), "%.2f", Math.abs(amount));
            holder.transactionAmount.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.error_red));
            holder.transactionIcon.setText("-");
        }
        holder.transactionAmount.setText(amountText);
        
        // Set status
        String status = transaction.getStatus();
        if (status == null || status.isEmpty()) {
            status = "Completed";
        }
        holder.transactionStatus.setText(status);
        
        // Set status color
        if ("completed".equalsIgnoreCase(status)) {
            holder.transactionStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.success_color));
        } else if ("failed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
            holder.transactionStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.error_red));
        } else {
            holder.transactionStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.warning_yellow));
        }
    }
    
    @Override
    public int getItemCount() {
        return transactions.size();
    }
    
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView transactionIcon;
        TextView transactionType;
        TextView transactionDescription;
        TextView transactionDate;
        TextView transactionAmount;
        TextView transactionStatus;
        
        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionIcon = itemView.findViewById(R.id.transaction_icon);
            transactionType = itemView.findViewById(R.id.transaction_type);
            transactionDescription = itemView.findViewById(R.id.transaction_description);
            transactionDate = itemView.findViewById(R.id.transaction_date);
            transactionAmount = itemView.findViewById(R.id.transaction_amount);
            transactionStatus = itemView.findViewById(R.id.transaction_status);
        }
    }
}
