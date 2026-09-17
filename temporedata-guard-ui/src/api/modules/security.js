import api from '../index'

export const securityApi = {
  // Audit Log
  queryAuditLog: (page = 0, size = 20, params = {}) =>
    api.post('/api/security/audit/query', params, { params: { page, size } }).then(r => r.data.data),
  recordAuditLog: (params) => api.post('/api/security/audit/record', params).then(r => r.data.data),
  clearAuditLog: () => api.delete('/api/security/audit/clear').then(r => r.data.data),
  loginLogs: (page = 0, size = 20) =>
    api.get('/api/security/audit/login-logs', { params: { page, size } }).then(r => r.data.data),
  behaviorLogs: (page = 0, size = 20) =>
    api.get('/api/security/audit/behavior-logs', { params: { page, size } }).then(r => r.data.data),
  // Data Category
  categoryTree: () => api.get('/api/security/category/tree').then(r => r.data.data),
  createCategory: (data) => api.post('/api/security/category', data).then(r => r.data.data),
  updateCategory: (id, data) => api.put(`/api/security/category/${id}`, data).then(r => r.data.data),
  deleteCategory: (id) => api.delete(`/api/security/category/${id}`).then(r => r.data.data),
  // Data Level
  listLevels: () => api.get('/api/security/level').then(r => r.data.data),
  createLevel: (data) => api.post('/api/security/level', data).then(r => r.data.data),
  updateLevel: (id, data) => api.put(`/api/security/level/${id}`, data).then(r => r.data.data),
  deleteLevel: (id) => api.delete(`/api/security/level/${id}`).then(r => r.data.data),
  // Data Mask
  listMasks: () => api.get('/api/security/mask').then(r => r.data.data),
  createMask: (data) => api.post('/api/security/mask', data).then(r => r.data.data),
  updateMask: (id, data) => api.put(`/api/security/mask/${id}`, data).then(r => r.data.data),
  deleteMask: (id) => api.delete(`/api/security/mask/${id}`).then(r => r.data.data),
  // Data Standard
  listStandards: () => api.get('/api/security/standard').then(r => r.data.data),
  createStandard: (data) => api.post('/api/security/standard', data).then(r => r.data.data),
  updateStandard: (id, data) => api.put(`/api/security/standard/${id}`, data).then(r => r.data.data),
  deleteStandard: (id) => api.delete(`/api/security/standard/${id}`).then(r => r.data.data)
}