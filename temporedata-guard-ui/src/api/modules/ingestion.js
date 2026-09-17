import api from '../index'

export const ingestionApi = {
  list: () => api.get('/api/ingestion').then(r => r.data.data),
  create: (data) => api.post('/api/ingestion', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/ingestion/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/ingestion/${id}`).then(r => r.data.data),
  execute: (id) => api.post(`/api/ingestion/${id}/execute`).then(r => r.data.data),
  cdcStatus: () => api.get('/api/ingestion/cdc/status').then(r => r.data.data),
  cdcOffsets: () => api.get('/api/ingestion/cdc/offsets').then(r => r.data.data),
  cdcDemo: () => api.post('/api/ingestion/cdc/demo').then(r => r.data.data),
  cdcReconcile: () => api.get('/api/ingestion/cdc/reconcile').then(r => r.data.data)
}