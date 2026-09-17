import api from '../index'

export const workflowApi = {
  list: () => api.get('/api/workflow').then(r => r.data.data),
  detail: (id) => api.get(`/api/workflow/${id}`).then(r => r.data.data),
  create: (data) => api.post('/api/workflow', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/workflow/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/workflow/${id}`).then(r => r.data.data),
  execute: (id) => api.post(`/api/workflow/${id}/execute`).then(r => r.data.data),
  schedule: (id, data) => api.put(`/api/workflow/${id}/schedule`, data).then(r => r.data),
  disableSchedule: (id) => api.put(`/api/workflow/${id}/schedule`, { enabled: 0 }).then(r => r.data),
  executions: (id) => api.get(`/api/workflow/${id}/executions`).then(r => r.data.data)
}