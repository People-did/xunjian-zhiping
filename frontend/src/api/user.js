import request from '../utils/request'

export function getUserList(params) {
  return request.get('/user/list', { params })
}

export function getStudents(params) {
  return request.get('/user/students', { params })
}

export function getTeachers() {
  return request.get('/user/teachers')
}

export function addUser(data) {
  return request.post('/user', data)
}

export function updateUser(data) {
  return request.put(`/user/${data.id}`, data)
}

export function deleteUser(id) {
  return request.delete(`/user/${id}`)
}

export function resetPassword(id) {
  return request.post(`/user/reset-pwd/${id}`)
}

// 批量导入用户
export function importUsers(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 下载导入模板
export function downloadTemplate() {
  return request.get('/user/template', { responseType: 'blob' })
}
