import api from '../index'

/**
 * Enterprise lineage module API (graph model: nodes/edges).
 */
export const lineageApi = {
  rebuild: () => api.post('/api/lineage/rebuild').then(r => r.data.data),
  getGraph: (params) => api.get('/api/lineage/graph', { params }).then(r => r.data.data),
  getOverview: () => api.get('/api/lineage/overview').then(r => r.data.data),
  getImpact: (params) => api.get('/api/lineage/impact', { params }).then(r => r.data.data),
  getTrace: (params) => api.get('/api/lineage/lineage', { params }).then(r => r.data.data),
  findPath: (data) => api.post('/api/lineage/path', data).then(r => r.data.data),
  findCycles: () => api.get('/api/lineage/cycles').then(r => r.data.data),
  getHeat: () => api.get('/api/lineage/heat').then(r => r.data.data),
  search: (keyword) => api.get('/api/lineage/search', { params: { keyword } }).then(r => r.data.data),
  exportJson: (params) => api.get('/api/lineage/export', { params }).then(r => r.data.data)
}