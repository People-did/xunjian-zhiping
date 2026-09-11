<template>
  <div class="page-container">
    <h2 class="page-title">我的成绩</h2>
    
    <!-- 筛选 -->
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="课程">
          <el-select v-model="searchForm.courseId" placeholder="全部课程" clearable>
            <el-option v-for="c in myCourses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 成绩统计卡片 -->
    <div class="stats-container">
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">提交报告数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-value">{{ stats.evaluated }}</div>
            <div class="stat-label">已评价数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-value">{{ stats.pending }}</div>
            <div class="stat-label">待评价数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-value stat-avg">{{ stats.avgScore || '-' }}</div>
            <div class="stat-label">平均成绩</div>
          </div>
        </el-col>
      </el-row>
    </div>
    
    <!-- 表格 -->
    <div class="table-container">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="courseName" label="课程" min-width="120" />
        <el-table-column prop="title" label="报告标题" min-width="150" />
        <el-table-column prop="createTime" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
              {{ row.status === 1 ? '已评价' : '待评价' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成绩" width="300">
          <template #default="{ row }">
            <div v-if="row.status === 1" class="score-container">
              <div class="score-item">
                <span class="score-label">完整性</span>
                <el-tag type="info" size="small">{{ row.completenessScore ?? '-' }}</el-tag>
              </div>
              <div class="score-item">
                <span class="score-label">规范性</span>
                <el-tag type="info" size="small">{{ row.specificationScore ?? '-' }}</el-tag>
              </div>
              <div class="score-item">
                <span class="score-label">专业性</span>
                <el-tag type="info" size="small">{{ row.knowledgeScore ?? '-' }}</el-tag>
              </div>
              <div class="score-item">
                <span class="score-label">总分</span>
                <el-tag :type="getScoreType(row.totalScore)" size="small">{{ row.totalScore ?? '-' }}</el-tag>
              </div>
            </div>
            <span v-else class="pending-text">待教师评价</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">查看详情</el-button>
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

    <!-- 成绩详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="报告详情" width="700px">
      <div v-if="currentReport">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="课程名称">{{ currentReport.courseName }}</el-descriptions-item>
          <el-descriptions-item label="报告标题">{{ currentReport.title }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ currentReport.createTime }}</el-descriptions-item>
          <el-descriptions-item label="评价状态">
            <el-tag :type="currentReport.status === 1 ? 'success' : 'warning'" size="small">
              {{ currentReport.status === 1 ? '已评价' : '待评价' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        
        <div v-if="currentReport.status === 1" class="evaluation-section">
          <h4>成绩详情</h4>
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="score-detail-card">
                <div class="score-detail-label">完整性</div>
                <div class="score-detail-value">{{ currentReport.completenessScore ?? '-' }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="score-detail-card">
                <div class="score-detail-label">规范性</div>
                <div class="score-detail-value">{{ currentReport.specificationScore ?? '-' }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="score-detail-card">
                <div class="score-detail-label">专业性</div>
                <div class="score-detail-value">{{ currentReport.knowledgeScore ?? '-' }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="score-detail-card highlight">
                <div class="score-detail-label">总分</div>
                <div class="score-detail-value">{{ currentReport.totalScore ?? '-' }}</div>
              </div>
            </el-col>
          </el-row>
          
          <div v-if="currentReport.aiEvaluation" class="evaluation-content">
            <h4>AI评价</h4>
            <div class="content-box">{{ currentReport.aiEvaluation }}</div>
          </div>
          
          <div v-if="currentReport.manualEvaluation" class="evaluation-content">
            <h4>教师评语</h4>
            <div class="content-box">{{ currentReport.manualEvaluation }}</div>
          </div>
          
          <div class="evaluation-time">
            评价时间：{{ currentReport.evaluateTime || '-' }}
          </div>
        </div>
        
        <el-empty v-else description="该报告尚未被评价，请耐心等待" />
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getMyScores } from '../api/report'
import { getMyCourses } from '../api/course'

const loading = ref(false)
const tableData = ref([])
const myCourses = ref([])
const detailDialogVisible = ref(false)
const currentReport = ref(null)

const searchForm = reactive({
  courseId: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const stats = computed(() => {
  const evaluated = tableData.value.filter(item => item.status === 1).length
  const total = tableData.value.length
  const pending = total - evaluated
  const evaluatedItems = tableData.value.filter(item => item.status === 1 && item.totalScore != null)
  const avgScore = evaluatedItems.length > 0 
    ? (evaluatedItems.reduce((sum, item) => sum + (item.totalScore || 0), 0) / evaluatedItems.length).toFixed(1)
    : null
  
  return {
    total: pagination.total,
    evaluated,
    pending,
    avgScore
  }
})

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}:${seconds}`
}

const getScoreType = (score) => {
  if (score == null) return 'info'
  if (score >= 90) return 'success'
  if (score >= 80) return ''
  if (score >= 70) return 'warning'
  return 'danger'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyScores({ ...pagination, courseId: searchForm.courseId })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadMyCourses = async () => {
  try {
    const res = await getMyCourses()
    myCourses.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.courseId = null
  handleSearch()
}

const handleViewDetail = (row) => {
  currentReport.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  loadData()
  loadMyCourses()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-title {
  margin-bottom: 20px;
  font-size: 20px;
  color: #303133;
}

.search-form {
  margin-bottom: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
}

.stats-container {
  margin-bottom: 20px;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  color: #fff;
}

.stat-card:nth-child(2) {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-card:nth-child(3) {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card:nth-child(4) {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
}

.stat-avg {
  color: #fff;
}

.stat-label {
  font-size: 14px;
  margin-top: 8px;
  opacity: 0.9;
}

.table-container {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
}

.score-container {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.score-label {
  font-size: 12px;
  color: #909399;
}

.pending-text {
  color: #909399;
  font-size: 14px;
}

.evaluation-section {
  margin-top: 20px;
}

.evaluation-section h4 {
  margin: 16px 0 12px;
  color: #303133;
  font-size: 16px;
}

.score-detail-card {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  text-align: center;
}

.score-detail-card.highlight {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.score-detail-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.score-detail-card.highlight .score-detail-label {
  color: rgba(255, 255, 255, 0.8);
}

.score-detail-value {
  font-size: 24px;
  font-weight: bold;
}

.evaluation-content {
  margin-top: 16px;
}

.evaluation-content h4 {
  margin-bottom: 8px;
}

.content-box {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.evaluation-time {
  margin-top: 16px;
  text-align: right;
  color: #909399;
  font-size: 12px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
