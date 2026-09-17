import api from '../index'

export const driverApi = {
  list: (tenantId = 'DEFAULT') => api.get('/api/driver', { params: { tenantId } }).then(r => r.data),
  create: (data) => api.post('/api/driver', data).then(r => r.data),
  delete: (id) => api.delete(`/api/driver/${id}`).then(r => r.data)
}