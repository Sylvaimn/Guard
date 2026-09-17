import api from '../index'

export const aiApi = {
  analyze: (data) => api.post('/api/ai/analyze', data).then(r => r.data.data),
  nl2sql: (data) => api.post('/api/ai/nl2sql', data).then(r => r.data.data),
  retrieve: (question) => api.get('/api/ai/retrieve', { params: { question } }).then(r => r.data.data),
  tools: () => api.get('/api/ai/tools').then(r => r.data.data),
  submitTask: (data) => api.post('/api/ai/tasks', data).then(r => r.data.data),
  taskDetail: (id) => api.get(`/api/ai/tasks/${id}`).then(r => r.data.data),
  clues: (status) => api.get('/api/ai/anomaly/clues', { params: { status } }).then(r => r.data.data),
  fraud: (keyword) => api.get('/api/ai/fraud', { params: { keyword } }).then(r => r.data.data),
  sentiment: (text) => api.post('/api/ai/sentiment', { text }).then(r => r.data.data),
  docTemplates: () => api.get('/api/ai/docs/templates').then(r => r.data.data),
  docGenerate: (template, variables) => api.post('/api/ai/docs/generate', { template, variables }).then(r => r.data.data),
  detectAnomaly: () => api.post('/api/ai/anomaly/detect').then(r => r.data.data),
  confirmClue: (id) => api.post(`/api/ai/anomaly/clues/${id}/confirm`).then(r => r.data.data),
  dismissClue: (id) => api.post(`/api/ai/anomaly/clues/${id}/dismiss`).then(r => r.data.data)
}
