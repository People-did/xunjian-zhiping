import request from '../utils/request'

export function getEvaluationList(params) {
  return request.get('/evaluation/list', { params })
}

export function getEvaluation(id) {
  return request.get(`/evaluation/${id}`)
}

export function getEvaluationByReport(reportId) {
  return request.get(`/evaluation/report/${reportId}`)
}

export function aiEvaluate(reportId) {
  return request.post(`/evaluation/ai/${reportId}`)
}

/**
 * 【全新】全格式AI智能评价 — 支持文档+截图+代码+ZIP混合成果
 * @param {number} reportId - 报告ID
 * @param {object} params - { rubricJson: string|null }
 */
export function aiFullEvaluate(reportId, params) {
  return request.post(`/evaluation/ai/full/${reportId}`, params || {})
}

/**
 * 【全新】自动生成评分标准
 * @param {number} reportId - 报告ID
 */
export function autoGenerateRubric(reportId) {
  return request.post(`/evaluation/rubric/auto-generate/${reportId}`)
}

/**
 * 修改/手动保存评价评分
 * 升级版：无损兼容传统三大指标修改与多维度自定义动态指标修改的数据输送
 */
export function manualEvaluate(id, data) {
  return request.put(`/evaluation/${id}`, data)
}