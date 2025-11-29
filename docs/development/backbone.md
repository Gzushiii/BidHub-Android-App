# AI Studio Prompt:
Create a modular design backbone skeleton for my app called: **BidHub Mobile Bidding Platform**

Use these theme colors:
- Primary: #007AFF (iOS system blue)
- Secondary: #8E8E93 (text secondary gray)
- Tertiary: #34C759 (success green)

### Requirements

#### 1. Core Layout
- Main content area: **Auction item cards with grid layout, bid status indicators, countdown timers, and user credit balance display**
- Footer: **Bottom navigation with 5 tabs (Home, Browse, Post, Credits, Profile), active state indicators, and notification badges**
- Example components: **Material Design buttons, input fields with validation, image upload components, bid placement modals, credit purchase cards, user profile cards**

#### 2. Onboarding Flow
- Welcome screen: **"Welcome to BidHub - Your bid, your win" with app logo, tagline, and "Get Started" CTA button**
- Questionnaire: **User preference setup (notification preferences, bidding categories of interest, payment method selection)**
- Progress indicator: **Step-by-step progress bar (1/4, 2/4, 3/4, 4/4) with completion checkmarks**
- Summary screen: **Account setup overview showing alias generation, credit balance (0), and "Start Bidding" action button**

#### 3. Conversion Funnel (Paywall)
- Pricing tiers: **3 tiers - Starter (100 credits), Popular (500 credits), Premium (1000 credits) with volume discounts**
- CTA button: **"Buy Credits Now" with payment method selection (GCash/Maya)**
- Trust elements: **"Secure Payment", "Instant Credit Delivery", "Money-back Guarantee", user testimonials, and security badges**

#### 4. Custom Features
- **Credit-based bidding system with pre-paid validation**
- **Alias system for bidder privacy protection**
- **Real-time auction countdown timers**
- **Image upload with compression for item listings**
- **Redemption code system for credit top-ups**
- **Bid history and transaction tracking**
- **Category-based item filtering and search**
- **Winner notification system with secure contact sharing**

### Design Principles
- Wireframe placeholders (not final visuals).
- Modular, scalable, adaptive for web + mobile.
- Material Design 3 components with clean minimalism
- 8dp grid system for consistent spacing
- High contrast ratios for accessibility
- Touch-friendly 44dp minimum target sizes

---

## 🎨 **BIDHUB DESIGN BACKBONE ARCHITECTURE**

### **Color System**
- **Primary Blue**: #007AFF - Primary actions, links, active states
- **Background White**: #FFFFFF - Main content areas, cards
- **Text Primary**: #000000 - Main text content, titles
- **Text Secondary**: #8E8E93 - Supporting text, placeholders
- **Border Gray**: #D1D1D6 - Input field borders, dividers
- **Error Red**: #FF3B30 - Validation errors, warnings
- **Success Green**: #34C759 - Positive feedback, success states
- **Warning Orange**: #FF9500 - Caution states, alerts

### **Typography Hierarchy**
- **Large Title**: 28-32sp, Bold, Primary color (App headers)
- **Title**: 22-24sp, Bold, Primary color (Screen titles)
- **Headline**: 18-20sp, SemiBold, Primary color (Section headers)
- **Body**: 16-17sp, Regular, Primary color (Main content)
- **Caption**: 14sp, Regular, Secondary color (Supporting text)
- **Small Text**: 12sp, Regular, Secondary color (Labels, metadata)

### **Layout Structure**
- **Status Bar**: 24dp height, system icons
- **Header Section**: 56-64dp height with back navigation and title
- **Content Area**: Flexible height with 16-24dp horizontal padding
- **Bottom Navigation**: 56dp height with 5 tabs and active indicators
- **Action Area**: Fixed height for primary buttons and CTAs

