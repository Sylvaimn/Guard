import api from '../index'

export const watchApi = {
  lists: (params = {}) => api.get('/api/watch/lists', { params }).then(r => r.data.data),
  createList: (data) => api.post('/api/watch/lists', data).then(r => r.data.data),
  updateListStatus: (id, status) => api.put(`/api/watch/lists/${id}/status`, null, { params: { status } }).then(r => r.data.data),
  alerts: (params = {}) => api.get('/api/watch/alerts', { params }).then(r => r.data.data),
  fire: (data) => api.post('/api/watch/fire', data).then(r => r.data.data),
  assign: (id, assignee) => api.post(`/api/watch/alerts/${id}/assign`, { assignee }).then(r => r.data.data),
  process: (id, data) => api.post(`/api/watch/alerts/${id}/process`, data).then(r => r.data.data),
  stats: () => api.get('/api/watch/stats').then(r => r.data.data)
}
