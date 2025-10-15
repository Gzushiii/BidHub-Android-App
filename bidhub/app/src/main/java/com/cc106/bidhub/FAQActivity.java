package com.cc106.bidhub;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cc106.bidhub.adapters.FAQAdapter;
import com.cc106.bidhub.models.FAQItem;
import java.util.ArrayList;
import java.util.List;

public class FAQActivity extends AppCompatActivity {
    
    private RecyclerView recyclerViewFAQ;
    private FAQAdapter faqAdapter;
    private List<FAQItem> faqItems;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);
        
        // Initialize views
        initializeViews();
        
        // Setup FAQ data
        setupFAQData();
        
        // Setup RecyclerView
        setupRecyclerView();
    }
    
    private void initializeViews() {
        recyclerViewFAQ = findViewById(R.id.recyclerViewFAQ);
        
        // Back button
        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }
    
    private void setupFAQData() {
        faqItems = new ArrayList<>();
        
        // General Questions
        faqItems.add(new FAQItem("What is BidHub?", 
            "BidHub is a mobile auction platform where you can buy and sell items through competitive bidding. Users bid with credits instead of real money, making it accessible and fun for everyone."));
        
        faqItems.add(new FAQItem("How do I get started?", 
            "1. Create an account with your email\n2. Purchase credits to start bidding\n3. Browse items or post your own\n4. Start bidding on items you like!"));
        
        faqItems.add(new FAQItem("What are credits?", 
            "Credits are the virtual currency used in BidHub. You can purchase credits with real money and use them to place bids on items. Credits make bidding more accessible and fun."));
        
        // Bidding Questions
        faqItems.add(new FAQItem("How does bidding work?", 
            "When you find an item you like, you can place a bid using your credits. The highest bidder wins when the auction ends. Make sure you have enough credits before bidding!"));
        
        faqItems.add(new FAQItem("What is Buy Now?", 
            "Buy Now allows you to purchase an item immediately at a fixed price set by the seller. This bypasses the auction process and gives you instant ownership of the item."));
        
        faqItems.add(new FAQItem("Can I retract my bid?", 
            "Bids are generally final once placed. However, you may be able to retract a bid if the auction hasn't ended and no other bids have been placed after yours. Check our terms for specific conditions."));
        
        // Selling Questions
        faqItems.add(new FAQItem("How do I sell an item?", 
            "1. Tap the 'Post' tab\n2. Fill in item details and upload photos\n3. Set a starting price and optional Buy Now price\n4. Choose auction duration\n5. Post your item and wait for bids!"));
        
        faqItems.add(new FAQItem("What are donation items?", 
            "Donation items are given away for free but require a reason for the donation. This helps build community and allows people to give back while decluttering their homes."));
        
        faqItems.add(new FAQItem("How many photos can I upload?", 
            "You can upload between 1 and 10 photos for each item. More photos help potential buyers see your item better and can increase bidding activity."));
        
        // Credits and Payment
        faqItems.add(new FAQItem("How do I buy credits?", 
            "Go to the Credits tab and choose from our credit packages. You can pay using various methods including GCash, bank transfer, or other supported payment options."));
        
        faqItems.add(new FAQItem("Are my payments secure?", 
            "Yes! We use secure payment processing to protect your financial information. All transactions are encrypted and processed through trusted payment providers."));
        
        faqItems.add(new FAQItem("Can I get a refund for credits?", 
            "Credit refunds are handled on a case-by-case basis. Contact our support team if you have issues with credit purchases or need assistance."));
        
        // Account and Profile
        faqItems.add(new FAQItem("What is a bidding alias?", 
            "Your bidding alias is a nickname that appears when you bid on items. It helps maintain privacy while still allowing other users to see who's bidding. You can regenerate your alias anytime."));
        
        faqItems.add(new FAQItem("How do I change my profile?", 
            "Go to the Profile tab and tap 'Edit' to update your information. You can change your username, email, and other profile details."));
        
        faqItems.add(new FAQItem("Is my personal information safe?", 
            "Yes, we take privacy seriously. Your real identity is protected during auctions, and we only share necessary information with other users (like your bidding alias)."));
        
        // Technical Support
        faqItems.add(new FAQItem("The app is not working properly", 
            "Try these troubleshooting steps:\n1. Close and restart the app\n2. Check your internet connection\n3. Update to the latest version\n4. Clear app cache\nIf problems persist, contact support."));
        
        faqItems.add(new FAQItem("I can't upload photos", 
            "Make sure you have:\n1. Given the app permission to access photos\n2. Photos are in supported formats (JPG, PNG)\n3. Photos are not too large (under 10MB each)\n4. Good internet connection"));
        
        faqItems.add(new FAQItem("How do I contact support?", 
            "You can reach our support team through:\n1. In-app support chat\n2. Email: support@bidhub.com\n3. Phone: +63-XXX-XXX-XXXX\nWe typically respond within 24 hours."));
        
        // Categories and Items
        faqItems.add(new FAQItem("What categories are available?", 
            "We have many categories including Fashion, Electronics, Home & Living, Hobbies & Games, and more. If your item doesn't fit any category, use 'Others'."));
        
        faqItems.add(new FAQItem("Can I sell anything?", 
            "Most items are allowed, but we have restrictions on:\n- Illegal items\n- Dangerous materials\n- Items that violate our terms\n- Counterfeit goods\nCheck our terms for the complete list."));
        
        faqItems.add(new FAQItem("How long do auctions last?", 
            "Default auction duration is 7 days, but sellers can choose different durations. You'll see the remaining time on each item. Auctions end automatically at the specified time."));
    }
    
    private void setupRecyclerView() {
        faqAdapter = new FAQAdapter(faqItems);
        recyclerViewFAQ.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFAQ.setAdapter(faqAdapter);
    }
}
