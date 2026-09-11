import request from '../utils/request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function getUserInfo() {
  return request.get('/auth/userinfo')
}

export function logout() {
  return request.post('/auth/logout')
}
