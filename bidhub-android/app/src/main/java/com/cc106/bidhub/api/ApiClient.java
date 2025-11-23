package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.utils.Config;
import com.cc106.bidhub.utils.TokenManager;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiClient {
    protected Context context;
    protected String baseUrl;
    
    public ApiClient(Context context) {
        this.context = context;
        this.baseUrl = Config.API_BASE_URL;
    }
    
    protected HttpURLConnection createConnection(String endpoint, String method) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(Config.CONNECT_TIMEOUT);
        conn.setReadTimeout(Config.READ_TIMEOUT);
        
        // Add auth token if available
        String token = TokenManager.getToken(context);
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        
        return conn;
    }
    
    protected String sendRequest(HttpURLConnection conn, JSONObject body) throws Exception {
        if (body != null) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.flush();
            os.close();
        }
        
        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        if (responseCode >= 200 && responseCode < 300) {
            return response.toString();
        } else {
            throw new ApiException(responseCode, response.toString());
        }
    }
    
    public static class ApiException extends Exception {
        private int statusCode;
        private String response;
        
        public ApiException(int statusCode, String response) {
            super("API Error: " + statusCode);
            this.statusCode = statusCode;
            this.response = response;
        }
        
        public int getStatusCode() { return statusCode; }
        public String getResponse() { return response; }
    }
}

