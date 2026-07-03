/**
 * E2E tests for BM (bowel movement) module
 */
import axios from 'axios';

const BASE = 'http://localhost:3000/api/v1';
let patientToken = '';

describe('BM E2E', () => {
  beforeAll(async () => {
    const email = `bm_e2e_${Date.now()}@test.com`;
    const { data } = await axios.post(`${BASE}/auth/register`, {
      email, password: 'BmTest123', role: 'PATIENT',
    });
    patientToken = data.accessToken;
  });

  test('GET /bm/settings — returns default settings', async () => {
    const { data, status } = await axios.get(`${BASE}/bm/settings`, {
      headers: { Authorization: `Bearer ${patientToken}` },
    });
    expect(status).toBe(200);
    expect(data.intervalDays).toBe(1);
    expect(data.reminderTime).toBe('08:00');
    expect(data.isEnabled).toBe(true);
  });

  test('PUT /bm/settings — updates settings', async () => {
    const { data } = await axios.put(`${BASE}/bm/settings`,
      { intervalDays: 2, reminderTime: '09:00' },
      { headers: { Authorization: `Bearer ${patientToken}` } },
    );
    expect(data.intervalDays).toBe(2);
    expect(data.reminderTime).toBe('09:00');
  });

  test('POST /bm/records — creates a record', async () => {
    const { data, status } = await axios.post(`${BASE}/bm/records`,
      { recordedAt: '2026-07-03T10:00:00', notes: '正常' },
      { headers: { Authorization: `Bearer ${patientToken}` } },
    );
    expect(status).toBe(201);
    expect(data.id).toBeTruthy();
    expect(data.notes).toBe('正常');
  });

  test('GET /bm/records — lists records', async () => {
    const { data, status } = await axios.get(`${BASE}/bm/records`, {
      headers: { Authorization: `Bearer ${patientToken}` },
    });
    expect(status).toBe(200);
    expect(data.records.length).toBeGreaterThanOrEqual(1);
    expect(data.total).toBeGreaterThanOrEqual(1);
  });

  test('GET /bm/calendar — returns month view', async () => {
    const { data, status } = await axios.get(
      `${BASE}/bm/calendar?year=2026&month=7`,
      { headers: { Authorization: `Bearer ${patientToken}` } },
    );
    expect(status).toBe(200);
    expect(data.days).toHaveLength(31);
    const day3 = data.days[2]; // July 3
    expect(day3.hasRecord).toBe(true);
    expect(day3.records[0].notes).toBe('正常');
  });

  test('GET /bm/records/:id — gets single record', async () => {
    const list = await axios.get(`${BASE}/bm/records`, {
      headers: { Authorization: `Bearer ${patientToken}` },
    });
    const recordId = list.data.records[0].id;

    const { data, status } = await axios.get(`${BASE}/bm/records/${recordId}`, {
      headers: { Authorization: `Bearer ${patientToken}` },
    });
    expect(status).toBe(200);
    expect(data.id).toBe(recordId);
  });

  test('PUT /bm/records/:id — updates notes', async () => {
    const list = await axios.get(`${BASE}/bm/records`, {
      headers: { Authorization: `Bearer ${patientToken}` },
    });
    const recordId = list.data.records[0].id;

    const { data } = await axios.put(`${BASE}/bm/records/${recordId}`,
      { notes: '更新备注' },
      { headers: { Authorization: `Bearer ${patientToken}` } },
    );
    expect(data.notes).toBe('更新备注');
  });

  test('DELETE /bm/records/:id — deletes record', async () => {
    // Create a fresh record to delete
    const rec = await axios.post(`${BASE}/bm/records`,
      { recordedAt: '2026-07-01T08:00:00' },
      { headers: { Authorization: `Bearer ${patientToken}` } },
    );
    const { status } = await axios.delete(`${BASE}/bm/records/${rec.data.id}`, {
      headers: { Authorization: `Bearer ${patientToken}` },
    });
    expect(status).toBe(200);
  });

  test('POST /bm/records — rejects invalid date (400 not 500)', async () => {
    await expect(
      axios.post(`${BASE}/bm/records`,
        { recordedAt: 'not-a-date' },
        { headers: { Authorization: `Bearer ${patientToken}` } },
      ),
    ).rejects.toMatchObject({ response: { status: 400 } });
  });
});
