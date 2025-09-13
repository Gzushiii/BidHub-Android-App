package com.cc106.bidhub;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cc106.bidhub.credits.SimpleCreditManager;
import com.cc106.bidhub.credits.CreditPackage;
import com.cc106.bidhub.toast.ToastHelper;

import java.util.List;

/**
 * Test activity to verify credits functionality
 * This can be used to test the credits system independently
 */
public class CreditsTestActivity extends BaseActivity {
    
    private SimpleCreditManager creditManager;
    private TextView statusText;
    private LinearLayout testContainer;
    private String testUserId = "test_user_12345";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create a simple test layout
        createTestLayout();
        
        // Initialize credit manager
        creditManager = new SimpleCreditManager(this);
        
        // Run tests
        runCreditsTests();
    }
    
    private void createTestLayout() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);
        
        // Status text
        statusText = new TextView(this);
        statusText.setText("Running Credits Tests...");
        statusText.setTextSize(16);
        statusText.setPadding(0, 0, 0, 16);
        mainLayout.addView(statusText);
        
        // Test container
        testContainer = new LinearLayout(this);
        testContainer.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(testContainer);
        
        setContentView(mainLayout);
    }
    
    private void runCreditsTests() {
        updateStatus("Testing Credits System...");
        
        // Test 1: Add credits
        updateStatus("Test 1: Adding 100 credits...");
        boolean success = creditManager.addCredits(testUserId, 100.0, SimpleCreditManager.TRANSACTION_PURCHASE);
        addTestResult("Add Credits", success);
        
        // Test 2: Check balance
        updateStatus("Test 2: Checking balance...");
        double balance = creditManager.getCreditBalance(testUserId);
        addTestResult("Check Balance", "Balance: " + creditManager.formatCurrency(balance));
        
        // Test 3: Get packages
        updateStatus("Test 3: Getting credit packages...");
        List<CreditPackage> packages = creditManager.getAvailablePackages();
        addTestResult("Get Packages", "Found " + packages.size() + " packages");
        
        // Test 4: Purchase credits
        updateStatus("Test 4: Testing purchase...");
        if (!packages.isEmpty()) {
            CreditPackage pkg = packages.get(0);
            boolean purchaseSuccess = creditManager.purchaseCredits(testUserId, pkg.getPackageId(), SimpleCreditManager.PAYMENT_TEST);
            addTestResult("Purchase Credits", purchaseSuccess);
        }
        
        // Test 5: Check final balance
        updateStatus("Test 5: Checking final balance...");
        double finalBalance = creditManager.getCreditBalance(testUserId);
        addTestResult("Final Balance", "Final Balance: " + creditManager.formatCurrency(finalBalance));
        
        updateStatus("All tests completed!");
    }
    
    private void updateStatus(String message) {
        statusText.setText(message);
        android.util.Log.d("CreditsTest", message);
    }
    
    private void addTestResult(String testName, boolean success) {
        addTestResult(testName, success ? "PASSED" : "FAILED");
    }
    
    private void addTestResult(String testName, String result) {
        TextView resultText = new TextView(this);
        resultText.setText(testName + ": " + result);
        resultText.setTextSize(14);
        resultText.setPadding(0, 8, 0, 8);
        testContainer.addView(resultText);
        
        android.util.Log.d("CreditsTest", testName + ": " + result);
    }
    
    @Override
    protected boolean isCurrentActivity(int itemId) {
        return false; // This is a test activity
    }
    
    @Override
    protected void setCurrentTabSelected() {
        // No tab selection for test activity
    }
    
    @Override
    public String getCurrentUserEmail() {
        return "test@example.com";
    }
}
