# BidHub API Specification - Manual Top-Up Endpoints

**Version**: 1.0  
**Date**: November 3, 2025  
**Base URL**: `https://bidhub-android-app.onrender.com/api`

---

## Authentication

All authenticated endpoints require JWT token in Authorization header:

```
Authorization: Bearer <JWT_TOKEN>
```

---

## Top-Up Endpoints

### POST /topups

Initiate a new manual top-up request.

**Authentication**: Required  
**Request Body**:
```json
{
  "amount": 500.00,
  "payment_method": "gcash"
}
```

**Request Parameters**:
- `amount` (number, required): Top-up amount (100.00 - 50000.00)
- `payment_method` (string, required): One of: `gcash`, `maya`, `bank_transfer`, `other`

**Success Response** (201 Created):
```json
{
  "success": true,
  "topup_id": 123,
  "generated_ref": "TOPUP202411030001",
  "instructions": "Please pay ₱500.00 to GCASH number +63 916 123 4567 with reference code TOPUP202411030001",
  "payment_number": "+63 916 123 4567",
  "amount": 500.00,
  "payment_method": "gcash",
  "status": "PENDING",
  "qr_code_data": "data:image/svg+xml;base64,..."
}
```

**Error Responses**:

400 Bad Request - Invalid amount:
```json
{
  "error": "Invalid amount",
  "details": "Amount must be between 100 and 50000 PHP"
}
```

400 Bad Request - Invalid payment method:
```json
{
  "error": "Invalid payment method",
  "details": "Payment method must be one of: gcash, maya, bank_transfer, other"
}
```

500 Internal Server Error - Reference generation failed:
```json
{
  "error": "Failed to generate reference code",
  "details": "Please try again"
}
```

---

### POST /topups/:id/submit

Submit receipt reference for a pending top-up.

**Authentication**: Required  
**Path Parameters**:
- `id` (integer): Top-up ID

**Request Body**:
```json
{
  "user_receipt_ref": "RECEIPT1234567890"
}
```

**Request Parameters**:
- `user_receipt_ref` (string, required): Receipt reference from payment (min 4 chars)

**Success Response** (200 OK):
```json
{
  "success": true,
  "status": "UNDER_REVIEW",
  "message": "Top-up submitted for review"
}
```

**Error Responses**:

400 Bad Request - Invalid status:
```json
{
  "error": "Invalid status transition",
  "details": "Cannot submit receipt for top-up in CONFIRMED status"
}
```

404 Not Found:
```json
{
  "error": "Top-up not found",
  "details": "Invalid top-up ID or you do not own this top-up"
}
```

---

### GET /topups/:id

Get top-up details and current status.

**Authentication**: Required  
**Path Parameters**:
- `id` (integer): Top-up ID

**Success Response** (200 OK):
```json
{
  "id": 123,
  "amount": 500.00,
  "currency": "PHP",
  "generated_ref": "TOPUP202411030001",
  "user_receipt_ref": "RECEIPT1234567890",
  "payment_method": "gcash",
  "payment_number": "+63 916 123 4567",
  "status": "UNDER_REVIEW",
  "created_at": "2024-11-03T10:30:00Z",
  "submitted_at": "2024-11-03T10:32:00Z",
  "confirmed_at": null,
  "rejected_at": null,
  "rejection_reason": null
}
```

**Error Responses**:

404 Not Found:
```json
{
  "error": "Top-up not found",
  "details": "Invalid top-up ID or you do not own this top-up"
}
```

---

### GET /topups

Get user's top-up history.

**Authentication**: Required  
**Query Parameters**:
- `status` (string, optional): Filter by status (`PENDING`, `UNDER_REVIEW`, `CONFIRMED`, `REJECTED`)
- `limit` (integer, optional, default: 20): Number of results per page
- `offset` (integer, optional, default: 0): Pagination offset

**Success Response** (200 OK):
```json
{
  "topups": [
    {
      "id": 123,
      "amount": 500.00,
      "generated_ref": "TOPUP202411030001",
      "payment_method": "gcash",
      "status": "CONFIRMED",
      "created_at": "2024-11-03T10:30:00Z",
      "confirmed_at": "2024-11-03T11:00:00Z"
    }
  ],
  "total": 5,
  "limit": 20,
  "offset": 0
}
```

---

### POST /topups/admin/:id/confirm

Admin confirms top-up and credits user account.

