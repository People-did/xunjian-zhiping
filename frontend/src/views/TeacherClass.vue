<template>
  <div class="page-container">
    <h2 class="page-title">我的班级</h2>
    
    <!-- 班级卡片列表 -->
    <el-row :gutter="20" class="class-row" v-loading="loading">
      <el-col :span="8" v-for="cls in classList" :key="cls.id">
        <el-card class="class-card" shadow="hover" @click="selectClass(cls)">
          <template #header>
            <div class="card-header">
              <span class="class-name">{{ cls.className }}</span>
              <el-tag type="primary">{{ cls.studentCount }} 名学生</el-tag>
            </div>
          </template>
          <div class="card-body">
            <p v-if="cls.description" class="description">{{ cls.description }}</p>
            <el-button type="primary" text @click.stop="selectClass(cls)">查看详情</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-empty v-if="!loading && classList.length === 0" description="暂无负责的班级" />
    
    <!-- 班级详情对话框 -->
    <el-dialog v-model="detailVisible" :title="currentClass?.className || '班级详情'" width="900px">
      <el-tabs v-if="currentClass">
        <!-- 班级统计 -->
        <el-tab-pane label="班级概况">
          <el-row :gutter="20" class="stat-row">
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ classDetail.studentCount || 0 }}</div>
                <div class="stat-label">学生人数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ statistics.totalReports || 0 }}</div>
                <div class="stat-label">报告总数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ statistics.evaluatedReports || 0 }}</div>
                <div class="stat-label">已评价</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ statistics.avgScore || 0 }}</div>
                <div class="stat-label">平均分</div>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
        
        <!-- 学生列表 -->
        <el-tab-pane label="学生列表">
          <el-table :data="students" stripe>
            <el-table-column prop="realName" label="姓名" />
            <el-table-column prop="username" label="学号" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column prop="phone" label="电话" />
          </el-table>
        </el-tab-pane>
        
        <!-- 报告列表 -->
        <el-tab-pane label="报告列表">
          <el-table :data="reports" stripe v-loading="reportsLoading">
            <el-table-column prop="studentName" label="学生" />
            <el-table-column prop="title" label="报告标题" show-overflow-tooltip />
            <el-table-column prop="statusName" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.statusName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalScore" label="得分" width="80">
              <template #default="{ row }">
                <span v-if="row.totalScore">{{ row.totalScore }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="180" />
          </el-table>
          <el-pagination
            v-model:current-page="reportsPage"
            :page-size="10"
            :total="reportsTotal"
            layout="prev, pager, next"
            @current-change="loadReports"
            style="margin-top: 15px; justify-content: center"
          />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTeacherClasses, getClassDetail, getClassReports, getClassStatistics } from '../api/class'

const loading = ref(false)
const detailVisible = ref(false)
const reportsLoading = ref(false)

const classList = ref([])
const currentClass = ref(null)
const classDetail = ref({})
const students = ref([])
const statistics = ref({})
const reports = ref([])
const reportsPage = ref(1)
const reportsTotal = ref(0)

const loadClasses = async () => {
  loading.value = true
  try {
    const res = await getTeacherClasses()
    classList.value = res.data || []
  } catch (error) {
    console.error(error)
    ElMessage.error('加载班级列表失败')
  } finally {
    loading.value = false
  }
}

const selectClass = async (cls) => {
  currentClass.value = cls
  detailVisible.value = true
  reportsPage.value = 1
  
  // 加载班级详情
  try {
    const detailRes = await getClassDetail(cls.id)
    classDetail.value = detailRes.data || {}
    students.value = classDetail.value.students || []
  } catch (error) {
    console.error(error)
  }
  
  // 加载统计信息
  try {
    const statRes = await getClassStatistics(cls.id)
    statistics.value = statRes.data || {}
  } catch (error) {
    console.error(error)
  }
  
  // 加载报告列表
  loadReports()
}

const loadReports = async () => {
  if (!currentClass.value) return
  
  reportsLoading.value = true
  try {
    const res = await getClassReports(currentClass.value.id, {
      pageNum: reportsPage.value,
      pageSize: 10
    })
    reports.value = res.data?.records || []
    reportsTotal.value = res.data?.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    reportsLoading.value = false
  }
}

onMounted(() => {
  loadClasses()
})
</script>

<style scoped>
.class-row {
  margin-bottom: 20px;
}

.class-card {
  margin-bottom: 20px;
  cursor: pointer;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.class-name {
  font-size: 16px;
  font-weight: bold;
}

.card-body {
  padding: 10px 0;
}

.description {
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 5px;
}
</style>
