package com.cc106.bidhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import com.cc106.bidhub.models.CreditTransaction;
import com.cc106.bidhub.utils.DateUtils;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private List<CreditTransaction> transactions;
    
    public TransactionAdapter(List<CreditTransaction> transactions) {
        this.transactions = transactions;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CreditTransaction transaction = transactions.get(position);
        holder.tvType.setText(transaction.getType());
        holder.tvAmount.setText("₱" + String.format("%.2f", transaction.getAmount()));
        holder.tvStatus.setText(transaction.getStatus());
        if (transaction.getCreatedAt() != null) {
            holder.tvDate.setText(DateUtils.formatDateShort(transaction.getCreatedAt()));
        }
    }
    
    @Override
    public int getItemCount() {
        return transactions.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvAmount, tvStatus, tvDate;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvType);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}

