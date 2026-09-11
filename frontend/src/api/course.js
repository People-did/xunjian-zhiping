import request from '../utils/request'

export function getCourseList(params) {
  return request.get('/course/list', { params })
}

export function getCourse(id) {
  return request.get(`/course/${id}`)
}

export function addCourse(data) {
  return request.post('/course', data)
}

export function updateCourse(data) {
  return request.put('/course', data)
}

export function deleteCourse(id) {
  return request.delete(`/course/${id}`)
}

export function getAvailableCourses() {
  return request.get('/course/available')
}

export function selectCourse(courseId) {
  return request.post(`/course/select/${courseId}`)
}

export function cancelCourse(courseId) {
  return request.delete(`/course/cancel/${courseId}`)
}

export function getMyCourses() {
  return request.get('/course/my-courses')
}

export function getCourseStudents(courseId) {
  return request.get(`/course/${courseId}/students`)
}
