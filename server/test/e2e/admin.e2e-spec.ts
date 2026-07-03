/**
 * E2E tests for Admin module
 */
import axios from 'axios';

const BASE = 'http://localhost:3000/api/v1';
let adminToken = '';

describe('Admin E2E', () => {
  beforeAll(async () => {
    const { data } = await axios.post(`${BASE}/admin/login`, {
      password: 'admin_enurse_2026',
    });
    adminToken = data.accessToken;
  });

  test('POST /admin/login — admin login succeeds', async () => {
    expect(adminToken).toBeTruthy();
  });

  test('POST /admin/login — wrong password rejected', async () => {
    await expect(
      axios.post(`${BASE}/admin/login`, { password: 'wrong' }),
    ).rejects.toMatchObject({ response: { status: 401 } });
  });

  test('GET /admin/users — lists users', async () => {
    const { data, status } = await axios.get(`${BASE}/admin/users`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    expect(status).toBe(200);
    expect(data.total).toBeGreaterThanOrEqual(1);
    expect(data.users.length).toBeGreaterThanOrEqual(1);
  });

  test('GET /admin/users?search= — filters users', async () => {
    const { data } = await axios.get(`${BASE}/admin/users?search=admin`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    expect(data.users.some((u: any) => u.email.includes('admin'))).toBe(true);
  });

  test('PUT /admin/users/:id/role — changes role', async () => {
    // Create a test user first
    const reg = await axios.post(`${BASE}/auth/register`, {
      email: `role_test_${Date.now()}@test.com`,
      password: 'Test12345',
      role: 'PATIENT',
    });
    const userId = reg.data.user.id;

    const { data, status } = await axios.put(
      `${BASE}/admin/users/${userId}/role`,
      { role: 'NURSE' },
      { headers: { Authorization: `Bearer ${adminToken}` } },
    );
    expect(status).toBe(200);
    expect(data.role).toBe('NURSE');
  });

  test('PUT /admin/users/:id/role — rejects invalid role (400)', async () => {
    const users = await axios.get(`${BASE}/admin/users`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    const userId = users.data.users[0].id;

    await expect(
      axios.put(`${BASE}/admin/users/${userId}/role`,
        { role: 'INVALID' },
        { headers: { Authorization: `Bearer ${adminToken}` } },
      ),
    ).rejects.toMatchObject({ response: { status: 400 } });
  });

  test('GET /admin/stats — returns system statistics', async () => {
    const { data, status } = await axios.get(`${BASE}/admin/stats`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    expect(status).toBe(200);
    expect(data.users.total).toBeGreaterThanOrEqual(1);
    expect(data).toHaveProperty('groups');
    expect(data).toHaveProperty('records');
    expect(data).toHaveProperty('alerts');
  });

  test('GET /admin/groups — lists all groups', async () => {
    const { data, status } = await axios.get(`${BASE}/admin/groups`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    expect(status).toBe(200);
    expect(data).toHaveProperty('groups');
    expect(data).toHaveProperty('total');
  });

  test('Admin endpoints — patient cannot access (403)', async () => {
    const reg = await axios.post(`${BASE}/auth/register`, {
      email: `pat_${Date.now()}@test.com`,
      password: 'Test12345',
      role: 'PATIENT',
    });
    await expect(
      axios.get(`${BASE}/admin/users`, {
        headers: { Authorization: `Bearer ${reg.data.accessToken}` },
      }),
    ).rejects.toMatchObject({ response: { status: 403 } });
  });
});
