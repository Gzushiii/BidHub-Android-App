package com.cc106.bidhub.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.R;
import com.cc106.bidhub.adapters.TransactionAdapter;
import com.cc106.bidhub.api.ApiClient;
import com.cc106.bidhub.api.CreditsApiClient;
import com.cc106.bidhub.api.TopupApiClient;
import com.cc106.bidhub.models.CreditTransaction;
import com.cc106.bidhub.models.Topup;
import java.util.ArrayList;
import java.util.List;

public class CreditsActivity extends AppCompatActivity {
    private TextView tvBalance;
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private ProgressBar progressBar;
    private Button btnTopUp;
    private List<CreditTransaction> transactions = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        tvBalance = findViewById(R.id.tvBalance);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        btnTopUp = findViewById(R.id.btnTopUp);
        
        adapter = new TransactionAdapter(transactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        btnTopUp.setOnClickListener(v -> showTopUpDialog());
        
        loadBalance();
        loadTransactions();
    }
    
    private void loadBalance() {
        new AsyncTask<Void, Void, Double>() {
            @Override
            protected Double doInBackground(Void... voids) {
                try {
                    CreditsApiClient apiClient = new CreditsApiClient(CreditsActivity.this);
                    return apiClient.getBalance();
                } catch (Exception e) {
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(Double balance) {
                if (balance != null) {
                    tvBalance.setText("₱" + String.format("%.2f", balance));
                }
            }
        }.execute();
    }
    
    private void loadTransactions() {
        progressBar.setVisibility(View.VISIBLE);
        
        new AsyncTask<Void, Void, List<CreditTransaction>>() {
            @Override
            protected List<CreditTransaction> doInBackground(Void... voids) {
                try {
                    CreditsApiClient apiClient = new CreditsApiClient(CreditsActivity.this);
                    return apiClient.getTransactions(null, null, 20, 0);
                } catch (Exception e) {
                    return new ArrayList<>();
                }
            }
            
            @Override
            protected void onPostExecute(List<CreditTransaction> result) {
                progressBar.setVisibility(View.GONE);
                transactions.clear();
                transactions.addAll(result);
                adapter.notifyDataSetChanged();
            }
        }.execute();
    }
    
    private void showTopUpDialog() {
        // Simple dialog for top-up
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_topup, null);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etPaymentMethod = dialogView.findViewById(R.id.etPaymentMethod);
        
        builder.setView(dialogView);
        builder.setTitle("Top Up Credits");
        builder.setPositiveButton("Initiate", (dialog, which) -> {
            String amountStr = etAmount.getText().toString();
            String paymentMethod = etPaymentMethod.getText().toString();
            
            if (amountStr.isEmpty() || paymentMethod.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                double amount = Double.parseDouble(amountStr);
                initiateTopup(amount, paymentMethod);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void initiateTopup(double amount, String paymentMethod) {
        progressBar.setVisibility(View.VISIBLE);
        
        new AsyncTask<Void, Void, Topup>() {
            private String errorMessage = null;
            
            @Override
            protected Topup doInBackground(Void... voids) {
                try {
                    TopupApiClient apiClient = new TopupApiClient(CreditsActivity.this);
                    return apiClient.initiateTopup(amount, paymentMethod);
                } catch (ApiClient.ApiException e) {
                    errorMessage = "Failed to initiate top-up: " + e.getMessage();
                    return null;
                } catch (Exception e) {
                    errorMessage = "Network error. Please check your connection.";
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(Topup topup) {
                progressBar.setVisibility(View.GONE);
                
                if (topup != null) {
                    Toast.makeText(CreditsActivity.this, 
                        "Top-up initiated. Reference: " + topup.getGeneratedRef(), 
                        Toast.LENGTH_LONG).show();
                    showReceiptDialog(topup.getId());
                } else {
                    Toast.makeText(CreditsActivity.this, 
                        errorMessage != null ? errorMessage : "Failed to initiate top-up", 
                        Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
    
    private void showReceiptDialog(int topupId) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_receipt, null);
        EditText etReceiptRef = dialogView.findViewById(R.id.etReceiptRef);
        
        builder.setView(dialogView);
        builder.setTitle("Submit Receipt");
        builder.setPositiveButton("Submit", (dialog, which) -> {
            String receiptRef = etReceiptRef.getText().toString();
            if (receiptRef.isEmpty()) {
                Toast.makeText(this, "Please enter receipt reference", Toast.LENGTH_SHORT).show();
                return;
            }
            submitReceipt(topupId, receiptRef);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void submitReceipt(int topupId, String receiptRef) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    TopupApiClient apiClient = new TopupApiClient(CreditsActivity.this);
                    apiClient.submitReceipt(topupId, receiptRef);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Toast.makeText(CreditsActivity.this, "Receipt submitted. Waiting for confirmation.", Toast.LENGTH_LONG).show();
                    loadBalance();
                    loadTransactions();
                } else {
                    Toast.makeText(CreditsActivity.this, "Failed to submit receipt", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

