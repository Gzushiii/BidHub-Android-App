# Frontend/Backend Integration Solutions

**Created**: November 3, 2025  
**Project**: BidHub - Manual Top-Up System Integration  
**Status**: In Progress

---

## Executive Summary

This document outlines the comprehensive solutions for fixing frontend/backend integration issues and implementing the manual top-up payment system for BidHub. The goal is to achieve seamless communication between the Android app and Node.js/MySQL backend hosted on Render, with a simplified QR/reference code payment flow.

---

## Phase 1: Manual Top-Up System Implementation ✅ COMPLETED

### Database Schema ✅

**Files Created**:
- `docs/payments/manual_topup_schema.sql` - Complete schema with tables, views, procedures
- `docs/payments/manual_topup_README.md` - Comprehensive documentation
- `docs/payments/manual_topup_sequence.md` - Flow diagrams and sequence charts

**Key Components**:

1. **`topups` Table**: Tracks manual top-up requests
   - Generates unique reference codes
   - Stores user receipt references
   - Manages status transitions (PENDING → UNDER_REVIEW → CONFIRMED/REJECTED)
   - Audit trail with timestamps

2. **`credit_ledger` Table**: Immutable audit trail
   - Records every credit change
   - Balances before/after transactions
   - Links to topups, bids, refunds
   - Essential for reconciliation

3. **Stored Procedures**:
   - `sp_confirm_topup()`: Atomic confirmation with credit addition
   - `sp_reject_topup()`: Tracks rejection with reasons

4. **Views**:
   - `v_pending_topups`: Admin review queue
   - `v_user_topup_stats`: User statistics
   - `v_credit_ledger_summary`: Transaction summaries

---

### Backend API Implementation ✅

**Files Created**:
- `bidhub-backend/src/routes/topups.js` - Complete REST API implementation

**Endpoints Implemented**:

| Method | Endpoint | Description | Auth | Status |
|--------|----------|-------------|------|--------|
| POST | `/api/topups` | Initiate new top-up | User | ✅ |
| POST | `/api/topups/:id/submit` | Submit receipt reference | User | ✅ |
| GET | `/api/topups/:id` | Get top-up details | User | ✅ |
| GET | `/api/topups` | List user's top-ups | User | ✅ |
| POST | `/api/topups/admin/:id/confirm` | Admin confirm top-up | Admin | ✅ |
| POST | `/api/topups/admin/:id/reject` | Admin reject top-up | Admin | ✅ |
| GET | `/api/topups/admin/pending` | Admin pending queue | Admin | ✅ |

**Features**:
- ✅ Input validation for amounts and payment methods
- ✅ Unique reference code generation with retry logic
- ✅ Atomic transactions for credit additions
- ✅ Audit ledger entries for every change
- ✅ Admin approval workflow
- ✅ Comprehensive error handling

---

### Keep-Alive Service ✅

**Files Created**:
- `bidhub-backend/src/services/keepAlive.js` - Prevent Render cold starts

**Implementation**:
- Periodic database pings every 5 minutes
- Configurable via environment variables
- Graceful shutdown handling
- Health endpoint monitoring

**Configuration**:
```bash
KEEP_ALIVE_ENABLED=true
KEEP_ALIVE_INTERVAL_MS=300000  # 5 minutes
```

---

### Server Updates ✅

**Files Modified**:
- `bidhub-backend/src/server.js`:
  - ✅ Added topups route registration
  - ✅ Integrated keep-alive service
  - ✅ Enhanced health check endpoint
  - ✅ Added graceful shutdown handlers

**Files Fixed**:
- `bidhub-backend/src/middleware/auth.js`:
  - ✅ Fixed database pool import (was using incorrect `db` variable)
  - ✅ Updated all queries to use `pool`

---

## Phase 2: Frontend/Backend Contract Alignment 🔄 IN PROGRESS

### API Contract Analysis

**Current Status**: Need to verify exact request/response formats

**Auth Endpoints**:
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- Expected response includes JWT token

**Items Endpoints**:
- `GET /api/items` - List items (with filtering)
- `GET /api/items/:id` - Get specific item
- `POST /api/items` - Create item
- `POST /api/items/:id/publish` - Publish draft item

