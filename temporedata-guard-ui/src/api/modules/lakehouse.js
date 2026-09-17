import api from '../index'

export const lakehouseApi = {
  init: () => api.get('/api/lakehouse/init').then(r => r.data.data),
  append: (rows) => api.post('/api/lakehouse/append', rows).then(r => r.data.data),
  read: () => api.get('/api/lakehouse/read').then(r => r.data.data),
  readSnapshot: (snapshotId) => api.get('/api/lakehouse/read', { params: { snapshotId } }).then(r => r.data.data),
  snapshots: () => api.get('/api/lakehouse/snapshots').then(r => r.data.data)
}
