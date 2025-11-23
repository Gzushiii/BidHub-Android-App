package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.models.Item;
import com.cc106.bidhub.models.User;
import com.cc106.bidhub.utils.Config;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class ItemApiClient extends ApiClient {
    
    public ItemApiClient(Context context) {
        super(context);
    }
    
    public List<Item> getItems(String status, Integer categoryId, String search, 
                              Integer minPrice, Integer maxPrice, 
                              Integer limit, Integer offset) throws Exception {
        StringBuilder endpoint = new StringBuilder(Config.ENDPOINT_ITEMS);
        List<String> params = new ArrayList<>();
        
        if (status != null) params.add("status=" + status);
        if (categoryId != null) params.add("category_id=" + categoryId);
        if (search != null) params.add("search=" + search);
        if (minPrice != null) params.add("min_price=" + minPrice);
        if (maxPrice != null) params.add("max_price=" + maxPrice);
        if (limit != null) params.add("limit=" + limit);
        if (offset != null) params.add("offset=" + offset);
        
        if (!params.isEmpty()) {
            endpoint.append("?").append(String.join("&", params));
        }
        
        HttpURLConnection conn = createConnection(endpoint.toString(), "GET");
        String response = sendRequest(conn, null);
        
        JSONObject json = new JSONObject(response);
        JSONArray itemsArray = json.getJSONArray("items");
        List<Item> items = new ArrayList<>();
        
        for (int i = 0; i < itemsArray.length(); i++) {
            items.add(parseItem(itemsArray.getJSONObject(i)));
        }
        
        return items;
    }
    
    public Item getItemById(String itemId) throws Exception {
        HttpURLConnection conn = createConnection(Config.ENDPOINT_ITEMS + "/" + itemId, "GET");
        String response = sendRequest(conn, null);
        return parseItem(new JSONObject(response));
    }
    
    public Item createItem(String title, String description, Integer categoryId, 
                          Double startingPrice, Double reservePrice, 
                          Integer durationDays, List<String> images) throws Exception {
        JSONObject body = new JSONObject();
        body.put("title", title);
        body.put("description", description);
        body.put("category_id", categoryId);
        body.put("starting_price", startingPrice);
        if (reservePrice != null) body.put("reserve_price", reservePrice);
        if (durationDays != null) body.put("duration_days", durationDays);
        if (images != null && !images.isEmpty()) {
            JSONArray imagesArray = new JSONArray();
            for (String image : images) {
                imagesArray.put(image);
            }
            body.put("images", imagesArray);
        }
        
        HttpURLConnection conn = createConnection(Config.ENDPOINT_ITEMS, "POST");
        String response = sendRequest(conn, body);
        
        JSONObject json = new JSONObject(response);
        return parseItem(json.getJSONObject("item"));
    }
    
    public void publishItem(String itemId, Integer durationDays) throws Exception {
        JSONObject body = new JSONObject();
        if (durationDays != null) body.put("duration_days", durationDays);
        
        HttpURLConnection conn = createConnection(
            Config.ENDPOINT_ITEMS + "/" + itemId + "/publish", "POST");
        sendRequest(conn, body);
    }
    
    public void buyNow(String itemId, Double amount) throws Exception {
        JSONObject body = new JSONObject();
        if (amount != null) body.put("amount", amount);
        
        HttpURLConnection conn = createConnection(
            Config.ENDPOINT_ITEMS + "/" + itemId + "/buy-now", "POST");
        sendRequest(conn, body);
    }
    
    private Item parseItem(JSONObject json) throws Exception {
        Item item = new Item();
        if (json.has("id")) item.setId(json.getString("id"));
        if (json.has("uuid_id")) item.setUuidId(json.getString("uuid_id"));
        if (json.has("title")) item.setTitle(json.getString("title"));
        if (json.has("description")) item.setDescription(json.getString("description"));
        if (json.has("category_id")) item.setCategoryId(json.getInt("category_id"));
        if (json.has("starting_bid")) item.setStartingBid(json.getDouble("starting_bid"));
        if (json.has("current_bid")) item.setCurrentBid(json.getDouble("current_bid"));
        if (json.has("buy_now_price")) item.setBuyNowPrice(json.getDouble("buy_now_price"));
        if (json.has("status")) item.setStatus(json.getString("status"));
        if (json.has("end_date")) item.setEndDate(json.getString("end_date"));
        if (json.has("images")) {
            JSONArray imagesArray = json.getJSONArray("images");
            List<String> images = new ArrayList<>();
            for (int i = 0; i < imagesArray.length(); i++) {
                JSONObject imageObj = imagesArray.getJSONObject(i);
                if (imageObj.has("image_url")) {
                    images.add(imageObj.getString("image_url"));
                } else {
                    images.add(imageObj.getString("url"));
                }
            }
            item.setImages(images);
        }
        if (json.has("seller")) {
            JSONObject sellerJson = json.getJSONObject("seller");
            User seller = new User();
            if (sellerJson.has("id")) seller.setId(sellerJson.getInt("id"));
            if (sellerJson.has("username")) seller.setUsername(sellerJson.getString("username"));
            if (sellerJson.has("alias")) seller.setAlias(sellerJson.getString("alias"));
            item.setSeller(seller);
        }
        return item;
    }
}

