import api from '../index'

// Scheduler is integrated into WorkflowController.
// Backend has no standalone /api/scheduler endpoint.
// Scheduling operations are available via /api/workflow/{id}/schedule
export const schedulerApi = {
  list: () => api.get('/api/workflow').then(r => {
    const data = r.data?.data || r.data
    // Filter workflows that have scheduling enabled
    return (Array.isArray(data) ? data : []).filter(w => w.scheduleStatus)
  }),
  // Schedule a workflow
  schedule: (workflowId, data) => api.put(`/api/workflow/${workflowId}/schedule`, data).then(r => r.data),
  // Disable schedule for a workflow
  disable: (workflowId) => api.put(`/api/workflow/${workflowId}/schedule`, { enabled: 0 }).then(r => r.data),
  // Execution history
  executions: (workflowId) => api.get(`/api/workflow/${workflowId}/executions`).then(r => r.data?.data || r.data)
}