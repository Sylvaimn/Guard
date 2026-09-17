import api from '../index'

export const metaApi = {
  sync: (datasourceId) => api.post(`/api/meta/sync/${datasourceId}`).then(r => r.data.data),
  syncAll: () => api.post('/api/meta/sync-all').then(r => r.data.data),
  list: () => api.get('/api/meta/tables').then(r => r.data.data),
  listTablesByDatasource: (datasourceId) => api.get(`/api/meta/tables/datasource/${datasourceId}`).then(r => r.data.data),
  overview: () => api.get('/api/meta/overview').then(r => r.data.data),
  getTableDetail: (tableId) => api.get(`/api/meta/tables/${tableId}`).then(r => r.data.data),
  getColumns: (tableId) => api.get(`/api/meta/tables/${tableId}/columns`).then(r => r.data.data),
  buildLineage: (datasourceId) => api.post(`/api/meta/lineage/build/${datasourceId}`).then(r => r.data.data),
  getLineage: (tableId, maxDepth) => api.get(`/api/meta/lineage/${tableId}`, { params: { maxDepth: maxDepth || 3 } }).then(r => r.data.data),

  // Enhanced lineage analysis
  getLineageGraph: (tableId, maxDepth = 4) =>
    api.get(`/api/meta/lineage/graph/${tableId}`, { params: { maxDepth } }).then(r => r.data.data),
  getImpactAnalysis: (tableId, maxDepth = 5) =>
    api.get(`/api/meta/lineage/impact/${tableId}`, { params: { maxDepth } }).then(r => r.data.data),
  analyzeSql: (sql) =>
    api.post('/api/meta/lineage/analyze-sql', { sql }).then(r => r.data.data),
  analyzeGraph: (tableId, maxDepth = 4) =>
    api.get(`/api/meta/lineage/analyze/${tableId}`, { params: { maxDepth } }).then(r => r.data.data),
  getFieldLineage: (tableId, columnName) =>
    api.get(`/api/meta/lineage/field/${tableId}/${encodeURIComponent(columnName)}`).then(r => r.data.data),
  searchLineage: (keyword) =>
    api.get('/api/meta/lineage/search', { params: { keyword } }).then(r => r.data.data),
  listAllLineage: () =>
    api.get('/api/meta/lineage/relations').then(r => r.data.data),
  addLineage: (data) =>
    api.post('/api/meta/lineage/add', data).then(r => r.data.data),
  addFieldLineage: (data) =>
    api.post('/api/meta/lineage/field/add', data).then(r => r.data.data),
  deleteLineage: (lineageId) =>
    api.delete(`/api/meta/lineage/${lineageId}`).then(r => r.data.data),
  unifiedSync: () => api.post('/api/meta/unified/sync').then(r => r.data.data),
  unifiedList: (sourceType) => api.get('/api/meta/unified/list', { params: { sourceType } }).then(r => r.data.data)
}
