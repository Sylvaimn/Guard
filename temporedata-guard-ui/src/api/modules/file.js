import api from '../index'
export const fileApi = {
  list: () => api.get('/api/file').then(r => r.data.data),
  upload: (formData) => api.post('/api/file/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => r.data.data),
  delete: (id) => api.delete(`/api/file/${id}`).then(r => r.data.data),
  getDownloadUrl: (id) => `/api/file/${id}/download`,
  preview: (id) => api.get(`/api/file/${id}/preview`).then(r => r.data.data),
  downloadBlob: (id) => api.get(`/api/file/${id}/download`, { responseType: 'blob' }).then(r => r.data)
}