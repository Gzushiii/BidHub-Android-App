package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.models.Topup;
import com.cc106.bidhub.utils.Config;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class TopupApiClient extends ApiClient {
    
    public TopupApiClient(Context context) {
        super(context);
    }
    
    public Topup initiateTopup(Double amount, String paymentMethod) throws Exception {
        JSONObject body = new JSONObject();
        body.put("amount", amount);
        body.put("payment_method", paymentMethod);
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_TOPUPS, "POST");
        String response = sendRequest(conn, body);
        
        return parseTopup(new JSONObject(response));
    }
    
    public void submitReceipt(Integer topupId, String receiptRef) throws Exception {
        JSONObject body = new JSONObject();
        body.put("user_receipt_ref", receiptRef);
        
        HttpURLConnection conn = createConnection(
            Config.ENDPOINT_TOPUPS + "/" + topupId + "/submit", "POST");
        sendRequest(conn, body);
    }
    
    public Topup getTopup(Integer topupId) throws Exception {
        HttpURLConnection conn = createConnection(
            Config.ENDPOINT_TOPUPS + "/" + topupId, "GET");
        String response = sendRequest(conn, null);
        return parseTopup(new JSONObject(response));
    }
    
    public List<Topup> getTopups(String status, Integer limit, Integer offset) throws Exception {
        StringBuilder endpoint = new StringBuilder(Config.ENDPOINT_TOPUPS);
        List<String> params = new ArrayList<>();
        
        if (status != null) params.add("status=" + status);
        if (limit != null) params.add("limit=" + limit);
        if (offset != null) params.add("offset=" + offset);
        
        if (!params.isEmpty()) {
            endpoint.append("?").append(String.join("&", params));
        }
        
        HttpURLConnection conn = createConnection(endpoint.toString(), "GET");
        String response = sendRequest(conn, null);
        
        JSONObject json = new JSONObject(response);
        JSONArray topupsArray = json.getJSONArray("topups");
        List<Topup> topups = new ArrayList<>();
        
        for (int i = 0; i < topupsArray.length(); i++) {
            topups.add(parseTopup(topupsArray.getJSONObject(i)));
        }
        
        return topups;
    }
    
    private Topup parseTopup(JSONObject json) throws Exception {
        Topup topup = new Topup();
        if (json.has("id")) topup.setId(json.getInt("id"));
        if (json.has("topup_id")) topup.setId(json.getInt("topup_id"));
        if (json.has("amount")) topup.setAmount(json.getDouble("amount"));
        if (json.has("generated_ref")) topup.setGeneratedRef(json.getString("generated_ref"));
        if (json.has("payment_method")) topup.setPaymentMethod(json.getString("payment_method"));
        if (json.has("status")) topup.setStatus(json.getString("status"));
        if (json.has("instructions")) topup.setInstructions(json.getString("instructions"));
        if (json.has("payment_number")) topup.setPaymentNumber(json.getString("payment_number"));
        if (json.has("user_receipt_ref")) topup.setUserReceiptRef(json.getString("user_receipt_ref"));
        return topup;
    }
}