### **Spacing System (8dp Grid)**
- **Micro**: 4dp - Between related elements
- **Small**: 8dp - Between form fields
- **Medium**: 16dp - Between sections
- **Large**: 24dp - Between major content blocks
- **Extra Large**: 32dp - Between screen sections

---

## 📱 **CORE SCREEN WIREFRAMES**

### **1. Authentication Screens**

#### **Login Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ ← [Back]    BidHub    [Logo] →  │
├─────────────────────────────────┤
│                                 │
│    [BidHub Logo]                │
│    Your bid, your win           │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Email Address               │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Password                    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │     Sign In                 │ │
│ └─────────────────────────────┘ │
│                                 │
│ Don't have an account?          │
│ [Create Account]                │
│                                 │
│ [Forgot Password?]              │
└─────────────────────────────────┘
```

#### **Registration Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ ← [Back]    Sign Up    [Help] → │
├─────────────────────────────────┤
│                                 │
│ Create Your Account             │
│ Join the bidding community      │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ First Name                  │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Last Name                   │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Email Address               │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Phone Number                │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Password                    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │     Create Account          │ │
│ └─────────────────────────────┘ │
│                                 │
│ By signing up, you agree to     │
│ [Terms of Service] and          │
│ [Privacy Policy]                │
└─────────────────────────────────┘
```

### **2. Main App Screens**

#### **Home Dashboard**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ [Profile] BidHub    [Search] 🔔 │
├─────────────────────────────────┤
│                                 │
│ Welcome back, [User Alias]!     │
│ Your credit balance: 500 💰     │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🔍 Search items...          │ │
│ └─────────────────────────────┘ │
│                                 │
│ Quick Actions                   │
│ ┌─────────┐ ┌─────────┐ ┌─────┐ │
│ │ Browse  │ │ Post    │ │Buy  │ │
│ │ Items   │ │ Item    │ │Creds│ │
│ └─────────┘ └─────────┘ └─────┘ │
│                                 │
│ Featured Auctions               │
│ ┌─────────────────────────────┐ │
│ │ [Item Image]                │ │
│ │ Vintage Camera              │ │
│ │ Current: ₱1,200             │ │
│ │ Time: 2h 15m left           │ │
│ │ [Place Bid]                 │ │
│ └─────────────────────────────┘ │
│                                 │
│ Your Active Bids                │
│ ┌─────────────────────────────┐ │
│ │ [Item Image]                │ │
│ │ Gaming Console              │ │
│ │ Your Bid: ₱3,500            │ │
│ │ Status: Winning 🏆          │ │
│ └─────────────────────────────┘ │
│                                 │
├─────────────────────────────────┤
│ 🏠 Browse Post 💰 Profile      │
└─────────────────────────────────┘
```

#### **Browse Items Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ ← [Back]    Browse    [Filter] →│
├─────────────────────────────────┤
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🔍 Search items...          │ │
│ └─────────────────────────────┘ │
│                                 │
│ Categories                      │
│ [Electronics] [Fashion] [Home]  │
│ [Sports] [Books] [Art] [More]   │
│                                 │
│ Sort: [Newest] [Price] [Ending] │
│                                 │
│ ┌─────────┐ ┌─────────┐ ┌─────┐ │
│ │[Image]  │ │[Image]  │ │[Img]│ │
│ │iPhone   │ │Sneakers │ │Book │ │
│ │₱15,000  │ │₱2,500   │ │₱800 │ │
│ │2h left  │ │5h left  │ │1d   │ │
│ └─────────┘ └─────────┘ └─────┘ │
│                                 │
│ ┌─────────┐ ┌─────────┐ ┌─────┐ │
│ │[Image]  │ │[Image]  │ │[Img]│ │
│ │Laptop   │ │Watch    │ │Game │ │
│ │₱25,000  │ │₱8,000   │ │₱1,2│ │
│ │3d left  │ │12h left │ │00  │ │
│ └─────────┘ └─────────┘ └─────┘ │
│                                 │
├─────────────────────────────────┤
│ 🏠 Browse Post 💰 Profile      │
└─────────────────────────────────┘
```

