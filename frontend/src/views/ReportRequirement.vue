<template>
  <div class="page-container">
    <h2 class="page-title">报告要求管理</h2>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="课程">
          <el-select v-model="searchForm.courseId" placeholder="全部课程" clearable @change="handleSearch" style="width: 280px;">
            <el-option v-for="c in teacherCourses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar">
      <el-button type="primary" @click="goToCreate">发布报告要求</el-button>
    </div>
    
    <div class="table-container">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="title" label="报告标题" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程" width="180" />
        <el-table-column prop="deadline" label="截止时间" width="180">
          <template #default="{ row }">
            {{ row.deadline || '未设置' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '进行中' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        
        <el-table-column label="操作" width="320" align="center" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; justify-content: center; gap: 6px;">
              <el-button type="primary" size="small" plain icon="View" @click="handleView(row)">查看</el-button>
              <el-button type="success" size="small" plain icon="Download" @click="handleExport(row)">导出</el-button>
              <el-button type="warning" size="small" plain icon="Edit" @click="goToEdit(row)">编辑</el-button>
              <el-button 
                :type="row.status === 1 ? 'info' : 'success'" 
                size="small" 
                plain
                :icon="row.status === 1 ? 'VideoPause' : 'VideoPlay'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" size="small" plain icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="exportDialogVisible" title="导出报告要求" width="400px">
      <div style="text-align: center;">
        <el-button type="primary" size="large" @click="handleExportWord" style="width: 200px; margin: 10px;">
          导出为 Word
        </el-button>
        <el-button type="success" size="large" @click="handleExportPdf" style="width: 200px; margin: 10px;">
          导出为 PDF
        </el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="报告要求详情" width="850px">
      <div v-if="currentRequirement">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报告标题" :span="2">{{ currentRequirement.title }}</el-descriptions-item>
          <el-descriptions-item label="课程">{{ currentRequirement.courseName }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">
            {{ currentRequirement.deadline || '未设置' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentRequirement.status === 1 ? 'success' : 'info'">
              {{ currentRequirement.status === 1 ? '进行中' : '已禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentRequirement.createTime }}</el-descriptions-item>
          <el-descriptions-item label="标准类别">
            <el-tag :type="currentRequirement.hasCustomCriterion === 1 ? 'danger' : 'info'">
              {{ currentRequirement.hasCustomCriterion === 1 ? '大模型自定义指标考核' : '系统默认三大维度考核' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        
        <div v-if="currentRequirement.hasCustomCriterion === 1 && currentRequirement.criteria && currentRequirement.criteria.length" class="content-section">
          <h4 style="color: #f56c6c;"><el-icon><Compass /></el-icon> 本次作业绑定的考核细则 (大模型精准卡点)</h4>
          <div class="criterion-timeline-box">
            <el-timeline>
              <el-timeline-item
                v-for="(c, index) in currentRequirement.criteria"
                :key="index"
                type="primary"
                size="large"
              >
                <div class="timeline-item-header">
                  <span class="criterion-name">{{ c.name }}</span>
                  <el-tag type="danger" size="small" effect="dark">满分/权重：{{ c.maxScore }}分 ({{ c.weight }}%)</el-tag>
                </div>
                <p class="criterion-desc">{{ c.description || '暂无详细描述规则。' }}</p>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>

        <div class="content-section">
          <h4>报告要求正文内容</h4>
          <div class="content-box" v-html="currentRequirement.content"></div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="handleExport(currentRequirement)">导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyCourses } from '../api/course'
import request from '../utils/request' // 引入用于级联查出指标明细
import { 
  getReportRequirementList, 
  deleteReportRequirement,
  updateRequirementStatus,
  exportWord,
  exportPdf
} from '../api/reportRequirement'

const router = useRouter()
const loading = ref(false)
const exportDialogVisible = ref(false)
const detailVisible = ref(false)

const teacherCourses = ref([])
const tableData = ref([])
const currentRequirement = ref(null)

const searchForm = reactive({ courseId: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const loadTeacherCourses = async () => {
  try {
    const res = await getMyCourses()
    teacherCourses.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      courseId: searchForm.courseId || undefined
    }
    const res = await getReportRequirementList(params)
    
    const records = (res.data?.records || []).map(item => {
      const course = teacherCourses.value.find(c => c.id === item.courseId)
      return { ...item, courseName: course?.courseName || '' }
    })
    tableData.value = records
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
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

const goToCreate = () => {
  router.push({ path: '/report-requirement/edit' })
}

const goToEdit = (row) => {
  router.push({ path: '/report-requirement/edit', query: { id: row.id } })
}

// 查看详情：级联查出绑定的自定义标准条目
const handleView = async (row) => {
  currentRequirement.value = { ...row, criteria: [] }
  if (row.hasCustomCriterion === 1) {
    try {
      // 通过之前在物理表设计好的索引和关联机制，轻量化获取其标准集合
      const res = await request.get(`/report-requirement/${row.id}`)
      // 后端如果直接在 get 路由级联或者我们直接调查询接口：
      const criterionRes = await request.get(`/report-requirement/course/${row.courseId}`) 
      // 更加稳妥直接的做法：利用 MyBatis-Plus 自动查询或者发单独路由，这里假设后端 get 路由返回或者有独立映射
      // 为了 100% 稳妥，我们直接向后端刚在 Service 加好的 getById 或对应机制要明细
      const details = await request.get(`/report-requirement/${row.id}`)
      // 我们直接发起关联查询
      const resCriteria = await request.get(`/report-requirement/course/${row.courseId}`)
      // 稳妥拿到明细
      currentRequirement.value.criteria = details.data?.criteria || []
    } catch (e) {
      console.error(e)
    }
  }
  detailVisible.value = true
}

const handleExport = (row) => {
  currentRequirement.value = row
  exportDialogVisible.value = true
}

const handleExportWord = () => {
  if (currentRequirement.value) {
    exportWord(currentRequirement.value.id)
  }
  exportDialogVisible.value = false
}

const handleExportPdf = () => {
  if (currentRequirement.value) {
    exportPdf(currentRequirement.value.id)
  }
  exportDialogVisible.value = false
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定${action}该报告要求？`, '提示', { type: 'warning' })
  try {
    await updateRequirementStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该报告要求？删除后学生将无法查看。', '提示', { type: 'warning' })
  try {
    await deleteReportRequirement(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  loadTeacherCourses().then(() => loadData())
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
.content-section {
  margin-top: 20px;
  h4 { margin-bottom: 12px; color: #303133; }
}
.criterion-timeline-box {
  padding: 20px 16px 4px;
  background: #fff5f5;
  border: 1px solid #ffdbdb;
  border-radius: 6px;
  margin-bottom: 16px;
}
.timeline-item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}
.criterion-name {
  font-weight: bold;
  color: #303133;
  font-size: 14px;
}
.criterion-desc {
  font-size: 13px;
  color: #606266;
  margin: 4px 0 0;
  line-height: 1.6;
}
.content-box {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>