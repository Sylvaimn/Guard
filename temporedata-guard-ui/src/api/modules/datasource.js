import api from '../index'

export const datasourceApi = {
  list: () => api.get('/api/datasource').then(r => r.data.data),
  create: (data) => api.post('/api/datasource', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/datasource/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/datasource/${id}`).then(r => r.data.data),
  test: (id) => api.post(`/api/datasource/${id}/test`).then(r => r.data.data),
  plugins: () => api.get('/api/datasource/plugins').then(r => r.data.data),
  meta: (id) => api.get(`/api/datasource/${id}/meta`).then(r => r.data.data),
  query: (id, sql) => api.post(`/api/datasource/${id}/query`, { sql }).then(r => r.data.data),
  execute: (id, sql) => api.post(`/api/datasource/${id}/execute`, { sql }).then(r => r.data.data)
}