**Authentication**: Required (Admin only)  
**Path Parameters**:
- `id` (integer): Top-up ID

**Success Response** (200 OK):
```json
{
  "success": true,
  "new_balance": 1500.00,
  "message": "Top-up confirmed and credits added"
}
```

**Error Responses**:

400 Bad Request - Invalid status:
```json
{
  "error": "Invalid status",
  "details": "Cannot confirm top-up in PENDING status"
}
```

404 Not Found:
```json
{
  "error": "Top-up not found",
  "details": "Invalid top-up ID"
}
```

500 Internal Server Error - Transaction failed:
```json
{
  "error": "Failed to confirm top-up",
  "details": "Transaction rollback occurred"
}
```

---

### POST /topups/admin/:id/reject

Admin rejects top-up request.

**Authentication**: Required (Admin only)  
**Path Parameters**:
- `id` (integer): Top-up ID

**Request Body**:
```json
{
  "reason": "Receipt reference doesn't match payment records"
}
```

**Request Parameters**:
- `reason` (string, required): Rejection reason (min 10 chars)

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "Top-up rejected"
}
```

**Error Responses**:

400 Bad Request - Invalid reason:
```json
{
  "error": "Invalid rejection reason",
  "details": "Reason must be at least 10 characters"
}
```

404 Not Found:
```json
{
  "error": "Top-up not found or not in valid status",
  "details": "Cannot reject this top-up"
}
```

---

### GET /topups/admin/pending

Get pending top-ups requiring admin review.

**Authentication**: Required (Admin only)  
**Query Parameters**:
- `limit` (integer, optional, default: 50): Number of results per page
- `offset` (integer, optional, default: 0): Pagination offset

**Success Response** (200 OK):
```json
{
  "topups": [
    {
      "id": 123,
      "user_id": 456,
      "username": "john_doe",
      "alias": "JohnD",
      "email": "john@example.com",
      "amount": 500.00,
      "generated_ref": "TOPUP202411030001",
      "user_receipt_ref": "RECEIPT1234567890",
      "payment_method": "gcash",
      "status": "UNDER_REVIEW",
      "created_at": "2024-11-03T10:30:00Z",
      "submitted_at": "2024-11-03T10:32:00Z",
      "hours_waiting": 2
    }
  ],
  "total": 10,
  "limit": 50,
  "offset": 0
}
```

---

## Existing Endpoints

### Authentication

**POST /auth/register**
- Register new user
- Returns JWT token

**POST /auth/login**
- User login
- Returns JWT token

### Credits

**GET /credits/balance**
- Get user credit balance
- Returns balance + recent transactions

**GET /credits/transactions**
- Get transaction history
- Supports filtering by type/status

**POST /credits/purchase**
- Legacy purchase endpoint (still supported)
- Auto-confirms credits

### Items

**GET /items**
- List all active items
- Supports pagination and filtering

**GET /items/:id**
- Get specific item details

**POST /items**
- Create new item
- Returns item ID

**POST /items/:id/publish**
- Publish draft item
- Changes status to 'active'

### Bids

**POST /bids/place**
- Place a bid on item
- Validates credits and bid amount
- Returns bid confirmation

---

## Error Response Format

All errors follow this format:

```json
{
  "error": "Error Type",
  "details": "Human-readable error message",
  "correlationId": "optional-correlation-id"
}
```

**Common HTTP Status Codes**:
- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Missing or invalid token
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists
- `500 Internal Server Error` - Server error

---

## Rate Limiting

- **Limit**: 100 requests per 15 minutes per IP
- **Headers**:
  - `X-RateLimit-Limit`: 100
  - `X-RateLimit-Remaining`: 95
  - `X-RateLimit-Reset`: 1699001234

---

## CORS

- **Allowed Origins**: All (*)
- **Methods**: GET, POST, PUT, DELETE, OPTIONS
- **Credentials**: Supported

---

## Security

- **Password Hashing**: bcrypt with 8 rounds
- **JWT Expiry**: 7 days
- **Token Format**: `Bearer <token>`
- **HTTPS**: Required in production
- **Input Validation**: All inputs validated with Joi

---

## Version History

- **v1.0** (2025-11-03): Initial manual top-up system release

---

**For complete API documentation**, see:
- [Manual Top-Up README](./payments/manual_topup_README.md)
- [Sequence Flow](./payments/manual_topup_sequence.md)
- [Database Schema](./payments/manual_topup_schema.sql)

