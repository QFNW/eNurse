import client from './client';

export const adminLogin = (password: string) =>
  client.post('/admin/login', { password }).then((r) => r.data);

export const getUsers = (params?: { search?: string; page?: number; pageSize?: number }) =>
  client.get('/admin/users', { params }).then((r) => r.data);

export const updateUserRole = (userId: string, role: string) =>
  client.put(`/admin/users/${userId}/role`, { role }).then((r) => r.data);

export const disableUser = (userId: string) =>
  client.delete(`/admin/users/${userId}`).then((r) => r.data);

export const getGroups = (params?: { page?: number; pageSize?: number }) =>
  client.get('/admin/groups', { params }).then((r) => r.data);

export const getStats = () =>
  client.get('/admin/stats').then((r) => r.data);
