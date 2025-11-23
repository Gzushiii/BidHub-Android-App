package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.utils.Config;
import org.json.JSONObject;
import java.net.HttpURLConnection;

public class BidApiClient extends ApiClient {
    
    public BidApiClient(Context context) {
        super(context);
    }
    
    public void placeBid(String itemId, Double amount) throws Exception {
        JSONObject body = new JSONObject();
        body.put("item_id", itemId);
        body.put("amount", amount);
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_BIDS, "POST");
        sendRequest(conn, body);
    }
}