#### **Item Detail Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ ← [Back]    Item    [Share] ❤️  │
├─────────────────────────────────┤
│                                 │
│ ┌─────────────────────────────┐ │
│ │                             │ │
│ │     [Item Image Gallery]    │ │
│ │                             │ │
│ │  ● ○ ○ ○ ○  [1/5]          │ │
│ └─────────────────────────────┘ │
│                                 │
│ Vintage Canon Camera            │
│ Electronics > Cameras           │
│                                 │
│ Condition: Excellent            │
│ Starting Bid: ₱1,000            │
│ Current Bid: ₱1,200             │
│ Time Left: 2h 15m 30s           │
│                                 │
│ Description:                    │
│ This vintage Canon camera is    │
│ in excellent working condition. │
│ Perfect for photography...      │
│                                 │
│ Seller: CameraCollector_23      │
│ ⭐⭐⭐⭐⭐ (127 reviews)          │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Your Bid: ₱____             │ │
│ │ [Place Bid] [Buy Now]       │ │
│ └─────────────────────────────┘ │
│                                 │
│ Bid History                     │
│ User_456 - ₱1,200 (2h ago)     │
│ PhotoFan_99 - ₱1,100 (3h ago)  │
│ CameraLover - ₱1,000 (5h ago)  │
│                                 │
├─────────────────────────────────┤
│ 🏠 Browse Post 💰 Profile      │
└─────────────────────────────────┘
```

#### **Post Item Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ ← [Back]    Post Item    [Save] │
├─────────────────────────────────┤
│                                 │
│ Create New Listing              │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Item Title                  │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Item Description            │ │
│ │                             │ │
│ │                             │ │
│ └─────────────────────────────┘ │
│                                 │
│ Add Photos (0/10)               │
│ ┌─────────┐ ┌─────────┐ ┌─────┐ │
│ │   +     │ │   +     │ │  +  │ │
│ │ Add     │ │ Add     │ │ Add │ │
│ └─────────┘ └─────────┘ └─────┘ │
│                                 │
│ Category                        │
│ ┌─────────────────────────────┐ │
│ │ Electronics ▼               │ │
│ └─────────────────────────────┘ │
│                                 │
│ Condition                       │
│ ┌─────────────────────────────┐ │
│ │ Excellent ▼                 │ │
│ └─────────────────────────────┘ │
│                                 │
│ Starting Bid                    │
│ ┌─────────────────────────────┐ │
│ │ ₱____                       │ │
│ └─────────────────────────────┘ │
│                                 │
│ Auction Duration                │
│ ┌─────────────────────────────┐ │
│ │ 7 days ▼                    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │     List Item               │ │
│ └─────────────────────────────┘ │
│                                 │
├─────────────────────────────────┤
│ 🏠 Browse Post 💰 Profile      │
└─────────────────────────────────┘
```

