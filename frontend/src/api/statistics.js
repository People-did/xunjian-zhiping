import request from '../utils/request'

export function getClassStatistics(classId) {
  return request.get(`/statistics/class/${classId}`)
}

export function getAllClassesStatistics() {
  return request.get('/statistics/all')
}

export function exportScores(classId) {
  return request.get(`/statistics/export/${classId}`, { responseType: 'blob' })
}
