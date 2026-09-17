import api from '../index'

export const changeAuditApi = {
  list: (params) => api.get('/api/changeaudit/list', { params }).then(r => r.data.data),
  byEntity: (entityType, entityId) =>
    api.get('/api/changeaudit/by-entity', { params: { entityType, entityId } }).then(r => r.data.data)
}