#### **Credits Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ ← [Back]    Credits    [History]│
├─────────────────────────────────┤
│                                 │
│ Your Credit Balance             │
│ 500 💰                         │
│                                 │
│ Buy More Credits                │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Starter Package             │ │
│ │ 100 Credits                 │ │
│ │ ₱100                        │ │
│ │ [Select]                    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Popular Package             │ │
│ │ 500 Credits                 │ │
│ │ ₱450 (Save ₱50)             │ │
│ │ [Select] ⭐                 │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Premium Package             │ │
│ │ 1000 Credits                │ │
│ │ ₱800 (Save ₱200)            │ │
│ │ [Select]                    │ │
│ └─────────────────────────────┘ │
│                                 │
│ Payment Method                  │
│ ┌─────────────────────────────┐ │
│ │ [GCash] [Maya] [Bank]       │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │     Buy Credits             │ │
│ └─────────────────────────────┘ │
│                                 │
│ Redeem Code                     │
│ ┌─────────────────────────────┐ │
│ │ Enter redemption code       │ │
│ └─────────────────────────────┘ │
│                                 │
├─────────────────────────────────┤
│ 🏠 Browse Post 💰 Profile      │
└─────────────────────────────────┘
```

#### **Profile Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ ← [Back]    Profile    [Edit] → │
├─────────────────────────────────┤
│                                 │
│ ┌─────────────────────────────┐ │
│ │     [Profile Picture]       │ │
│ │                             │ │
│ │ CameraCollector_23          │ │
│ │ ⭐⭐⭐⭐⭐ (127 reviews)        │ │
│ └─────────────────────────────┘ │
│                                 │
│ Account Information             │
│ ┌─────────────────────────────┐ │
│ │ Name: John Smith            │ │
│ │ Email: john@email.com       │ │
│ │ Phone: +63 912 345 6789     │ │
│ │ Member since: Jan 2024      │ │
│ └─────────────────────────────┘ │
│                                 │
│ My Activity                     │
│ ┌─────────────────────────────┐ │
│ │ Active Bids: 3              │ │
│ │ Won Auctions: 12            │ │
│ │ Items Sold: 8               │ │
│ │ Total Spent: ₱45,000        │ │
│ └─────────────────────────────┘ │
│                                 │
│ Settings                        │
│ ┌─────────────────────────────┐ │
│ │ Notifications               │ │
│ │ Privacy Settings            │ │
│ │ Payment Methods             │ │
│ │ Help & Support              │ │
│ │ About BidHub                │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │     Sign Out                │ │
│ └─────────────────────────────┘ │
│                                 │
├─────────────────────────────────┤
│ 🏠 Browse Post 💰 Profile      │
└─────────────────────────────────┘
```

### **3. Onboarding Flow Screens**

#### **Welcome Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│                                 │
│                                 │
│        [BidHub Logo]            │
│                                 │
│    Welcome to BidHub            │
│                                 │
│    Your bid, your win           │
│                                 │
│    The secure mobile bidding    │
│    platform where every bid     │
│    counts and every win is      │
│    guaranteed.                  │
│                                 │
│                                 │
│ ┌─────────────────────────────┐ │
│ │     Get Started             │ │
│ └─────────────────────────────┘ │
│                                 │
│ Already have an account?        │
│ [Sign In]                       │
│                                 │
│                                 │
└─────────────────────────────────┘
```

#### **Preference Setup Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ ← [Back]    Setup    [Skip] →   │
├─────────────────────────────────┤
│                                 │
│ Let's personalize your          │
│ experience                      │
│                                 │
│ Step 1 of 4                     │
│ ████░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                 │
│ What interests you?             │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ☑ Electronics               │ │
│ │ ☐ Fashion & Accessories     │ │
│ │ ☑ Home & Garden             │ │
│ │ ☐ Sports & Recreation       │ │
│ │ ☐ Books & Media             │ │
│ │ ☐ Art & Collectibles        │ │
│ └─────────────────────────────┘ │
│                                 │
│ Notification Preferences        │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ☑ Bid Updates               │ │
│ │ ☑ Auction Endings           │ │
│ │ ☐ Marketing Emails          │ │
│ │ ☑ Payment Confirmations     │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │        Continue             │ │
│ └─────────────────────────────┘ │
│                                 │
└─────────────────────────────────┘
```

