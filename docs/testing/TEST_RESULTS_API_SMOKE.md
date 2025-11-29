# BidHub API Smoke Test Results

**Date:** November 3, 2025  
**Environment:** Render deployment (`https://bidhub-android-app.onrender.com/api`)  
**Script:** `npm run smoke` → `tests/apiSmokeTest.js`

---

## Summary

The automated smoke test targets the most important read-only endpoints plus the authenticated balance check. Credentials used were the seeded test account (`test@example.com / test1234`). All required tests passed within expected latency budgets.

| Test                           | Method | Status | HTTP Code | Notes                        |
|--------------------------------|--------|--------|-----------|------------------------------|
| `/health`                      | GET    | ✅     | 200       | Includes DB/keep-alive status |
| `/items`                       | GET    | ✅     | 200       | Returned active listings      |
| `/categories`                  | GET    | ✅     | 200       | Category catalogue intact     |
| `/auth/login`                  | POST   | ✅     | 200       | Token issued for test user    |
| `/credits/balance`             | GET    | ✅     | 200       | Requires JWT, returned balance|
| `/topups`                      | GET    | ✅     | 200       | Optional check, empty array   |

Execution time (Render free tier cold start considered):

- Health check: **~22.4s** (startup warm-up)
- Authenticated requests: **< 200ms** each

No required tests failed. Optional `/topups` endpoint returned an empty list as expected because no manual top-ups have been submitted for the test user yet.

---

## Running the Smoke Test Locally

```bash
cd bidhub-backend
npm install    # ensures node-fetch dependency
npm run smoke
```

Optional environment variables:

- `BIDHUB_API_BASE_URL` – override the API base URL.
- `BIDHUB_TEST_EMAIL` / `BIDHUB_TEST_PASSWORD` – change login credentials.
- `BIDHUB_ALLOW_MUTATIONS=true` – enable non-destructive POST tests (e.g. creating a pending top-up request).

---

## Follow-up Recommendations

1. **CI Integration** – Add `npm run smoke` to the deployment pipeline to detect regressions automatically.
2. **Extended Coverage** – When a staging database is available, enable mutation tests to validate:
   - `POST /bids/place`
   - `POST /items/:id/buy-now`
   - `POST /topups`
3. **Alerting** – Combine the smoke test with monitoring (status page checks) to catch Render cold starts early.

---

**Result:** Smoke suite confirms key endpoints are reachable and responding correctly. No immediate API availability issues detected.***

