<template>
  <div class="page-container">
    <h2 class="page-title">我的课程</h2>
    
    <!-- 通知公告 -->
    <div v-if="notifications.length > 0" class="notification-section">
      <el-alert
        v-for="item in notifications"
        :key="item.id"
        :title="item.title"
        :description="item.content"
        type="info"
        show-icon
        :closable="false"
        class="notification-item"
      >
        <template #default>
          <span style="color: #909399; font-size: 12px;">{{ item.createTime }}</span>
          <el-button type="primary" size="small" @click="viewRequirement(item)" style="margin-left: 10px;">查看详情</el-button>
        </template>
      </el-alert>
    </div>
    
    <div class="course-container">
      <!-- 已选课程 -->
      <div class="section">
        <h3 class="section-title">已选修课程</h3>
        <div v-if="selectedCourses.length > 0" class="course-list">
          <el-card v-for="course in selectedCourses" :key="course.id" class="course-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="course-name">{{ course.courseName }}</span>
                <el-tag type="success">已选修</el-tag>
              </div>
            </template>
            <div class="course-info">
              <p v-if="course.teacherName"><strong>授课教师：</strong>{{ course.teacherName }}</p>
              <p><strong>课程描述：</strong>{{ course.description || '暂无描述' }}</p>
              <p><strong>创建时间：</strong>{{ course.createTime }}</p>
            </div>
            <template #footer>
              <div class="card-footer">
                <el-button type="primary" size="small" @click="goToReport(course)">查看报告</el-button>
                <el-button type="warning" size="small" @click="viewCourseRequirements(course)">报告要求</el-button>
                <el-button type="danger" size="small" @click="handleCancel(course)">取消选课</el-button>
              </div>
            </template>
          </el-card>
        </div>
        <el-empty v-else description="您还没有选修任何课程" />
      </div>
      
      <!-- 可选课程 -->
      <div class="section">
        <h3 class="section-title">可选课程</h3>
        <div v-if="availableCourses.length > 0" class="course-list">
          <el-card v-for="course in availableCourses" :key="course.id" class="course-card available" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="course-name">{{ course.courseName }}</span>
                <el-tag type="info">可选</el-tag>
              </div>
            </template>
            <div class="course-info">
              <p v-if="course.teacherName"><strong>授课教师：</strong>{{ course.teacherName }}</p>
              <p><strong>课程描述：</strong>{{ course.description || '暂无描述' }}</p>
            </div>
            <template #footer>
              <div class="card-footer">
                <el-button type="primary" @click="handleSelect(course)">选修此课程</el-button>
              </div>
            </template>
          </el-card>
        </div>
        <el-empty v-else description="暂无可选课程" />
      </div>
    </div>

    <!-- 课程报告要求对话框 -->
    <el-dialog v-model="requirementDialogVisible" :title="'课程报告要求 - ' + currentCourse?.courseName" width="700px">
      <div v-if="courseRequirements.length > 0" class="requirement-list">
        <el-card 
          v-for="req in courseRequirements" 
          :key="req.id" 
          class="requirement-card"
          :class="{ 'overdue': isOverdue(req.deadline) }"
        >
          <template #header>
            <div class="requirement-header">
              <span class="requirement-title">{{ req.title }}</span>
              <el-tag :type="isOverdue(req.deadline) ? 'danger' : 'success'" size="small">
                {{ isOverdue(req.deadline) ? '已截止' : '进行中' }}
              </el-tag>
            </div>
          </template>
          <div class="requirement-content">
            <p><strong>截止时间：</strong>{{ req.deadline || '未设置' }}</p>
            <p><strong>详细内容：</strong></p>
            <div class="content-text">{{ req.content || '无详细内容' }}</div>
          </div>
          <template #footer>
            <div class="requirement-footer">
              <el-button type="success" @click="goToSubmitReport(req)">提交报告</el-button>
            </div>
          </template>
        </el-card>
      </div>
      <el-empty v-else description="暂无报告要求" />
      <template #footer>
        <el-button @click="requirementDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 报告要求详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="报告要求详情" width="700px">
      <div v-if="currentRequirement">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报告标题" :span="2">{{ currentRequirement.title }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">
            <span :class="{ 'overdue-text': isOverdue(currentRequirement.deadline) }">
              {{ currentRequirement.deadline || '未设置' }}
              <el-tag v-if="isOverdue(currentRequirement.deadline)" type="danger" size="small" style="margin-left: 8px;">已截止</el-tag>
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ currentRequirement.createTime }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="content-section">
          <h4>报告要求内容</h4>
          <div class="content-box">{{ currentRequirement.content || '无详细内容' }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button v-if="currentRequirement && !isOverdue(currentRequirement.deadline)" type="primary" @click="goToSubmitReport(currentRequirement)">
          提交报告
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { getAvailableCourses, selectCourse, cancelCourse, getMyCourses } from '../api/course'
import { getCourseRequirements } from '../api/reportRequirement'
import { getNotificationList, markAsRead } from '../api/notification'

const router = useRouter()
const selectedCourses = ref([])
const availableCourses = ref([])
const notifications = ref([])
const courseRequirements = ref([])
const requirementDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentCourse = ref(null)
const currentRequirement = ref(null)

const loadMyCourses = async () => {
  try {
    const res = await getMyCourses()
    selectedCourses.value = res.data || []
    // 加载可选课程后过滤掉已选的
    loadAvailableCourses()
  } catch (error) {
    console.error(error)
  }
}

const loadAvailableCourses = async () => {
  try {
    const res = await getAvailableCourses()
    const selectedIds = selectedCourses.value.map(c => c.id)
    availableCourses.value = (res.data || []).filter(c => !selectedIds.includes(c.id))
  } catch (error) {
    console.error(error)
  }
}

// 加载通知
const loadNotifications = async () => {
  try {
    const res = await getNotificationList({ pageNum: 1, pageSize: 10 })
    // 只显示未读的报告通知
    notifications.value = (res.data?.records || []).filter(n => n.type === 1 && n.isRead === 0)
  } catch (error) {
    console.error(error)
  }
}

// 查看通知详情
const viewRequirement = async (notification) => {
  try {
    await markAsRead(notification.id)
    // 从通知中获取课程信息
    const course = selectedCourses.value.find(c => notification.relatedId)
    if (course) {
      await viewCourseRequirements(course)
    }
    // 移除已读通知
    notifications.value = notifications.value.filter(n => n.id !== notification.id)
  } catch (error) {
    console.error(error)
  }
}

// 查看课程报告要求
const viewCourseRequirements = async (course) => {
  currentCourse.value = course
  try {
    const res = await getCourseRequirements(course.id)
    courseRequirements.value = res.data || []
    requirementDialogVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

// 检查是否已截止
const isOverdue = (deadline) => {
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

// 跳转到提交报告
const goToSubmitReport = (requirement) => {
  requirementDialogVisible.value = false
  detailDialogVisible.value = false
  router.push({ 
    path: '/report', 
    query: { 
      courseId: requirement.courseId,
      requirementId: requirement.id,
      requirementTitle: requirement.title
    } 
  })
}

const handleSelect = async (course) => {
  await ElMessageBox.confirm(`确定选修「${course.courseName}」吗？`, '选课确认', { type: 'info' })
  try {
    await selectCourse(course.id)
    ElMessage.success('选课成功')
    loadMyCourses()
  } catch (error) {
    console.error(error)
  }
}

const handleCancel = async (course) => {
  await ElMessageBox.confirm(`确定取消选修「${course.courseName}」吗？`, '取消选课', { type: 'warning' })
  try {
    await cancelCourse(course.id)
    ElMessage.success('已取消选课')
    loadMyCourses()
  } catch (error) {
    console.error(error)
  }
}

const goToReport = (course) => {
  router.push({ path: '/report', query: { courseId: course.id } })
}

onMounted(() => {
  loadMyCourses()
  loadNotifications()
})
</script>

<style scoped>
.notification-section {
  margin-bottom: 20px;
}

.notification-item {
  margin-bottom: 10px;
}

.course-container {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.section-title {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #409eff;
  color: #303133;
}

.course-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.course-card {
  transition: transform 0.2s;
}

.course-card:hover {
  transform: translateY(-4px);
}

.course-card.available {
  border: 1px dashed #409eff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.course-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.course-info p {
  margin: 8px 0;
  font-size: 14px;
  color: #606266;
}

.card-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.requirement-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 500px;
  overflow-y: auto;
}

.requirement-card {
  border-left: 4px solid #409eff;
}

.requirement-card.overdue {
  border-left-color: #f56c6c;
  opacity: 0.8;
}

.requirement-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.requirement-title {
  font-weight: bold;
  color: #303133;
}

.requirement-content p {
  margin: 8px 0;
  font-size: 14px;
  color: #606266;
}

.content-text {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  white-space: pre-wrap;
  line-height: 1.8;
}

.requirement-footer {
  display: flex;
  justify-content: flex-end;
}

.content-section {
  margin-top: 20px;
  
  h4 {
    margin-bottom: 12px;
    color: #303133;
  }
}

.content-box {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
}

.overdue-text {
  color: #f56c6c;
}
</style>