#### **Summary Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ ← [Back]    Complete    [Help] →│
├─────────────────────────────────┤
│                                 │
│ You're all set!                 │
│                                 │
│ Step 4 of 4                     │
│ ████████████████████████████████│
│                                 │
│ Your Account Summary            │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Alias: CameraCollector_23   │ │
│ │ Credits: 0 💰               │ │
│ │ Interests: Electronics,     │ │
│ │           Home & Garden     │ │
│ │ Notifications: Enabled      │ │
│ └─────────────────────────────┘ │
│                                 │
│ Next Steps:                     │
│ • Buy credits to start bidding  │
│ • Browse items in your          │
│   favorite categories           │
│ • List your first item          │
│                                 │
│ ┌─────────────────────────────┐ │
│ │     Start Bidding           │ │
│ └─────────────────────────────┘ │
│                                 │
└─────────────────────────────────┘
```

### **4. Payment & Credit Screens**

#### **Credit Purchase Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ ← [Back]    Buy Credits    [❓] │
├─────────────────────────────────┤
│                                 │
│ Choose Your Package             │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Starter                     │ │
│ │ 100 Credits                 │ │
│ │ ₱100                        │ │
│ │ Perfect for trying out      │ │
│ │ [Select]                    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Popular ⭐                  │ │
│ │ 500 Credits                 │ │
│ │ ₱450 (Save ₱50)             │ │
│ │ Best value for regular      │ │
│ │ bidders                     │ │
│ │ [Select]                    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Premium                     │ │
│ │ 1000 Credits                │ │
│ │ ₱800 (Save ₱200)            │ │
│ │ For serious collectors      │ │
│ │ [Select]                    │ │
│ └─────────────────────────────┘ │
│                                 │
│ Payment Method                  │
│ ┌─────────────────────────────┐ │
│ │ [GCash] [Maya] [Bank]       │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │     Buy Credits             │ │
│ └─────────────────────────────┘ │
│                                 │
│ 🔒 Secure Payment               │
│ 💯 Money-back Guarantee         │
│ ⚡ Instant Credit Delivery      │
└─────────────────────────────────┘
```

#### **Redemption Code Screen**
```
┌─────────────────────────────────┐
│ [Status Bar]                    │
├─────────────────────────────────┤
│ ← [Back]    Redeem Code    [❓] │
├─────────────────────────────────┤
│                                 │
│ Enter Your Redemption Code      │
│                                 │
│ After purchasing credits, you   │
│ will receive a unique code via  │
│ email or SMS. Enter it here to  │
│ add credits to your account.    │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Redemption Code             │ │
│ │ ABC123XYZ                   │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │     Redeem Credits          │ │
│ └─────────────────────────────┘ │
│                                 │
│ Didn't receive your code?       │
│ [Resend Code]                   │
│                                 │
│ Need help?                      │
│ [Contact Support]               │
│                                 │
│ Recent Redemptions              │
│ ┌─────────────────────────────┐ │
│ │ ABC123XYZ - 500 credits     │ │
│ │ 2 hours ago                 │ │
│ │                             │ │
│ │ DEF456GHI - 100 credits     │ │
│ │ 1 day ago                   │ │
│ └─────────────────────────────┘ │
│                                 │
└─────────────────────────────────┘
```

---

## 🧩 **COMPONENT LIBRARY**

### **Button Components**
- **Primary Button**: Blue background, white text, 56dp height
- **Secondary Button**: White background, blue border, blue text
- **Text Button**: Blue text, no background, underlined
- **Icon Button**: Circular, 44dp, with icon and optional label
- **Floating Action Button**: Circular, 56dp, with icon

### **Input Components**
- **Text Input**: 56dp height, rounded corners, validation states
- **Search Input**: With search icon, clear button
- **Number Input**: For bid amounts and prices
- **Dropdown Select**: Category and condition selection
- **Text Area**: For item descriptions

### **Card Components**
- **Item Card**: Image, title, price, time remaining
- **Credit Package Card**: Package details, pricing, selection
- **User Profile Card**: Avatar, name, rating, stats
- **Bid History Card**: Bidder alias, amount, timestamp
- **Transaction Card**: Transaction details, status, amount

### **Navigation Components**
- **Bottom Navigation**: 5 tabs with active indicators
- **Top Navigation**: Back button, title, action buttons
- **Tab Navigation**: Horizontal tabs with underline indicator
- **Breadcrumb**: Category navigation path

