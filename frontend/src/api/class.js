import request from '../utils/request'

export function getClassList(params) {
  return request.get('/class/list', { params })
}

export function getAllClasses() {
  return request.get('/class/all')
}

export function getTeacherClasses() {
  return request.get('/class/teacher')
}

export function getClassDetail(id) {
  return request.get(`/class/${id}/detail`)
}

export function getClassReports(id, params) {
  return request.get(`/class/${id}/reports`, { params })
}

export function getClassStatistics(id) {
  return request.get(`/class/${id}/statistics`)
}

export function getClassStudents(id) {
  return request.get(`/class/${id}/students`)
}

export function addClass(data) {
  return request.post('/class', data)
}

export function updateClass(data) {
  return request.put(`/class/${data.id}`, data)
}

export function deleteClass(id) {
  return request.delete(`/class/${id}`)
}
