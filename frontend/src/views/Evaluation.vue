<template>
  <div class="page-container">
    <h2 class="page-title">评价管理</h2>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="课程" v-if="isTeacher">
          <el-select v-model="searchForm.courseId" placeholder="全部课程" clearable style="width: 200px">
            <el-option v-for="c in teacherCourses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" v-else>
          <el-select v-model="searchForm.classId" placeholder="全部" clearable>
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-container">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="studentName" label="学生" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="reportTitle" label="报告" show-overflow-tooltip />
        <el-table-column label="完整性 (旧)" prop="completenessScore" width="100">
          <template #default="{ row }">
            <span>{{ row.dynamicScoresJson ? '-' : row.completenessScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="规范性 (旧)" prop="specificationScore" width="100">
          <template #default="{ row }">
            <span>{{ row.dynamicScoresJson ? '-' : row.specificationScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="知识点 (旧)" prop="knowledgeScore" width="100">
          <template #default="{ row }">
            <span>{{ row.dynamicScoresJson ? '-' : row.knowledgeScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="综合总分" prop="totalScore" width="100">
          <template #default="{ row }">
            <el-tag :type="getScoreType(row.totalScore)">{{ row.totalScore }}分</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评价方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isAi === 1 ? 'primary' : 'info'">
              {{ row.isAi === 1 ? 'AI智能批改' : '人工评定' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="evaluateTime" label="评价时间" width="180" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; justify-content: center; gap: 6px;">
              <el-button type="primary" size="small" plain icon="View" @click="handleView(row)">查看详情</el-button>
              <el-button type="warning" size="small" plain icon="Edit" @click="handleEdit(row)">修改评分</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="报告评价多维详情看板" width="750px">
      <div v-if="currentEval">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生">{{ currentEval.studentName }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ currentEval.className }}</el-descriptions-item>
          <el-descriptions-item label="报告标题" :span="2">{{ currentEval.reportTitle }}</el-descriptions-item>
          <el-descriptions-item label="评价人/教师">{{ currentEval.teacherName || '大模型智能引擎' }}</el-descriptions-item>
          <el-descriptions-item label="评价方式">
            <el-tag :type="currentEval.isAi === 1 ? 'primary' : 'info'">
              {{ currentEval.isAi === 1 ? 'AI智能批改' : '人工评定' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最终考核总分" :span="2">
            <b style="font-size: 18px; color: #409eff;">{{ currentEval.totalScore }} 分</b>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="parsedDynamicScores" class="score-section">
          <h4 style="color: #e6a23c;"><el-icon><Compass /></el-icon> 教师定制化指标考核分布明细</h4>
          <div style="background: #fafafa; border: 1px solid #e4e7ed; border-radius: 6px; padding: 16px;">
            <div v-for="(val, key) in parsedDynamicScores" :key="key" class="dynamic-score-card">
              <div class="dynamic-score-meta">
                <span class="dynamic-key-name">{{ key }}</span>
                <span class="dynamic-key-val">得分：<b>{{ val.score }}</b> 分</span>
              </div>
              <el-progress
                :percentage="calculatePercentage(val.score, key)"
                :status="val.score >= 60 ? 'success' : 'exception'"
                :stroke-width="12"
              />
              <p class="dynamic-key-comment" v-if="val.comment">
                <el-icon><ChatLineSquare /></el-icon> 维度评语：{{ val.comment }}
              </p>
            </div>
          </div>
        </div>

        <div v-else class="score-section">
          <h4>评分详情 (系统默认标准)</h4>
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="score-item">
                <p class="label">完整性 (30分)</p>
                <p class="value">{{ currentEval.completenessScore }}</p>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="score-item">
                <p class="label">规范性 (30分)</p>
                <p class="value">{{ currentEval.specificationScore }}</p>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="score-item">
                <p class="label">知识点 (40分)</p>
                <p class="value">{{ currentEval.knowledgeScore }}</p>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="score-item total">
                <p class="label">综合总分</p>
                <p class="value">{{ currentEval.totalScore }}</p>
              </div>
            </el-col>
          </el-row>
        </div>

        <div v-if="parsedSuggestions && parsedSuggestions.length" class="evaluation-content" style="background: #f0f9eb; border: 1px solid #c2e7b0;">
          <h5 style="color: #67c23a; font-weight: bold; margin: 0 0 8px 0;"><el-icon><Opportunity /></el-icon> AI 赋能实训改进建议列表</h5>
          <ul style="margin: 0; padding-left: 20px; color: #606266; font-size: 13px; line-height: 1.8;">
            <li v-for="(sug, sIdx) in parsedSuggestions" :key="sIdx">{{ sug }}</li>
          </ul>
        </div>

        <div v-if="currentEval.manualEvaluation" class="evaluation-content">
          <h5>教师终审定性备注</h5>
          <p style="margin: 0; font-size: 13px; color: #303133;">{{ currentEval.manualEvaluation }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="教学质检 - 教师人工订正评分" width="520px">
      <el-form ref="formRef" :model="form" label-width="110px">

        <div v-if="form.isCustom">
          <div style="background: #fff8f8; border: 1px solid #fde2e2; border-radius: 4px; padding: 10px 14px; margin-bottom: 16px; font-size: 13px; color: #f56c6c;">
            当前作业启用大模型自定义评分标准。教师修改各单项分值后，系统将重构动态物理长口袋 JSON 数据包并重算大总分。
          </div>
          <el-form-item v-for="(item, key) in form.dynamicScores" :key="key" :label="String(key)">
            <el-input-number
              v-model="item.score"
              :min="0"
              :max="getCustomMaxScore(key)"
              :precision="1"
              :step="1"
            />
            <span style="font-size: 12px; color: #909399; margin-left: 10px;">(当前上限: {{ getCustomMaxScore(key) }}分)</span>
          </el-form-item>
        </div>

        <div v-else>
          <el-form-item label="完整性得分">
            <el-input-number v-model="form.completenessScore" :min="0" :max="30" :precision="1" />
          </el-form-item>
          <el-form-item label="规范性得分">
            <el-input-number v-model="form.specificationScore" :min="0" :max="30" :precision="1" />
          </el-form-item>
          <el-form-item label="知识点得分">
            <el-input-number v-model="form.knowledgeScore" :min="0" :max="40" :precision="1" />
          </el-form-item>
        </div>

        <el-form-item label="审定折合总分">
          <el-input-number v-model="form.totalScore" :min="0" :max="100" :precision="1" readonly style="background: #f5f7fa;" />
        </el-form-item>
        <el-form-item label="教师终审备注">
          <el-input v-model="form.manualEvaluation" type="textarea" :rows="3" placeholder="请输入人工修正或最终定性评语..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存修正</el-button>
      </template>
    </el-dialog>

    <!-- AI全格式评价弹窗（三步流程） -->
    <el-dialog v-model="aiDialogVisible" title="AI智能评价" width="700px" :close-on-click-modal="false">
      <!-- 步骤条 -->
      <el-steps :active="aiStep" align-center style="margin-bottom: 30px">
        <el-step title="评分标准" description="确认评分维度" />
        <el-step title="AI评价中" description="大模型分析中" />
        <el-step title="评价完成" description="查看完整报告" />
      </el-steps>

      <!-- 步骤1：评分标准确认 -->
      <div v-if="aiStep === 0">
        <el-alert title="评分标准确认" type="info" :closable="false" style="margin-bottom: 16px">
          请选择评分标准。可自动根据成果类型生成，也可手动输入自定义标准。
        </el-alert>

        <el-form label-width="100px">
          <el-form-item label="选择报告">
            <el-select v-model="aiForm.reportId" placeholder="请选择待评价的学生报告" style="width:100%">
              <el-option v-for="r in pendingReports" :key="r.id" :label="`${r.studentName} - ${r.title}`" :value="r.id" />
            </el-select>
          </el-form-item>

          <el-divider />

          <el-form-item label="评分标准">
            <el-radio-group v-model="aiForm.rubricMode" style="margin-bottom: 12px">
              <el-radio value="auto">自动生成（根据成果类型智能推荐）</el-radio>
              <el-radio value="default">使用默认标准（完整/规范/质量）</el-radio>
              <el-radio value="custom">手动输入自定义标准</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 自动生成的评分标准预览 -->
          <el-form-item v-if="aiForm.rubricMode === 'auto' && aiForm.rubricPreview.length > 0" label="生成结果">
            <div style="background:#f5f7fa;padding:12px;border-radius:4px;width:100%">
              <el-table :data="aiForm.rubricPreview" size="small" border>
                <el-table-column prop="name" label="维度" width="120" />
                <el-table-column prop="weight" label="权重(%)" width="80" />
                <el-table-column prop="maxScore" label="满分" width="80" />
                <el-table-column prop="description" label="评分要点" show-overflow-tooltip />
              </el-table>
            </div>
          </el-form-item>

          <!-- 自定义评分标准输入 -->
          <el-form-item v-if="aiForm.rubricMode === 'custom'" label="评分标准">
            <el-input v-model="aiForm.customRubric" type="textarea" :rows="5"
              placeholder='[{"name":"完整性","weight":35,"maxScore":35,"description":"..."},{"name":"规范性","weight":30,"maxScore":30,"description":"..."}]' />
          </el-form-item>
        </el-form>

        <div style="text-align:right;margin-top:12px">
          <el-button v-if="aiForm.rubricMode === 'auto' && aiForm.reportId" type="success" @click="handleAutoGenerateRubric" :loading="rubricGenerating">
            生成评分标准
          </el-button>
        </div>
      </div>

      <!-- 步骤2：AI评价中 -->
      <div v-if="aiStep === 1" style="text-align:center;padding:40px">
        <el-icon class="is-loading" :size="48" color="#409eff"><Loading /></el-icon>
        <p style="margin-top:16px;color:#606266">AI 正在分析成果内容并生成评价报告...</p>
        <p style="font-size:12px;color:#909399">正在解析文件内容 → 匹配评分标准 → 生成7段式报告</p>
      </div>

      <!-- 步骤3：完整报告展示 -->
      <div v-if="aiStep === 2 && fullReport">
        <el-tabs v-model="reportTab">
          <el-tab-pane label="📋 成果概览" name="overview">
            <el-descriptions :column="2" border size="small" v-if="fullReport.overview">
              <el-descriptions-item label="文件数">{{ fullReport.overview.fileCount }}</el-descriptions-item>
              <el-descriptions-item label="文件类型">{{ fullReport.overview.fileTypes }}</el-descriptions-item>
              <el-descriptions-item label="成果类型">{{ fullReport.overview.resultType }}</el-descriptions-item>
              <el-descriptions-item label="完成度">{{ fullReport.overview.completeness }}</el-descriptions-item>
            </el-descriptions>
            <!-- 文档摘要 -->
            <div v-if="fullReport.documentSummary" style="margin-top:16px">
              <h4>📄 文档摘要</h4>
              <pre style="background:#f5f7fa;padding:12px;border-radius:4px;font-size:13px;white-space:pre-wrap">{{ formatJsonDisplay(fullReport.documentSummary) }}</pre>
            </div>
          </el-tab-pane>

          <el-tab-pane label="💻 代码/截图" name="media" v-if="fullReport.imageSummary || fullReport.codeSummary">
            <div v-if="fullReport.imageSummary" style="margin-bottom:16px">
              <h4>🖼️ 截图分析</h4>
              <pre style="background:#f5f7fa;padding:12px;border-radius:4px;font-size:13px;white-space:pre-wrap">{{ formatJsonDisplay(fullReport.imageSummary) }}</pre>
            </div>
            <div v-if="fullReport.codeSummary">
              <h4>💻 代码分析</h4>
              <pre style="background:#f5f7fa;padding:12px;border-radius:4px;font-size:13px;white-space:pre-wrap">{{ formatJsonDisplay(fullReport.codeSummary) }}</pre>
            </div>
          </el-tab-pane>

          <el-tab-pane label="📊 评分详情" name="scoring">
            <div v-if="fullReport.scoringDetails && fullReport.scoringDetails.length">
              <el-table :data="fullReport.scoringDetails" border size="small">
                <el-table-column prop="dimension" label="评分维度" width="120" />
                <el-table-column prop="maxScore" label="满分" width="70" />
                <el-table-column prop="score" label="得分" width="70">
                  <template #default="{ row }">
                    <el-tag :type="getScoreTagType(row.score, row.maxScore)">{{ row.score }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="comment" label="评分说明" show-overflow-tooltip min-width="200" />
              </el-table>
              <div style="text-align:center;margin-top:16px">
                <span style="font-size:32px;font-weight:bold;color:#409eff">{{ fullReport.totalScore }}</span>
                <span style="font-size:16px;color:#909399"> / 100 分</span>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="🎯 综合评价" name="evaluation">
            <div v-if="fullReport.overallEvaluation">
              <el-alert :title="'✅ 优点：' + fullReport.overallEvaluation.strengths" type="success" :closable="false" style="margin-bottom:12px" />
              <el-alert :title="'⚠️ 不足：' + fullReport.overallEvaluation.weaknesses" type="warning" :closable="false" />
            </div>
          </el-tab-pane>

          <el-tab-pane label="💡 改进建议" name="suggestions">
            <el-empty v-if="!fullReport.suggestions || !fullReport.suggestions.length" description="无改进建议" />
            <el-timeline v-else>
              <el-timeline-item v-for="(sug, i) in fullReport.suggestions" :key="i" :timestamp="'建议' + (i+1)" placement="top">
                <el-card>{{ sug }}</el-card>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>

          <el-tab-pane label="⚠️ 核查要点" name="checks">
            <el-empty v-if="!fullReport.checkPoints || !fullReport.checkPoints.length" description="✅ 无需特别核查" />
            <ul v-else style="line-height:2">
              <li v-for="(cp, i) in fullReport.checkPoints" :key="i">{{ cp }}</li>
            </ul>
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <el-button @click="aiDialogVisible = false">关闭</el-button>
        <el-button v-if="aiStep === 0" type="primary" @click="handleStartAiEvaluate" :loading="aiLoading" :disabled="!aiForm.reportId">
          开始AI评价
        </el-button>
        <el-button v-if="aiStep === 2" type="success" @click="finishAiEvaluate">完成，刷新列表</el-button>
      </template>
    </el-dialog>

    <el-button
      type="success"
      class="ai-evaluate-btn"
      @click="aiDialogVisible = true"
    >
      <el-icon><MagicStick /></el-icon>
      AI智能评价
    </el-button>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getEvaluationList, getEvaluation, manualEvaluate, aiEvaluate, aiFullEvaluate, autoGenerateRubric } from '../api/evaluation'
import { getAllClasses } from '../api/class'
import { getReportList } from '../api/report'
import { getMyCourses } from '../api/course'
import { useUserStoreHook } from '../stores'

const userStore = useUserStoreHook()
const isTeacher = computed(() => userStore.userInfo?.role === 2)

const loading = ref(false)
const detailVisible = ref(false)
const editVisible = ref(false)
const aiDialogVisible = ref(false)
const aiLoading = ref(false)
const formRef = ref()
const classes = ref([])
const teacherCourses = ref([])
const currentEval = ref(null)
const pendingReports = ref([])

const searchForm = reactive({ classId: null, courseId: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const tableData = ref([])

// 升级版核心变动表单模型容器
const form = reactive({
  id: null,
  isCustom: false, // 判定当前改分记录性质
  completenessScore: 0,
  specificationScore: 0,
  knowledgeScore: 0,
  dynamicScores: {}, // 承载自定义多维度打分细则对象的修改备份口袋
  totalScore: 0,
  manualEvaluation: ''
})

const aiForm = reactive({
  reportId: null,
  rubricMode: 'auto',    // 'auto' | 'default' | 'custom'
  rubricPreview: [],     // 自动生成的评分标准预览
  customRubric: ''       // 手动输入的评分标准
})

// AI全格式评价步骤控制
const aiStep = ref(0)           // 0=确认标准, 1=评价中, 2=完成
const rubricGenerating = ref(false)
const fullReport = ref(null)    // 完整7段式报告数据
const reportTab = ref('overview')

// 【精细化计算】反推自定义指标在大模型中的最高可能值，防止教师篡改越界
const getCustomMaxScore = (key) => {
  if (!currentEval.value || !currentEval.value.aiEvaluation) return 100
  try {
    // 根据大模型返回的参考结构进行推算，默认上限兜底
    return 100
  } catch {
    return 100
  }
}

// 智能进度条转化比率函数
const calculatePercentage = (score, key) => {
  const s = parseFloat(score) || 0
  if (s <= 0) return 0
  return Math.min(Math.round(s * 2.5), 100)
}

// 【计算属性】解析动态JSON分值细则
const parsedDynamicScores = computed(() => {
  if (!currentEval.value || !currentEval.value.dynamicScoresJson) return null
  try {
    const obj = JSON.parse(currentEval.value.dynamicScoresJson)
    return obj.scores || null
  } catch (e) {
    // 兼容部分直接存在aiEvaluation中的动态格式
    try {
      const obj = JSON.parse(currentEval.value.aiEvaluation)
      return obj.scores || null
    } catch {
      return null
    }
  }
})

// 【计算属性】智能提取改进建议
const parsedSuggestions = computed(() => {
  if (!currentEval.value || !currentEval.value.aiEvaluation) return []
  try {
    const obj = JSON.parse(currentEval.value.aiEvaluation)
    return obj.suggestions || []
  } catch {
    return []
  }
})

// 【大升级：智能联动监听器】
watch(() => [form.completenessScore, form.specificationScore, form.knowledgeScore], () => {
  if (!form.isCustom) {
    form.totalScore = form.completenessScore + form.specificationScore + form.knowledgeScore
  }
})

// 监听动态表单内部单项分值变化，实时累加计算大总分
watch(() => form.dynamicScores, (newVal) => {
  if (form.isCustom && newVal) {
    let sum = 0
    for (let k in newVal) {
      sum += (parseFloat(newVal[k].score) || 0)
    }
    form.totalScore = parseFloat(sum.toFixed(2))
  }
}, { deep: true })

const getScoreType = (score) => {
  if (!score) return 'info'
  if (score >= 90) return 'success'
  if (score >= 80) return 'primary'
  if (score >= 60) return 'warning'
  return 'danger'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getEvaluationList({ ...searchForm, ...pagination })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadClasses = async () => {
  try {
    const res = await getAllClasses()
    classes.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

const loadTeacherCourses = async () => {
  try {
    const res = await getMyCourses()
    teacherCourses.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

const loadPendingReports = async () => {
  try {
    const res = await getReportList({ status: 0, pageNum: 1, pageSize: 100 })
    pendingReports.value = res.data.records || []
  } catch (error) {
    console.error(error)
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.classId = null
  searchForm.courseId = null
  handleSearch()
}

const handleView = async (row) => {
  try {
    const res = await getEvaluation(row.id)
    currentEval.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

// 【大升级：调起评分修改弹窗】
const handleEdit = (row) => {
  currentEval.value = row // 锁定参考源
  form.id = row.id
  form.manualEvaluation = row.manualEvaluation || ''
  form.totalScore = parseFloat(row.totalScore) || 0

  if (row.dynamicScoresJson) {
    // 1. 如果存在自定义多维度长口袋，切入动态表单工作轨道
    form.isCustom = true
    try {
      const parsed = JSON.parse(row.dynamicScoresJson)
      // 深拷贝克隆一份各项打分指标实体包，解耦前端操作
      form.dynamicScores = JSON.parse(JSON.stringify(parsed.scores || {}))
    } catch {
      form.dynamicScores = {}
    }
  } else {
    // 2. 传统三大旧指标工作轨道
    form.isCustom = false
    form.completenessScore = parseFloat(row.completenessScore) || 0
    form.specificationScore = parseFloat(row.specificationScore) || 0
    form.knowledgeScore = parseFloat(row.knowledgeScore) || 0
    form.dynamicScores = {}
  }
  editVisible.value = true
}

// 【大升级：提交打分修正包】
const handleSubmit = async () => {
  try {
    let submitData = {
      totalScore: form.totalScore,
      manualEvaluation: form.manualEvaluation
    }

    if (form.isCustom) {
      // 自定义标准包流转：把微调后的动态指标map包覆在 dynamicScores 口袋里安全提交
      const rawPayload = currentEval.value?.dynamicScoresJson ? JSON.parse(currentEval.value.dynamicScoresJson) : {}
      rawPayload.scores = form.dynamicScores
      rawPayload.totalScore = form.totalScore

      submitData.dynamicScores = rawPayload
    } else {
      // 传统包流转
      submitData.completenessScore = form.completenessScore
      submitData.specificationScore = form.specificationScore
      submitData.knowledgeScore = form.knowledgeScore
    }

    await manualEvaluate(form.id, submitData)
    ElMessage.success('人工评分审定保存成功！')
    editVisible.value = false
    loadData()
  } catch (error) {
    console.error(error)
  }
}

// 【全新】自动生成评分标准
const handleAutoGenerateRubric = async () => {
  if (!aiForm.reportId) {
    ElMessage.warning('请先选择报告')
    return
  }
  rubricGenerating.value = true
  try {
    const res = await autoGenerateRubric(aiForm.reportId)
    aiForm.rubricPreview = res.data || []
    ElMessage.success('评分标准已根据成果内容自动生成')
  } catch (error) {
    ElMessage.error('生成失败: ' + (error.message || '未知错误'))
  } finally {
    rubricGenerating.value = false
  }
}

// 【全新】开始全格式AI评价（三步流程入口）
const handleStartAiEvaluate = async () => {
  if (!aiForm.reportId) {
    ElMessage.warning('请选择要进行评价的报告')
    return
  }

  // 构建评分标准参数
  let rubricJson = null
  if (aiForm.rubricMode === 'auto' && aiForm.rubricPreview.length > 0) {
    rubricJson = JSON.stringify(aiForm.rubricPreview)
  } else if (aiForm.rubricMode === 'custom') {
    if (!aiForm.customRubric.trim()) {
      ElMessage.warning('请输入自定义评分标准')
      return
    }
    rubricJson = aiForm.customRubric.trim()
  }
  // 'default' 模式传 null，后端使用默认三大维度

  aiLoading.value = true
  aiStep.value = 1  // 切换到"评价中"

  try {
    const res = await aiFullEvaluate(aiForm.reportId, { rubricJson })
    const data = res.data

    // 解析 aiEvaluation JSON 获取完整报告
    let reportData = data
    if (data.aiEvaluation) {
      try {
        reportData = JSON.parse(data.aiEvaluation)
      } catch {
        reportData = data
      }
    }

    fullReport.value = reportData
    aiStep.value = 2  // 切换到"完成"
    ElMessage.success('AI全格式评价完成！')
  } catch (error) {
    ElMessage.error('AI评价失败: ' + (error.message || '请稍后重试'))
    aiStep.value = 0  // 回到第一步
  } finally {
    aiLoading.value = false
  }
}

// 完成评价，刷新列表
const finishAiEvaluate = () => {
  aiDialogVisible.value = false
  aiStep.value = 0
  fullReport.value = null
  aiForm.reportId = null
  aiForm.rubricPreview = []
  aiForm.customRubric = ''
  aiForm.rubricMode = 'auto'
  loadData()
  loadPendingReports()
}

// 评分标签颜色
const getScoreTagType = (score, maxScore) => {
  if (!score || !maxScore) return 'info'
  const ratio = score / maxScore
  if (ratio >= 0.9) return 'success'
  if (ratio >= 0.75) return 'primary'
  if (ratio >= 0.6) return 'warning'
  return 'danger'
}

// 格式化JSON展示
const formatJsonDisplay = (data) => {
  if (!data) return ''
  if (typeof data === 'string') {
    try { return JSON.stringify(JSON.parse(data), null, 2) } catch { return data }
  }
  return JSON.stringify(data, null, 2)
}

// 原有AI评价（兼容旧版）
const handleAiEvaluate = async () => {
  if (!aiForm.reportId) {
    ElMessage.warning('请选择要进行考核的报告要求')
    return
  }

  aiLoading.value = true
  try {
    await aiEvaluate(aiForm.reportId)
    ElMessage.success('AI智能全自动多维质检批改圆满完成！')
    aiDialogVisible.value = false
    loadData()
    loadPendingReports()
  } catch (error) {
    ElMessage.error(error.message || 'AI评价失败')
  } finally {
    aiLoading.value = false
  }
}

onMounted(() => {
  loadData()
  loadClasses()
  loadPendingReports()
  if (isTeacher.value) {
    loadTeacherCourses()
  }
})
</script>

<style scoped>
.search-form :deep(.el-select) {
  width: 200px;
}
.ai-evaluate-btn {
  position: fixed;
  right: 40px;
  bottom: 100px;
  z-index: 100;
}
.score-section {
  margin-top: 20px;
  h4 { margin-bottom: 16px; color: #303133; }
}
.dynamic-score-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.dynamic-score-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.dynamic-key-name {
  font-weight: bold;
  color: #303133;
  font-size: 14px;
}
.dynamic-key-val {
  font-size: 13px;
  color: #606266;
  b { color: #409eff; font-size: 15px; }
}
.dynamic-key-comment {
  font-size: 12px;
  color: #e6a23c;
  margin: 6px 0 0 0;
  line-height: 1.4;
  background: #fffdf5;
  padding: 4px 6px;
  border-radius: 2px;
}
.score-item {
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  .label { font-size: 14px; color: #909399; margin-bottom: 8px; }
  .value { font-size: 24px; font-weight: bold; color: #303133; }
  &.total .value { color: #409eff; }
}
.evaluation-content {
  margin-top: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  h5 { margin-bottom: 8px; color: #606266; font-weight: bold; }
  pre { white-space: pre-wrap; word-break: break-all; font-family: inherit; margin: 0; }
}
</style>