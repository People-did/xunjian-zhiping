import request from '../utils/request'

export function getReportList(params) {
  return request.get('/report/list', { params })
}

export function getReport(id) {
  return request.get(`/report/${id}`)
}

export function uploadReport(data) {
  return request.post('/report/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 【全新】多文件批量上传 — 支持文档+截图+代码+ZIP混合上传
 */
export function uploadMultipleReports(data) {
  return request.post('/report/upload/multi', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteReport(id) {
  return request.delete(`/report/${id}`)
}

export function downloadReport(id) {
  return request.get(`/report/download/${id}`, { responseType: 'blob' })
}

// 获取学生自己的成绩列表
export function getMyScores(params) {
  return request.get('/report/my-scores', { params })
}
