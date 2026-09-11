<template>
  <div class="page-container">
    <h2 class="page-title">课程管理</h2>
    
    <!-- 搜索/筛选 -->
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="班级">
          <el-select v-model="searchForm.classId" placeholder="全部" clearable>
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="searchForm.courseName" placeholder="请输入课程名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 表格 -->
    <div class="table-container">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">新增课程</el-button>
      </div>
      
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="teacherName" label="授课教师" />
        <el-table-column prop="className" label="所属班级" />
        <el-table-column prop="description" label="课程描述" show-overflow-tooltip />
        <el-table-column prop="totalStudents" label="选修人数" width="100" align="center">
          <template #default="{ row }">
            <span>{{ row.totalStudents || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalReports" label="报告数" width="100" align="center" />
        <el-table-column prop="evaluatedReports" label="已评价" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="success">{{ row.evaluatedReports || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="avgScore" label="平均分" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.avgScore" class="score">{{ row.avgScore.toFixed(1) }}</span>
            <span v-else class="no-score">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; justify-content: center; gap: 8px;">
              <el-button 
                type="primary" 
                size="small"
                plain
                icon="InfoFilled" 
                @click="handleView(row)"
              >详情</el-button>
              
              <el-button 
                type="primary" 
                size="small"
                plain
                icon="Edit" 
                @click="handleEdit(row)"
              >编辑</el-button>
              
              <el-button 
                type="danger" 
                size="small" 
                plain
                icon="Delete" 
                @click="handleDelete(row)"
              >删除</el-button>
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
    
    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="授课教师" prop="teacherId">
          <el-select v-model="form.teacherId" placeholder="请选择授课教师" clearable>
            <el-option v-for="t in teachers" :key="t.id" :label="t.realName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属班级" prop="classId">
          <el-select v-model="form.classId" placeholder="请选择所属班级" clearable>
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入课程描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="课程详情" width="600px">
      <div v-if="currentCourse">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="课程名称">{{ currentCourse.courseName }}</el-descriptions-item>
          <el-descriptions-item label="授课教师">{{ currentCourse.teacherName }}</el-descriptions-item>
          <el-descriptions-item label="所属班级">{{ currentCourse.className }}</el-descriptions-item>
          <el-descriptions-item label="选修人数">{{ currentCourse.totalStudents }}</el-descriptions-item>
          <el-descriptions-item label="提交报告数">{{ currentCourse.totalReports }}</el-descriptions-item>
          <el-descriptions-item label="已评价报告">{{ currentCourse.evaluatedReports }}</el-descriptions-item>
          <el-descriptions-item label="平均分">
            <span v-if="currentCourse.avgScore" class="score">{{ currentCourse.avgScore.toFixed(1) }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentCourse.createTime }}</el-descriptions-item>
          <el-descriptions-item label="课程描述" :span="2">{{ currentCourse.description }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCourseList, addCourse, updateCourse, deleteCourse, getCourse } from '../api/course'
import { getAllClasses } from '../api/class'
import { getTeachers } from '../api/user'

const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const tableData = ref([])
const classes = ref([])
const teachers = ref([])
const currentCourse = ref(null)

const searchForm = reactive({ classId: null, courseName: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const form = reactive({
  id: null,
  courseName: '',
  teacherId: null,
  classId: null,
  description: ''
})

const formRules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请选择授课教师', trigger: 'change' }],
  classId: [{ required: true, message: '请选择所属班级', trigger: 'change' }]
}

const dialogTitle = ref('新增课程')

const loadData = async () => {
  loading.value = true
  try {
    const params = { ...searchForm, ...pagination }
    const res = await getCourseList(params)
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

const loadTeachers = async () => {
  try {
    const res = await getTeachers()
    teachers.value = res.data || []
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
  searchForm.courseName = ''
  handleSearch()
}

const handleAdd = () => {
  dialogTitle.value = '新增课程'
  form.id = null
  form.courseName = ''
  form.teacherId = null
  form.classId = null
  form.description = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑课程'
  form.id = row.id
  form.courseName = row.courseName
  form.teacherId = row.teacherId
  form.classId = row.classId
  form.description = row.description
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    const res = await getCourse(row.id)
    currentCourse.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    if (form.id) {
      await updateCourse(form)
      ElMessage.success('更新成功')
    } else {
      await addCourse(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该课程？删除后关联的选课记录也会被删除。', '提示', { type: 'warning' })
  try {
    await deleteCourse(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  loadData()
  loadClasses()
  loadTeachers()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}

.score {
  font-weight: bold;
  color: #409eff;
}

.no-score {
  color: #999;
}
</style>
