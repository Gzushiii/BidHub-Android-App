package com.cc106.bidhub.payments;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Supabase + Stripe Payment Gateway Implementation
 * Handles payment processing through Supabase backend with Stripe integration
 */
public class SupabaseStripePaymentGateway implements PaymentGateway {
    private static final String TAG = "SupabaseStripeGateway";
    
    // Supabase configuration
    private static final String SUPABASE_URL = "https://your-project.supabase.co";
    private static final String SUPABASE_ANON_KEY = "your-anon-key";
    private static final String STRIPE_PAYMENT_ENDPOINT = "/rest/v1/payments";
    
    // Payment methods
    public static final String PAYMENT_METHOD_CARD = "card";
    public static final String PAYMENT_METHOD_STRIPE = "stripe";
    
    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService executorService;
    
    public SupabaseStripePaymentGateway(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.executorService = Executors.newCachedThreadPool();
    }
    
    @Override
    public void processPayment(String userId, double amount, String currency, String description, PaymentCallback callback) {
        Log.i(TAG, "Processing Supabase+Stripe payment: " + amount + " " + currency + " for user: " + userId);
        
        executorService.execute(() -> {
            try {
                // Create payment intent through Supabase
                createPaymentIntent(userId, amount, currency, description, callback);
            } catch (Exception e) {
                Log.e(TAG, "Error processing payment", e);
                callback.onPaymentFailed("PAYMENT_ERROR", "Failed to process payment: " + e.getMessage());
            }
        });
    }
    
    private void createPaymentIntent(String userId, double amount, String currency, String description, PaymentCallback callback) {
        try {
            // Create payment request payload
            JSONObject paymentData = new JSONObject();
            paymentData.put("user_id", userId);
            paymentData.put("amount", (int) (amount * 100)); // Convert to cents
            paymentData.put("currency", currency.toLowerCase());
            paymentData.put("description", description);
            paymentData.put("payment_method", PAYMENT_METHOD_STRIPE);
            
            RequestBody body = RequestBody.create(
                paymentData.toString(),
                MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + STRIPE_PAYMENT_ENDPOINT)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(body)
                    .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network error creating payment intent", e);
                    callback.onPaymentFailed("NETWORK_ERROR", "Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            Log.i(TAG, "Payment intent created successfully");
                            
                            // Parse response to get payment intent details
                            JSONObject responseJson = new JSONObject(responseBody);
                            String paymentIntentId = responseJson.optString("id");
                            String clientSecret = responseJson.optString("client_secret");
                            
                            if (paymentIntentId != null && clientSecret != null) {
                                // For MVP, simulate successful payment
                                // In production, you would integrate with Stripe SDK here
                                simulateStripePayment(paymentIntentId, clientSecret, callback);
                            } else {
                                callback.onPaymentFailed("INVALID_RESPONSE", "Invalid payment intent response");
                            }
                            
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing payment response", e);
                            callback.onPaymentFailed("PARSE_ERROR", "Error parsing payment response");
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Payment intent creation failed: " + response.code() + " - " + errorBody);
                        callback.onPaymentFailed("API_ERROR", "Payment failed: " + response.code());
                    }
                }
            });
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating payment request", e);
            callback.onPaymentFailed("REQUEST_ERROR", "Error creating payment request");
        }
    }
    
    private void simulateStripePayment(String paymentIntentId, String clientSecret, PaymentCallback callback) {
        // Simulate Stripe payment processing
        executorService.execute(() -> {
            try {
                // Simulate processing delay
                Thread.sleep(2000);
                
                // Simulate 95% success rate for MVP
                if (Math.random() > 0.05) {
                    String transactionId = "stripe_" + paymentIntentId + "_" + System.currentTimeMillis();
                    String reference = "ref_" + paymentIntentId.substring(0, 8);
                    
                    Log.i(TAG, "Stripe payment successful: " + transactionId);
                    callback.onPaymentSuccess(transactionId, reference);
                } else {
                    Log.w(TAG, "Stripe payment failed (simulated)");
                    callback.onPaymentFailed("STRIPE_ERROR", "Payment declined by bank");
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                callback.onPaymentFailed("INTERRUPTED", "Payment processing was interrupted");
            }
        });
    }
    
    @Override
    public boolean isPaymentMethodSupported(String paymentMethod) {
        return PAYMENT_METHOD_CARD.equals(paymentMethod) || 
               PAYMENT_METHOD_STRIPE.equals(paymentMethod);
    }
    
    @Override
    public String getGatewayName() {
        return "Supabase + Stripe";
    }
    
    /**
     * Create a payment method for future use
     */
    public void createPaymentMethod(String userId, String cardToken, PaymentMethodCallback callback) {
        executorService.execute(() -> {
            try {
                JSONObject paymentMethodData = new JSONObject();
                paymentMethodData.put("user_id", userId);
                paymentMethodData.put("type", "card");
                paymentMethodData.put("card_token", cardToken);
                
                RequestBody body = RequestBody.create(
                    paymentMethodData.toString(),
                    MediaType.get("application/json; charset=utf-8")
                );
                
                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/payment_methods")
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();
                
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onError("Network error: " + e.getMessage());
                    }
                    
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            callback.onSuccess("Payment method created successfully");
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                            callback.onError("Failed to create payment method: " + response.code());
                        }
                    }
                });
                
            } catch (JSONException e) {
                callback.onError("Error creating payment method request");
            }
        });
    }
    
    /**
     * Get payment history for a user
     */
    public void getPaymentHistory(String userId, PaymentHistoryCallback callback) {
        executorService.execute(() -> {
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + STRIPE_PAYMENT_ENDPOINT + "?user_id=eq." + userId + "&order=created_at.desc")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .get()
                    .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        callback.onSuccess(responseBody);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        callback.onError("Failed to get payment history: " + response.code());
                    }
                }
            });
        });
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // Callback interfaces
    public interface PaymentMethodCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public interface PaymentHistoryCallback {
        void onSuccess(String paymentHistory);
        void onError(String error);
    }
}
