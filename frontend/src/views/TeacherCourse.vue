<template>
  <div class="page-container">
    <h2 class="page-title">我的课程</h2>
    
    <!-- 工具栏：班级筛选 + 报告管理 -->
    <div class="toolbar">
      <div class="filter-group">
        <span class="filter-label">授课班级：</span>
        <el-select v-model="selectedClassId" placeholder="全部班级" clearable style="width: 200px;">
          <el-option v-for="cls in classOptions" :key="cls.id" :label="cls.className" :value="cls.id" />
        </el-select>
      </div>
      <div class="action-group">
        <el-button type="success" @click="goToAllReports">报告管理（查看所有）</el-button>
      </div>
    </div>
    
    <!-- 课程列表 -->
    <div class="course-list" v-loading="loading">
      <el-empty v-if="!loading && filteredCourses.length === 0" :description="selectedClassId ? '该班级暂无课程' : '暂无分配的课程'" />
      
      <el-table v-else :data="filteredCourses" stripe>
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="courseCode" label="课程编号" width="150" />
        <el-table-column prop="className" label="授课班级" width="200" />
        <el-table-column prop="credit" label="学分" width="80" align="center" />
        <el-table-column prop="studentCount" label="学生人数" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ row.studentCount || 0 }} 人</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportCount" label="已提交报告" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.reportCount > 0 ? 'success' : 'warning'">{{ row.reportCount || 0 }} 份</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button type="success" size="small" @click="goToCourseReport(row)">查看报告</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="课程详情" width="600px">
      <el-descriptions :column="2" border v-if="currentCourse">
        <el-descriptions-item label="课程名称" :span="2">{{ currentCourse.courseName }}</el-descriptions-item>
        <el-descriptions-item label="课程编号">{{ currentCourse.courseCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="授课班级">{{ currentCourse.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学分">{{ currentCourse.credit || '-' }}</el-descriptions-item>
        <el-descriptions-item label="课时">{{ currentCourse.hours || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentCourse.status === 1 ? 'success' : 'info'">
            {{ currentCourse.status === 1 ? '进行中' : '已结束' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="分配时间">{{ currentCourse.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="课程描述" :span="2">
          {{ currentCourse.description || '暂无描述' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="goToCourseReport(currentCourse)">查看报告</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyCourses, getCourseStudents } from '../api/course'
import { getReportList } from '../api/report'
import { getAllClasses } from '../api/class'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const allClasses = ref([])
const selectedClassId = ref(null)
const detailVisible = ref(false)
const currentCourse = ref(null)

// 班级下拉选项
const classOptions = computed(() => {
  return allClasses.value.filter(cls => 
    tableData.value.some(course => course.classId === cls.id)
  )
})

// 根据班级筛选课程
const filteredCourses = computed(() => {
  if (!selectedClassId.value) {
    return tableData.value
  }
  return tableData.value.filter(course => course.classId === selectedClassId.value)
})

const loadClasses = async () => {
  try {
    const res = await getAllClasses()
    allClasses.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    // 获取课程列表
    const courseRes = await getMyCourses()
    const courses = courseRes.data || []
    
    // 获取班级列表
    await loadClasses()
    
    // 补充班级名称和学生数量
    const result = []
    for (const course of courses) {
      // 查找班级名称
      const classInfo = allClasses.value.find(c => c.id === course.classId)
      
      // 获取学生数量
      let studentCount = 0
      if (course.classId) {
        try {
          const studentRes = await getCourseStudents(course.classId)
          studentCount = studentRes.data?.length || 0
        } catch (e) {
          console.error(e)
        }
      }
      
      // 获取报告数量
      let reportCount = 0
      try {
        const reportRes = await getReportList({ pageNum: 1, pageSize: 1, courseId: course.id })
        reportCount = reportRes.data?.total || 0
      } catch (e) {
        console.error(e)
      }
      
      result.push({
        ...course,
        className: classInfo?.className || '-',
        studentCount,
        reportCount
      })
    }
    
    tableData.value = result
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const viewDetail = (course) => {
  currentCourse.value = course
  detailVisible.value = true
}

// 查看当前课程（筛选后）的报告
const goToCourseReport = (course) => {
  router.push({ path: '/report', query: { courseId: course.id } })
}

// 查看所有报告
const goToAllReports = () => {
  router.push({ path: '/report' })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-weight: 500;
  color: #606266;
}

.action-group {
  display: flex;
  gap: 10px;
}

.course-list {
  margin-top: 0;
}
</style>
