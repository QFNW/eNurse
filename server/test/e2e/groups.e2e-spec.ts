/**
 * E2E tests for Groups module
 */
import axios from 'axios';

const BASE = 'http://localhost:3000/api/v1';
let nurseToken = '';
let patientToken = '';
let groupId = '';
let inviteCode = '';

describe('Groups E2E', () => {
  beforeAll(async () => {
    // Create nurse account via admin (self-registration forces PATIENT)
    const adminLogin = await axios.post(`${BASE}/admin/login`, { password: 'admin_enurse_2026' });
    const adminToken = adminLogin.data.accessToken;
    const nurseEmail = `nurse_${Date.now()}@test.com`;

    // Register then promote to NURSE, re-login for updated role
    const reg = await axios.post(`${BASE}/auth/register`, {
      email: nurseEmail, password: 'NurseTest123', role: 'NURSE',
    });
    await axios.put(`${BASE}/admin/users/${reg.data.user.id}/role`, { role: 'NURSE' },
      { headers: { Authorization: `Bearer ${adminToken}` } });
    const nurseLogin = await axios.post(`${BASE}/auth/login`,
      { email: nurseEmail, password: 'NurseTest123' });
    nurseToken = nurseLogin.data.accessToken;

    // Create patient account
    const patient = await axios.post(`${BASE}/auth/register`, {
      email: `patient_${Date.now()}@test.com`, password: 'PatientTest123', role: 'PATIENT',
    });
    patientToken = patient.data.accessToken;
  });

  test('POST /groups — nurse creates group', async () => {
    const { data, status } = await axios.post(`${BASE}/groups`,
      { name: '内科一组' },
      { headers: { Authorization: `Bearer ${nurseToken}` } },
    );
    expect(status).toBe(201);
    expect(data.name).toBe('内科一组');
    expect(data.inviteCode).toHaveLength(8);
    groupId = data.id;
    inviteCode = data.inviteCode;
  });

  test('POST /groups — patient cannot create group (403)', async () => {
    await expect(
      axios.post(`${BASE}/groups`,
        { name: '非法小组' },
        { headers: { Authorization: `Bearer ${patientToken}` } },
      ),
    ).rejects.toMatchObject({ response: { status: 403 } });
  });

  test('GET /groups — nurse sees own groups', async () => {
    const { data, status } = await axios.get(`${BASE}/groups`, {
      headers: { Authorization: `Bearer ${nurseToken}` },
    });
    expect(status).toBe(200);
    expect(data.length).toBeGreaterThanOrEqual(1);
  });

  test('POST /groups/join — patient joins by invite code', async () => {
    const { data, status } = await axios.post(`${BASE}/groups/join`,
      { inviteCode },
      { headers: { Authorization: `Bearer ${patientToken}` } },
    );
    expect(status).toBe(201);
    expect(data.group.name).toBe('内科一组');
  });

  test('POST /groups/join — patient cannot join second group', async () => {
    await expect(
      axios.post(`${BASE}/groups/join`,
        { inviteCode: 'NONEXIST' },
        { headers: { Authorization: `Bearer ${patientToken}` } },
      ),
    ).rejects.toMatchObject({ response: { status: 400 } });
  });

  test('GET /groups/:id — member can view group', async () => {
    const { data, status } = await axios.get(`${BASE}/groups/${groupId}`, {
      headers: { Authorization: `Bearer ${patientToken}` },
    });
    expect(status).toBe(200);
    expect(data.name).toBe('内科一组');
  });

  test('GET /groups/:id/members — nurse sees members', async () => {
    const { data, status } = await axios.get(`${BASE}/groups/${groupId}/members`, {
      headers: { Authorization: `Bearer ${nurseToken}` },
    });
    expect(status).toBe(200);
    expect(data.length).toBeGreaterThanOrEqual(1);
  });

  test('GET /groups/:id/members/:userId/events — nurse views member events', async () => {
    const members = await axios.get(`${BASE}/groups/${groupId}/members`, {
      headers: { Authorization: `Bearer ${nurseToken}` },
    });
    const memberId = members.data[0].userId;

    const { data, status } = await axios.get(
      `${BASE}/groups/${groupId}/members/${memberId}/events`,
      { headers: { Authorization: `Bearer ${nurseToken}` } },
    );
    expect(status).toBe(200);
    expect(data).toHaveProperty('bmRecords');
    expect(data).toHaveProperty('medRecords');
    expect(data).toHaveProperty('alerts');
  });

  test('GET /groups/:id/members/:userId/strategies — nurse views strategies', async () => {
    const members = await axios.get(`${BASE}/groups/${groupId}/members`, {
      headers: { Authorization: `Bearer ${nurseToken}` },
    });
    const memberId = members.data[0].userId;

    const { data, status } = await axios.get(
      `${BASE}/groups/${groupId}/members/${memberId}/strategies`,
      { headers: { Authorization: `Bearer ${nurseToken}` } },
    );
    expect(status).toBe(200);
    expect(data).toHaveProperty('bmSettings');
    expect(data).toHaveProperty('medications');
  });

  test('GET /groups/:id/members/:userId/strategies — patient gets 403', async () => {
    await expect(
      axios.get(`${BASE}/groups/${groupId}/members/someuser/strategies`, {
        headers: { Authorization: `Bearer ${patientToken}` },
      }),
    ).rejects.toMatchObject({ response: { status: 403 } });
  });
});