### **Feedback Components**
- **Toast Messages**: Success, error, warning notifications
- **Loading Spinner**: For async operations
- **Progress Bar**: For multi-step processes
- **Empty State**: No items, no bids, no results
- **Error State**: Network errors, validation errors

### **Modal Components**
- **Bid Placement Modal**: Bid amount input, confirmation
- **Image Upload Modal**: Photo selection, preview, upload
- **Payment Modal**: Payment method selection, confirmation
- **Confirmation Modal**: Action confirmation with details
- **Filter Modal**: Advanced search and filter options

---

## 📐 **RESPONSIVE DESIGN BREAKPOINTS**

### **Mobile (320px - 768px)**
- Single column layout
- Stacked navigation
- Touch-optimized controls
- Swipe gestures for image galleries
- Bottom sheet modals

### **Tablet (768px - 1024px)**
- Two-column layout for item grids
- Side navigation drawer
- Larger touch targets
- Split-screen modals
- Enhanced image galleries

### **Desktop (1024px+)**
- Multi-column layouts
- Hover states and interactions
- Keyboard navigation support
- Larger modal dialogs
- Advanced filtering sidebar

---

## 🎯 **USER EXPERIENCE FLOWS**

### **New User Onboarding**
1. Welcome Screen → Get Started
2. Registration → Account Creation
3. Preference Setup → Category Selection
4. Summary Screen → Start Bidding
5. Credit Purchase → First Bid

### **Item Listing Flow**
1. Post Tab → Create Listing
2. Item Details → Title, Description
3. Image Upload → Photo Selection
4. Category Selection → Condition, Price
5. Auction Settings → Duration, Terms
6. Review & Submit → Confirmation

### **Bidding Flow**
1. Browse Items → Item Discovery
2. Item Detail → View Details
3. Bid Placement → Amount Input
4. Credit Validation → Balance Check
5. Bid Confirmation → Success
6. Bid Tracking → Status Updates

### **Credit Purchase Flow**
1. Credits Tab → Buy Credits
2. Package Selection → Choose Amount
3. Payment Method → GCash/Maya
4. Payment Processing → Secure Transaction
5. Code Delivery → Email/SMS
6. Code Redemption → Balance Update

---

## 🔧 **TECHNICAL IMPLEMENTATION NOTES**

### **State Management**
- User authentication state
- Credit balance state
- Active bids state
- Item browsing state
- Notification preferences state

### **Data Flow**
- User actions → State updates → UI refresh
- API calls → Loading states → Success/Error handling
- Real-time updates → WebSocket connections → Live updates

### **Performance Considerations**
- Image lazy loading and compression
- List virtualization for large datasets
- Caching for frequently accessed data
- Offline support for basic functionality
- Progressive loading for better UX

### **Accessibility Features**
- Screen reader support
- High contrast mode
- Large text support
- Voice navigation
- Keyboard navigation
- Focus management

---

This design backbone provides a comprehensive foundation for the BidHub mobile bidding platform, ensuring consistency, scalability, and excellent user experience across all features and interactions.

---

**Commit Message:**
```
feat: create comprehensive design backbone skeleton for BidHub mobile app

- Complete color system with primary (#007AFF), secondary (#8E8E93), and tertiary (#34C759) colors
- Comprehensive screen wireframes for all major app flows (auth, main app, onboarding, payment)
- Core layout components including auction cards, navigation, and Material Design elements
- Onboarding flow with welcome screen, preference setup, and summary screens
- Conversion funnel with 3-tier credit packages and trust elements
- Custom BidHub features including credit system, alias privacy, and auction timers
- Design principles following Material Design 3 with 8dp grid system and accessibility standards
- Component library with buttons, inputs, cards, navigation, and feedback components
- Responsive design breakpoints for mobile, tablet, and desktop
- User experience flows for onboarding, listing, bidding, and credit purchase
- Technical implementation notes for state management, data flow, and performance
```
