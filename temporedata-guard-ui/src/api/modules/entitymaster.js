import api from '../index'

export const entityMasterApi = {
  list: (params = {}) => api.get('/api/entitymaster', { params }).then(r => r.data.data),
  detail: (id) => api.get(`/api/entitymaster/${id}`).then(r => r.data.data),
  register: (data) => api.post('/api/entitymaster/register', data).then(r => r.data.data),
  confirm: (id) => api.post(`/api/entitymaster/${id}/confirm`).then(r => r.data.data),
  merge: (masterId, candidateId) => api.post('/api/entitymaster/merge', null, { params: { masterId, candidateId } }).then(r => r.data.data),
  addLabel: (id, data) => api.post(`/api/entitymaster/${id}/labels`, data).then(r => r.data.data),
  remove: (id) => api.delete(`/api/entitymaster/${id}`).then(r => r.data.data)
}
