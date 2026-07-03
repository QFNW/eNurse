/**
 * E2E tests for Auth module — run against live server at http://localhost:3000
 * Prerequisites: docker compose up -d + prisma migrate dev + prisma db seed
 * Run: npx jest --config test/jest-e2e.json --testPathPattern="auth"
 */
import axios from 'axios';

const BASE = 'http://localhost:3000/api/v1';

let patientToken = '';
let patientRefresh = '';
let patientId = '';

describe('Auth E2E', () => {
  const email = `e2e_${Date.now()}@test.com`;
  const password = 'E2eTest123';

  test('POST /auth/register — creates patient account', async () => {
    const { data, status } = await axios.post(`${BASE}/auth/register`, {
      email,
      password,
      role: 'PATIENT',
    });
    expect(status).toBe(201);
    expect(data.accessToken).toBeTruthy();
    expect(data.refreshToken).toBeTruthy();
    expect(data.user.role).toBe('PATIENT');
    patientToken = data.accessToken;
    patientRefresh = data.refreshToken;
    patientId = data.user.id;
  });

  test('POST /auth/register — forces PATIENT role even if ADMIN requested', async () => {
    const { data } = await axios.post(`${BASE}/auth/register`, {
      email: `admin_attempt_${Date.now()}@test.com`,
      password,
      role: 'ADMIN',
    });
    expect(data.user.role).toBe('PATIENT');
  });

  test('POST /auth/register — rejects duplicate email', async () => {
    await expect(
      axios.post(`${BASE}/auth/register`, { email, password, role: 'PATIENT' }),
    ).rejects.toMatchObject({ response: { status: 409 } });
  });

  test('POST /auth/register — validates input', async () => {
    await expect(
      axios.post(`${BASE}/auth/register`, { email: 'bademail', password: '12' }),
    ).rejects.toMatchObject({ response: { status: 400 } });
  });

  test('POST /auth/login — logs in with valid credentials', async () => {
    const { data, status } = await axios.post(`${BASE}/auth/login`, {
      email,
      password,
    });
    expect(status).toBe(200);
    expect(data.accessToken).toBeTruthy();
    patientToken = data.accessToken;
    patientRefresh = data.refreshToken;
  });

  test('POST /auth/login — rejects wrong password', async () => {
    await expect(
      axios.post(`${BASE}/auth/login`, { email, password: 'wrong' }),
    ).rejects.toMatchObject({ response: { status: 401 } });
  });

  test('POST /auth/refresh — gets new token pair', async () => {
    const { data, status } = await axios.post(`${BASE}/auth/refresh`, {
      refreshToken: patientRefresh,
    });
    expect(status).toBe(200);
    expect(data.accessToken).toBeTruthy();
    expect(data.accessToken).not.toBe(patientToken);
    patientToken = data.accessToken;
    patientRefresh = data.refreshToken;
  });

  test('POST /auth/logout — blacklists refresh token', async () => {
    const { status } = await axios.post(`${BASE}/auth/logout`,
      { refreshToken: patientRefresh },
      { headers: { Authorization: `Bearer ${patientToken}` } },
    );
    expect(status).toBe(200);

    // Verify old refresh token is rejected
    await expect(
      axios.post(`${BASE}/auth/refresh`, { refreshToken: patientRefresh }),
    ).rejects.toMatchObject({ response: { status: 401 } });
  });

  test('GET /auth/protected — 401 without token', async () => {
    await expect(axios.get(`${BASE}/users/me`))
      .rejects.toMatchObject({ response: { status: 401 } });
  });
});