**Bids Endpoints**:
- `POST /api/bids/place` - Place a bid
- Credit validation and atomic transactions

**Credits Endpoints**:
- `GET /api/credits/balance` - Get user balance
- `GET /api/credits/transactions` - Transaction history
- `POST /api/credits/purchase` - Legacy purchase (still supported)

**New Top-ups Endpoints**:
- All endpoints documented in `manual_topup_README.md`

---

### Category ID Mapping ✅ ALREADY FIXED

**Status**: Resolved in previous commits

- `CategoryMapping.java` utility class created
- Maps Android string IDs to backend integer IDs
- `ItemApiClient.java` updated to use mapping
- All categories now properly synced

---

## Phase 3: Remaining Tasks

### A. OpenAPI Specification Generation ⏳ PENDING

**Task**: Generate OpenAPI 3.1 spec from existing routes

**Approach**:
1. Document all endpoints with request/response schemas
2. Include error responses
3. Add authentication requirements
4. Include examples
5. Generate `docs/openapi.yaml`

**Tools Needed**:
- Manual documentation (no automatic generation available)
- Reference existing route files

---

### B. Android API Client Updates ⏳ PENDING

**Files to Update**:
1. `AuthApiClient.java` - Already has network error handling ✅
2. `ItemApiClient.java` - Already has category mapping ✅
3. `BidApiClient.java` - Already has network error handling ✅
4. **NEW**: `TopupApiClient.java` - Create for manual top-ups

**Tasks**:
- Create `TopupApiClient.java` with all top-up endpoints
- Update `CreditsFragment.java` to use new top-up API
- Implement retry logic with exponential backoff
- Add "Connecting..." UI states
- Handle cold start gracefully

---

### C. Database Synchronization Strategy ⏳ PENDING

**Current Issue**: Android uses SQLite, backend uses MySQL

**Recommended Solution**: Make MySQL single source of truth

**Approach**:
1. Keep SQLite for offline caching only
2. Remove SQLite write operations for user/bid/item data
3. Implement background sync queue
4. Add sync status indicators
5. Handle conflicts gracefully

**Files to Modify**:
- `DatabaseHelper.java` - Mark as read-only for synced data
- Create `SyncManager.java` - Background sync queue
- Update all data access to prefer backend first

---

### D. Token Management ⏳ PENDING

**Current**: Basic JWT with 7-day expiry

**Needed**:
1. Refresh token implementation
2. Token revocation on logout
3. Multi-device tracking
4. Automatic token refresh

**Files to Create/Update**:
- `TokenManager.java` - Android-side token handling
- `bidhub-backend/src/middleware/tokenRefresh.js` - Refresh endpoint
- `revoked_tokens` table - Track invalidated tokens

---

### E. Credit Transaction Integrity ⏳ PARTIALLY COMPLETE

**Current**: Basic transactions exist

**Needed**:
1. ✅ Atomic transactions (already implemented in top-ups)
2. ✅ Credit ledger (already created)
3. ⏳ Optimistic locking for concurrent bids
4. ⏳ Bid concurrency protection
5. ⏳ Stored procedures for bid placement

---

### F. Testing Infrastructure ⏳ PENDING

**Needed**:
1. Jest + Supertest for Node.js backend
2. Integration tests for all endpoints
3. Android instrumentation tests
4. End-to-end flow tests
5. Mock data generators

---

## Implementation Status Summary

| Component | Status | Completion |
|-----------|--------|------------|
| Manual Top-Up Schema | ✅ Complete | 100% |
| Top-Up Documentation | ✅ Complete | 100% |
| Backend Top-Up API | ✅ Complete | 100% |
| Keep-Alive Service | ✅ Complete | 100% |
| Server Integration | ✅ Complete | 100% |
| Auth Middleware Fix | ✅ Complete | 100% |
| OpenAPI Spec | ⏳ Pending | 0% |
| Android Top-Up Client | ⏳ Pending | 0% |
| Database Sync Strategy | ⏳ Pending | 20% |
| Token Refresh | ⏳ Pending | 0% |
| Bid Concurrency | ⏳ Pending | 60% |
| Testing Infrastructure | ⏳ Pending | 0% |

