<template>
  <div class="page-container">
    <h2 class="page-title">
      {{ getPageTitle() }}
    </h2>
    
    <!-- 搜索/筛选 -->
    <div class="search-form" v-if="!isStudent">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="课程">
          <el-select v-model="searchForm.courseId" placeholder="全部课程" clearable>
            <el-option v-for="c in allCourses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="searchForm.classId" placeholder="全部班级" clearable>
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="待评价" :value="0" />
            <el-option label="已评价" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 表格 -->
    <div class="table-container">
      <div class="toolbar" v-if="isStudent">
        <el-button type="primary" @click="dialogVisible = true">上传报告</el-button>
      </div>
      
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="studentName" label="学生" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="courseName" label="课程" width="180" />
        <el-table-column prop="title" label="报告标题" show-overflow-tooltip />
        <el-table-column prop="fileName" label="文件名" show-overflow-tooltip />
        <el-table-column prop="statusName" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="100">
          <template #default="{ row }">
            <span v-if="row.totalScore" class="score">{{ row.totalScore }}</span>
            <span v-else class="no-score">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleView(row)">查看详情</el-button>
              <el-button type="success" size="small" @click="handleDownload(row)">下载</el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
    
    <!-- 上传对话框 -->
    <el-dialog v-model="dialogVisible" title="上传实训成果" width="600px">
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="100px">
        <el-form-item label="选择课程" prop="courseId">
          <el-select v-model="uploadForm.courseId" placeholder="请选择课程" style="width: 100%">
            <el-option v-for="c in studentCourses" :key="c.id" :label="c.courseName" :value="c.id">
              <span>{{ c.courseName }}</span>
              <span class="course-teacher" v-if="c.teacherName"> - {{ c.teacherName }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="成果标题" prop="title">
          <el-input v-model="uploadForm.title" placeholder="请输入实训成果标题" />
        </el-form-item>
        <el-form-item label="成果文件" prop="files">
          <el-upload
            ref="uploadRef"
            :file-list="fileList"
            :auto-upload="false"
            multiple
            :limit="20"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".doc,.docx,.pdf,.txt,.md,.py,.java,.js,.ts,.html,.css,.sql,.c,.cpp,.go,.rs,.php,.json,.xml,.yaml,.yml,.zip,.png,.jpg,.jpeg,.gif,.bmp,.webp"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div>将文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">
                支持文档(.doc/.docx/.pdf/.txt/.md)、代码(.py/.java/.js等)、截图(.png/.jpg)、压缩包(.zip)
                <br/>可同时上传多个文件，总大小不超过50MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <!-- 已选文件类型预览 -->
        <el-form-item v-if="fileCategories.length > 0" label="文件类型">
          <el-tag v-for="cat in fileCategories" :key="cat.type" :type="cat.color" style="margin-right:8px">
            {{ cat.icon }} {{ cat.type }} × {{ cat.count }}
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading" :disabled="fileList.length === 0">
          {{ fileList.length > 1 ? `上传 ${fileList.length} 个文件` : '上传' }}
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="报告详情" width="700px">
      <div v-if="currentReport">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报告标题">{{ currentReport.title }}</el-descriptions-item>
          <el-descriptions-item label="学生">{{ currentReport.studentName }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ currentReport.className }}</el-descriptions-item>
          <el-descriptions-item label="课程">{{ currentReport.courseName }}</el-descriptions-item>
          <el-descriptions-item label="文件名">{{ currentReport.fileName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentReport.status === 1 ? 'success' : 'warning'">
              {{ currentReport.statusName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ currentReport.createTime }}</el-descriptions-item>
        </el-descriptions>
        
        <div v-if="currentReport.totalScore" class="score-section">
          <h4>评价结果</h4>
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="score-item">
                <p class="label">完整性</p>
                <p class="value">{{ currentReport.completenessScore }}</p>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="score-item">
                <p class="label">规范性</p>
                <p class="value">{{ currentReport.specificationScore }}</p>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="score-item">
                <p class="label">知识点</p>
                <p class="value">{{ currentReport.knowledgeScore }}</p>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="score-item total">
                <p class="label">总分</p>
                <p class="value">{{ currentReport.totalScore }}</p>
              </div>
            </el-col>
          </el-row>
          
          <div v-if="currentReport.aiEvaluation" class="ai-evaluation">
            <h5>AI评价详情</h5>
            <pre>{{ formatEvaluation(currentReport.aiEvaluation) }}</pre>
          </div>
          
          <div v-if="currentReport.manualEvaluation" class="manual-evaluation">
            <h5>教师评价</h5>
            <p>{{ currentReport.manualEvaluation }}</p>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReportList, uploadReport, uploadMultipleReports, deleteReport, getReport } from '../api/report'
import { getAllClasses } from '../api/class'
import { getCourseList, getMyCourses } from '../api/course'
import { useUserStoreHook } from '../stores'

const route = useRoute()
const userStore = useUserStoreHook()
const isStudent = computed(() => userStore.userInfo?.role === 3)
const currentCourseId = computed(() => route.query.courseId ? Number(route.query.courseId) : null)
const currentClassId = computed(() => route.query.classId ? Number(route.query.classId) : null)
const requirementId = computed(() => route.query.requirementId ? Number(route.query.requirementId) : null)
const requirementTitle = computed(() => route.query.requirementTitle || '')

const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const uploading = ref(false)
const uploadRef = ref()
const uploadFormRef = ref()
const classes = ref([])
const allCourses = ref([])
const studentCourses = ref([])
const fileList = ref([])
const currentReport = ref(null)

const searchForm = reactive({ courseId: null, classId: null, status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const tableData = ref([])

const uploadForm = reactive({ courseId: null, title: '', files: [] })
const uploadRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  title: [{ required: true, message: '请输入成果标题', trigger: 'blur' }]
}

const fileCategories = computed(() => {
  const cats = {}
  fileList.value.forEach(f => {
    const ext = (f.name || '').split('.').pop()?.toLowerCase() || 'unknown'
    const info = getFileTypeInfo(ext)
    if (!cats[info.type]) cats[info.type] = { ...info, count: 0 }
    cats[info.type].count++
  })
  return Object.values(cats)
})

const getFileTypeInfo = (ext) => {
  const map = {
    doc: { type: '文档', icon: '📄', color: 'primary' },
    docx: { type: '文档', icon: '📄', color: 'primary' },
    pdf: { type: '文档', icon: '📄', color: 'primary' },
    txt: { type: '文档', icon: '📄', color: 'primary' },
    md: { type: '文档', icon: '📄', color: 'primary' },
    py: { type: '代码', icon: '💻', color: 'success' },
    java: { type: '代码', icon: '💻', color: 'success' },
    js: { type: '代码', icon: '💻', color: 'success' },
    ts: { type: '代码', icon: '💻', color: 'success' },
    html: { type: '代码', icon: '💻', color: 'success' },
    css: { type: '代码', icon: '💻', color: 'success' },
    sql: { type: '代码', icon: '💻', color: 'success' },
    png: { type: '截图', icon: '🖼️', color: 'warning' },
    jpg: { type: '截图', icon: '🖼️', color: 'warning' },
    jpeg: { type: '截图', icon: '🖼️', color: 'warning' },
    gif: { type: '截图', icon: '🖼️', color: 'warning' },
    zip: { type: '压缩包', icon: '📦', color: 'info' }
  }
  return map[ext] || { type: '其他', icon: '📁', color: '' }
}

const courseName = computed(() => {
  let course = studentCourses.value.find(c => c.id === currentCourseId.value)
  if (course) return course.courseName
  course = allCourses.value.find(c => c.id === currentCourseId.value)
  return course?.courseName || ''
})

const className = computed(() => {
  const cls = classes.value.find(c => c.id === currentClassId.value)
  return cls?.className || ''
})

const getPageTitle = () => {
  if (currentCourseId.value || currentClassId.value) {
    const parts = []
    if (courseName.value) parts.push(courseName.value)
    if (className.value) parts.push(className.value)
    if (parts.length > 0) {
      return parts.join(' - ') + ' - 报告列表'
    }
    return '课程 - 报告列表'
  }
  return isStudent.value ? '我的实训报告' : '报告管理'
}

const loadData = async () => {
  loading.value = true
  try {
    let params = { pageNum: pagination.pageNum, pageSize: pagination.pageSize }
    if (isStudent.value) {
      params.courseId = currentCourseId.value || undefined
    } else {
      if (currentCourseId.value) params.courseId = currentCourseId.value
      if (currentClassId.value) params.classId = currentClassId.value
      if (!currentCourseId.value && !currentClassId.value) {
        params = { ...searchForm, ...pagination }
      }
    }
    const res = await getReportList(params)
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

const loadAllCourses = async () => {
  try {
    const res = await getCourseList({ pageNum: 1, pageSize: 100 })
    allCourses.value = res.data.records || []
  } catch (error) {
    console.error(error)
  }
}

const loadStudentCourses = async () => {
  try {
    const res = await getMyCourses()
    studentCourses.value = res.data || []
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
  searchForm.classId = null
  searchForm.status = null
  handleSearch()
}

const handleFileChange = (file) => {
  // 收集所有已选文件的 raw 对象，确保新增的 file.raw 也被捕获
  uploadForm.files = fileList.value.map(f => f.raw).filter(Boolean)
  if (file.raw) {
    uploadForm.files = [...uploadForm.files, file.raw]
  }
}

const handleFileRemove = () => {
  uploadForm.files = fileList.value.map(f => f.raw).filter(Boolean)
}

const handleUpload = async () => {
  const valid = await uploadFormRef.value.validate().catch(() => false)
  if (!valid) return

  if (fileList.value.length === 0) {
    ElMessage.warning('请选择至少一个文件')
    return
  }

  uploading.value = true
  try {
    // 🛠️ 【核心修复机制】精准纠偏现存会话中的学生主键ID：
    // 1. 将原先残缺的 userId 全盘转换为标准持久层对应的 id
    // 2. 如果因为缓存错位依然未拿到，则传入 0（后端控制器会通过 Security 自动覆盖补齐真正合法的 studentId）
    const finalStudentId = userStore.userInfo?.id || userStore.userInfo?.userId || 0

    const formData = new FormData()
    formData.append('studentId', finalStudentId)
    formData.append('courseId', uploadForm.courseId)
    formData.append('title', uploadForm.title)

    const files = fileList.value.map(f => f.raw).filter(Boolean)

    if (files.length > 1) {
      files.forEach(file => {
        formData.append('files', file)
      })
      await uploadMultipleReports(formData)
    } else {
      formData.append('file', files[0])
      await uploadReport(formData)
    }

    ElMessage.success(`成功上传 ${files.length} 个文件`)
    dialogVisible.value = false
    uploadForm.courseId = null
    uploadForm.title = ''
    uploadForm.files = []
    fileList.value = []
    loadData()
  } catch (error) {
    console.error(error)
  } finally {
    uploading.value = false
  }
}

const handleView = async (row) => {
  try {
    const res = await getReport(row.id)
    currentReport.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

const handleDownload = (row) => {
  window.open(`/api/report/download/${row.id}`, '_blank')
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该报告？', '提示', { type: 'warning' })
  try {
    await deleteReport(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    console.error(error)
  }
}

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

const formatEvaluation = (jsonStr) => {
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch {
    return jsonStr
  }
}

onMounted(() => {
  loadData()
  loadClasses()
  loadAllCourses()
  if (isStudent.value) {
    loadStudentCourses().then(() => {
      if (currentCourseId.value && requirementTitle.value) {
        uploadForm.courseId = currentCourseId.value
        uploadForm.title = requirementTitle.value
        dialogVisible.value = true
      }
    })
  }
})
</script>

<style scoped>
.page-container { overflow: hidden; }
.toolbar { margin-bottom: 16px; }
:deep(.el-table) { overflow: hidden; }
:deep(.el-table__body-wrapper) { overflow-x: hidden !important; }
.action-buttons { display: flex; flex-wrap: nowrap; gap: 4px; }
.score { font-weight: bold; color: #409eff; }
.no-score { color: #999; }
.score-section {
  margin-top: 20px;
  h4 { margin-bottom: 16px; color: #303133; }
}
.score-item {
  text-align: center; padding: 16px; background: #f5f7fa; border-radius: 4px;
  .label { font-size: 14px; color: #909399; margin-bottom: 8px; }
  .value { font-size: 24px; font-weight: bold; color: #303133; }
  &.total .value { color: #409eff; }
}
.ai-evaluation, .manual-evaluation {
  margin-top: 16px; padding: 16px; background: #f5f7fa; border-radius: 4px;
  h5 { margin-bottom: 8px; color: #606266; }
  pre { white-space: pre-wrap; word-break: break-all; font-family: inherit; margin: 0; }
}
</style>