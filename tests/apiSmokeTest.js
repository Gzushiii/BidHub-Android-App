#!/usr/bin/env node
/**
 * Simple smoke test for BidHub REST API.
 * Performs read-only checks by default. Set BIDHUB_ALLOW_MUTATIONS=true
 * to enable non-destructive POST tests (e.g. initiating a top-up).
 */

async function ensureFetch() {
  if (typeof fetch === 'undefined') {
    const { default: fetchFn } = await import('node-fetch');
    globalThis.fetch = fetchFn;
  }
}

const BASE_URL = process.env.BIDHUB_API_BASE_URL || 'https://bidhub-android-app.onrender.com/api';
const TEST_EMAIL = process.env.BIDHUB_TEST_EMAIL || 'test@example.com';
const TEST_PASSWORD = process.env.BIDHUB_TEST_PASSWORD || 'test1234';
const ALLOW_MUTATIONS = /^true$/i.test(process.env.BIDHUB_ALLOW_MUTATIONS || 'false');

const tests = [];
let authToken = null;

async function request(method, path, { body, headers = {}, auth = false } = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 60000); // 60s for cold starts

  const options = {
    method,
    headers: {
      'Accept': 'application/json',
      ...headers
    },
    signal: controller.signal
  };

  if (auth) {
    if (!authToken) {
      throw new Error('Auth token not available for authenticated request');
    }
    options.headers['Authorization'] = `Bearer ${authToken}`;
  }

  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }

  try {
    const response = await fetch(`${BASE_URL}${path}`, options);
    const text = await response.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch (parseErr) {
      data = text;
    }

    return {
      ok: response.ok,
      status: response.status,
      statusText: response.statusText,
      data
    };
  } catch (err) {
    return {
      ok: false,
      status: 0,
      statusText: err.name,
      data: { error: err.message || 'Request failed' }
    };
  } finally {
    clearTimeout(timeout);
  }
}

async function runTest(name, fn, { required = true } = {}) {
  const start = Date.now();
  try {
    const result = await fn();
    const duration = Date.now() - start;
    const testResult = {
      name,
      duration,
      required,
      success: result.ok,
      status: result.status,
      statusText: result.statusText,
      data: result.data
    };
    tests.push(testResult);
    return testResult;
  } catch (err) {
    const duration = Date.now() - start;
    const testResult = {
      name,
      duration,
      required,
      success: false,
      status: 0,
      statusText: err.name,
      data: { error: err.message || 'Unexpected error' }
    };
    tests.push(testResult);
    return testResult;
  }
}

async function main() {
  await ensureFetch();

  console.log('=======================================================');
  console.log('BidHub API Smoke Test');
  console.log('Base URL:', BASE_URL);
  console.log('Allow mutations:', ALLOW_MUTATIONS);
  console.log('=======================================================\n');

  await runTest('GET /health', () => request('GET', '/health'));
  await runTest('GET /items', () => request('GET', '/items'));
  await runTest('GET /categories', () => request('GET', '/categories'));

  // Attempt login
  const loginResult = await runTest(
    'POST /auth/login',
    () =>
      request('POST', '/auth/login', {
        body: {
          email: TEST_EMAIL,
          password: TEST_PASSWORD
        }
      }),
    { required: false }
  );

  if (loginResult.success && loginResult.data && loginResult.data.token) {
    authToken = loginResult.data.token;
    console.log('\nAuthenticated as:', TEST_EMAIL);
  } else {
    console.warn('\nWarning: Login failed. Authenticated tests will be skipped.');
  }

  if (authToken) {
    await runTest('GET /credits/balance', () => request('GET', '/credits/balance', { auth: true }));
    await runTest('GET /topups', () => request('GET', '/topups', { auth: true }), { required: false });

    if (ALLOW_MUTATIONS) {
      await runTest(
        'POST /topups (dry run)',
        () =>
          request(
            'POST',
            '/topups',
            {
              auth: true,
              body: {
                amount: 100,
                payment_method: 'gcash'
              }
            }
          ),
        { required: false }
      );
    }
  }

  console.log('\n=======================================================');
  console.log('Smoke Test Summary');
  console.log('=======================================================\n');

  let requiredFailures = 0;
  for (const t of tests) {
    const statusLabel = t.success ? 'PASS' : 'FAIL';
    const requirement = t.required ? '[required]' : '[optional]';
    console.log(
      `${statusLabel.padEnd(5)} ${requirement} ${t.name} (${t.duration}ms) -> HTTP ${t.status} ${t.statusText}`
    );
    if (!t.success && t.data) {
      console.log('      Response:', JSON.stringify(t.data).slice(0, 500));
    }
    if (!t.success && t.required) {
      requiredFailures += 1;
    }
  }

  console.log('\nTotal tests:', tests.length);
  console.log('Required failures:', requiredFailures);

  if (requiredFailures > 0) {
    console.error('\nOne or more required smoke tests failed.');
    process.exitCode = 1;
  } else {
    console.log('\nAll required smoke tests passed.');
  }
}

main().catch((err) => {
  console.error('Smoke test failed with unexpected error:', err);
  process.exitCode = 1;
});

