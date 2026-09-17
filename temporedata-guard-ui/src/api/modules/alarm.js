import api from '../index'

export const alarmApi = {
  list: () => api.get('/api/alarm/config').then(r => r.data?.data || r.data),
  create: (data) => api.post('/api/alarm/config', data).then(r => r.data?.data || r.data),
  update: (id, data) => api.put(`/api/alarm/config/${id}`, data).then(r => r.data?.data || r.data),
  delete: (id) => api.delete(`/api/alarm/config/${id}`).then(r => r.data?.data || r.data),
  detail: (id) => api.get(`/api/alarm/config/${id}`).then(r => r.data?.data || r.data),
  records: (id) => api.get(`/api/alarm/config/${id}/records`).then(r => r.data?.data || r.data),
  test: (id) => api.post(`/api/alarm/config/${id}/test`).then(r => r.data?.data || r.data)
}