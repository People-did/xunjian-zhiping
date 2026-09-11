import request from '../utils/request'

export function getAllConfigs() {
  return request.get('/config/all')
}

export function updateConfig(key, value) {
  return request.put(`/config/${key}`, { value })
}

export function batchUpdateConfigs(data) {
  return request.put('/config/batch', data)
}
