import api from '../index'

// 以图找人(照片比对闭环)
export const photoApi = {
  addPhoto: (entityId, data) => api.post(`/api/photo/persons/${entityId}/photos`, data).then(r => r.data.data),
  listPhotos: (entityId) => api.get(`/api/photo/persons/${entityId}/photos`).then(r => r.data.data),
  compare: (data) => api.post('/api/photo/compare', data).then(r => r.data.data),
  tasks: (status) => api.get('/api/photo/tasks', { params: { status } }).then(r => r.data.data),
  taskDetail: (id) => api.get(`/api/photo/tasks/${id}`).then(r => r.data.data),
  confirm: (id, cid) => api.post(`/api/photo/tasks/${id}/candidates/${cid}/confirm`).then(r => r.data.data),
  exclude: (id, cid, reason) => api.post(`/api/photo/tasks/${id}/candidates/${cid}/exclude`, { reason }).then(r => r.data.data),
  report: (id) => api.post(`/api/photo/tasks/${id}/report`).then(r => r.data.data),
  reports: (id) => api.get(`/api/photo/tasks/${id}/reports`).then(r => r.data.data)
}
