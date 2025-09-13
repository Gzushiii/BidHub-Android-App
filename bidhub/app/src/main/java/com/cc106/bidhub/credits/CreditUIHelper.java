package com.cc106.bidhub.credits;

import android.content.Context;
import android.widget.Toast;

import com.cc106.bidhub.toast.ToastHelper;

import java.util.List;

/**
 * UI Helper for Credit Management
 * Provides utility methods for displaying credit information
 */
public class CreditUIHelper {
    
    /**
     * Display credit balance information
     */
    public static void showBalanceInfo(Context context, String userId, SimpleCreditManager creditManager) {
        if (userId == null || creditManager == null) {
            ToastHelper.showError(context, "Unable to load credit information");
            return;
        }
        
        double balance = creditManager.getCreditBalance(userId);
        String balanceText = "Credit Balance: " + creditManager.formatCurrency(balance);
        ToastHelper.showInfo(context, balanceText);
    }
    
    /**
     * Display available credit packages
     */
    public static void showAvailablePackages(Context context, SimpleCreditManager creditManager) {
        if (creditManager == null) {
            ToastHelper.showError(context, "Unable to load packages");
            return;
        }
        
        List<CreditPackage> packages = creditManager.getAvailablePackages();
        if (packages.isEmpty()) {
            ToastHelper.showInfo(context, "No credit packages available");
            return;
        }
        
        StringBuilder packageInfo = new StringBuilder("Available Packages:\n");
        for (CreditPackage pkg : packages) {
            packageInfo.append("• ").append(pkg.getName())
                      .append(" - ").append(creditManager.formatCurrency(pkg.getCredits()))
                      .append(" for ").append(creditManager.formatCurrency(pkg.getPrice()))
                      .append("\n");
        }
        
        ToastHelper.showInfo(context, packageInfo.toString());
    }
    
    /**
     * Display transaction history
     */
    public static void showTransactionHistory(Context context, String userId, SimpleCreditManager creditManager) {
        if (userId == null || creditManager == null) {
            ToastHelper.showError(context, "Unable to load transaction history");
            return;
        }
        
        List<CreditTransaction> transactions = creditManager.getTransactionHistory(userId);
        if (transactions.isEmpty()) {
            ToastHelper.showInfo(context, "No transactions found");
            return;
        }
        
        StringBuilder history = new StringBuilder("Recent Transactions:\n");
        int count = Math.min(transactions.size(), 5); // Show last 5 transactions
        
        for (int i = 0; i < count; i++) {
            CreditTransaction transaction = transactions.get(i);
            String amount = creditManager.formatCurrency(Math.abs(transaction.getAmount()));
            String sign = transaction.getAmount() > 0 ? "+" : "-";
            
            history.append("• ").append(transaction.getType())
                  .append(": ").append(sign).append(amount)
                  .append(" (").append(transaction.getStatus()).append(")\n");
        }
        
        ToastHelper.showInfo(context, history.toString());
    }
    
    /**
     * Display purchase success message
     */
    public static void showPurchaseSuccess(Context context, CreditPackage pkg) {
        String message = "Purchase successful!\n" +
                        pkg.getName() + " - " + 
                        String.format("₱%.2f", pkg.getCredits()) + " credits added";
        ToastHelper.showSuccess(context, message);
    }
    
    /**
     * Display purchase failure message
     */
    public static void showPurchaseFailure(Context context, String errorMessage) {
        ToastHelper.showError(context, "Purchase failed: " + errorMessage);
    }
    
    /**
     * Display insufficient credits message
     */
    public static void showInsufficientCredits(Context context, double required, double available) {
        String message = String.format("Insufficient credits!\nRequired: ₱%.2f\nAvailable: ₱%.2f", 
                                     required, available);
        ToastHelper.showError(context, message);
    }
}
