package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.models.CreditTransaction;
import com.cc106.bidhub.utils.Config;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class CreditsApiClient extends ApiClient {
    
    public CreditsApiClient(Context context) {
        super(context);
    }
    
    public Double getBalance() throws Exception {
        HttpURLConnection conn = createConnection(Config.ENDPOINT_CREDITS_BALANCE, "GET");
        String response = sendRequest(conn, null);
        JSONObject json = new JSONObject(response);
        return json.getDouble("credits");
    }
    
    public List<CreditTransaction> getTransactions(String type, String status, 
                                                   Integer limit, Integer offset) throws Exception {
        StringBuilder endpoint = new StringBuilder(Config.ENDPOINT_CREDITS_TRANSACTIONS);
        List<String> params = new ArrayList<>();
        
        if (type != null) params.add("type=" + type);
        if (status != null) params.add("status=" + status);
        if (limit != null) params.add("limit=" + limit);
        if (offset != null) params.add("offset=" + offset);
        
        if (!params.isEmpty()) {
            endpoint.append("?").append(String.join("&", params));
        }
        
        HttpURLConnection conn = createConnection(endpoint.toString(), "GET");
        String response = sendRequest(conn, null);
        
        JSONObject json = new JSONObject(response);
        JSONArray transactionsArray = json.getJSONArray("transactions");
        List<CreditTransaction> transactions = new ArrayList<>();
        
        for (int i = 0; i < transactionsArray.length(); i++) {
            transactions.add(parseTransaction(transactionsArray.getJSONObject(i)));
        }
        
        return transactions;
    }
    
    public void purchaseCredits(Double amount, String paymentMethod, String transactionId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("amount", amount);
        body.put("payment_method", paymentMethod);
        body.put("transaction_id", transactionId);
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_CREDITS_PURCHASE, "POST");
        sendRequest(conn, body);
    }
    
    private CreditTransaction parseTransaction(JSONObject json) throws Exception {
        CreditTransaction transaction = new CreditTransaction();
        if (json.has("id")) transaction.setId(json.getInt("id"));
        if (json.has("type")) transaction.setType(json.getString("type"));
        if (json.has("amount")) transaction.setAmount(json.getDouble("amount"));
        if (json.has("status")) transaction.setStatus(json.getString("status"));
        if (json.has("created_at")) transaction.setCreatedAt(json.getString("created_at"));
        if (json.has("description")) transaction.setDescription(json.getString("description"));
        if (json.has("payment_method")) transaction.setPaymentMethod(json.getString("payment_method"));
        return transaction;
    }
}