**Overall Progress**: **45% Complete**

---

## Next Steps (Priority Order)

### Immediate (This Week)

1. **Create `TopupApiClient.java`** for Android
2. **Update `CreditsFragment.java`** to use manual top-up API
3. **Implement retry logic** with exponential backoff
4. **Add "Connecting..." UI states** for cold start handling

### Short-Term (Next 2 Weeks)

5. **Generate OpenAPI spec** from existing routes
6. **Verify all API contracts** match documentation
7. **Test manual top-up flow** end-to-end
8. **Deploy to Render** and verify keep-alive works

### Medium-Term (Next Month)

9. **Implement database sync strategy**
10. **Add token refresh mechanism**
11. **Complete bid concurrency protection**
12. **Build test suite**

---

## Testing Checklist

### Manual Top-Up Flow

- [ ] Initiate top-up and receive reference code
- [ ] Display QR code and payment instructions
- [ ] Submit receipt reference
- [ ] Admin reviews and confirms
- [ ] Credits added to account
- [ ] Ledger entries created
- [ ] Transaction history updated

### Edge Cases

- [ ] Duplicate reference code generation
- [ ] Invalid amount validation
- [ ] Missing receipt reference
- [ ] Concurrent confirmation attempts
- [ ] Network interruption during confirmation
- [ ] Timeout after 24 hours
- [ ] Rejection flow
- [ ] Cancel flow

### Integration

- [ ] Cold start handling
- [ ] Keep-alive prevents spin-down
- [ ] Retry logic with backoff
- [ ] Error messages user-friendly
- [ ] Offline mode graceful degradation

---

## Deployment Notes

### Environment Variables Needed

```bash
# Database (existing)
DB_HOST=your-aiven-host.a.aivencloud.com
DB_USER=your-db-user
DB_PASSWORD=your-password
DB_NAME=defaultdb
DB_PORT=12345
DB_SSL=true

# JWT (existing)
JWT_SECRET=your-secret-key

# Keep-Alive (new)
KEEP_ALIVE_ENABLED=true
KEEP_ALIVE_INTERVAL_MS=300000

# Payment Info (new)
PAYMENT_GCASH_NUMBER=+63 916 123 4567
PAYMENT_MAYA_NUMBER=+63 917 789 0123
PAYMENT_BANK_ACCOUNT=1234567890

# Optional
NODE_ENV=production
CORS_ORIGIN=*
```

### Database Migration Steps

1. Run `docs/payments/manual_topup_schema.sql` on Aiven MySQL
2. Verify tables created: `topups`, `credit_ledger`
3. Verify views created: `v_pending_topups`, etc.
4. Verify procedures created: `sp_confirm_topup`, `sp_reject_topup`
5. Test stored procedures with sample data

### Render Deployment

1. Push changes to GitHub
2. Render auto-deploys on push to main branch
3. Verify environment variables set in Render dashboard
4. Check logs for keep-alive service starting
5. Test `/api/health` endpoint includes database status
6. Monitor for cold start issues

---

## Known Issues and Limitations

### Current Limitations

1. **Manual Admin Review**: All top-ups require manual admin confirmation
2. **No Email/SMS**: Notifications not implemented yet
3. **No QR Code Generation**: Placeholder SVG returned
4. **No WebSocket Updates**: Status polling only
5. **No Auto-Confirmation**: All amounts require review

### Future Enhancements

1. Auto-confirmation for amounts < 1000 PHP
2. Email notifications on status changes
3. SMS alerts for large amounts
4. Real QR code generation with payment info
5. WebSocket real-time status updates
6. Fraud detection algorithms

---

## References

- [Manual Top-Up README](./payments/manual_topup_README.md)
- [Database Schema](./payments/manual_topup_schema.sql)
- [Sequence Diagram](./payments/manual_topup_sequence.md)
- [Codebase Issues Analysis](../CODEBASE_ISSUES_ANALYSIS.md)

---

**Document Version**: 1.0  
**Last Updated**: November 3, 2025  
**Next Review**: After Android client implementation

