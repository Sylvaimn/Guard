import api from '../index'

export const intelligenceApi = {
  search: (params) => api.get('/api/intelligence/search', { params }).then(r => r.data.data),
  rag: (keyword) => api.get('/api/intelligence/rag', { params: { keyword } }).then(r => r.data.data)
}
