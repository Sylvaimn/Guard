import api from '../index'
export const licenseApi = {
  list: () => api.get('/api/license').then(r => r.data.data),
  get: () => api.get('/api/license').then(r => r.data.data),
  install: (licenseKey) => api.post('/api/license/install', { licenseKey }).then(r => r.data.data),
  status: () => api.get('/api/license/status').then(r => r.data.data),
}