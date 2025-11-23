package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.models.User;
import com.cc106.bidhub.utils.Config;
import com.cc106.bidhub.utils.TokenManager;
import org.json.JSONObject;
import java.net.HttpURLConnection;

public class AuthApiClient extends ApiClient {
    
    public AuthApiClient(Context context) {
        super(context);
    }
    
    public User register(String username, String email, String phoneNumber, 
                        String password, String firstName, String lastName, 
                        String alias) throws Exception {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("email", email);
        body.put("phone_number", phoneNumber);
        body.put("password", password);
        body.put("first_name", firstName);
        body.put("last_name", lastName);
        body.put("alias", alias);
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_AUTH_REGISTER, "POST");
        String response = sendRequest(conn, body);
        
        JSONObject json = new JSONObject(response);
        String token = json.getString("token");
        TokenManager.saveToken(context, token);
        
        JSONObject userJson = json.getJSONObject("user");
        User user = parseUser(userJson);
        TokenManager.saveUserId(context, user.getId());
        TokenManager.saveUserEmail(context, user.getEmail());
        
        return user;
    }
    
    public User login(String email, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_AUTH_LOGIN, "POST");
        String response = sendRequest(conn, body);
        
        JSONObject json = new JSONObject(response);
        String token = json.getString("token");
        TokenManager.saveToken(context, token);
        
        JSONObject userJson = json.getJSONObject("user");
        User user = parseUser(userJson);
        TokenManager.saveUserId(context, user.getId());
        TokenManager.saveUserEmail(context, user.getEmail());
        
        return user;
    }
    
    public void logout() {
        TokenManager.clearAll(context);
    }
    
    private User parseUser(JSONObject json) throws Exception {
        User user = new User();
        user.setId(json.getInt("id"));
        user.setUsername(json.getString("username"));
        user.setEmail(json.getString("email"));
        user.setFirstName(json.getString("first_name"));
        user.setLastName(json.getString("last_name"));
        user.setAlias(json.getString("alias"));
        user.setCredits(json.getDouble("credits"));
        return user;
    }
}

