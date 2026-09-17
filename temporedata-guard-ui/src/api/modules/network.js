import api from '../index'

// 关系网络 / 资金流 / 通联 / 串并案 / 风险
export const networkApi = {
  similarCases: (keyword, persistEdge) => api.get('/api/network/similar-cases', { params: { keyword, persistEdge } }).then(r => r.data.data)
}
