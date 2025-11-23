package com.cc106.bidhub.api;

import android.content.Context;
import com.cc106.bidhub.models.Category;
import com.cc106.bidhub.utils.Config;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class CategoryApiClient extends ApiClient {
    
    public CategoryApiClient(Context context) {
        super(context);
    }
    
    public List<Category> getCategories() throws Exception {
        HttpURLConnection conn = createConnection(Config.ENDPOINT_CATEGORIES, "GET");
        String response = sendRequest(conn, null);
        
        JSONObject json = new JSONObject(response);
        JSONArray categoriesArray = json.getJSONArray("categories");
        List<Category> categories = new ArrayList<>();
        
        for (int i = 0; i < categoriesArray.length(); i++) {
            categories.add(parseCategory(categoriesArray.getJSONObject(i)));
        }
        
        return categories;
    }
    
    private Category parseCategory(JSONObject json) throws Exception {
        Category category = new Category();
        if (json.has("id")) category.setId(json.getInt("id"));
        if (json.has("name")) category.setName(json.getString("name"));
        if (json.has("description")) category.setDescription(json.getString("description"));
        if (json.has("parent_id") && !json.isNull("parent_id")) {
            category.setParentId(json.getInt("parent_id"));
        }
        if (json.has("subcategories")) {
            JSONArray subcategoriesArray = json.getJSONArray("subcategories");
            List<Category> subcategories = new ArrayList<>();
            for (int i = 0; i < subcategoriesArray.length(); i++) {
                subcategories.add(parseCategory(subcategoriesArray.getJSONObject(i)));
            }
            category.setSubcategories(subcategories);
        }
        return category;
    }
}

