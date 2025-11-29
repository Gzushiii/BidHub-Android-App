/**
 * Basic health check test
 * Tests the /api/health endpoint
 */

const request = require('supertest');
const app = require('../src/server');

describe('Health Check Endpoint', () => {
  it('should return 200 and health status', async () => {
    const response = await request(app)
      .get('/api/health')
      .expect(200);

    expect(response.body).toHaveProperty('status');
    expect(response.body).toHaveProperty('timestamp');
    expect(response.body).toHaveProperty('database');
  });
});

