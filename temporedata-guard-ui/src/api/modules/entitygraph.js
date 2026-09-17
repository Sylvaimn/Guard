import api from '../index'

export const entityGraphApi = {
  graph: (entityId, params = {}) => api.get(`/api/entitygraph/${entityId}`, { params }).then(r => r.data.data)
}
