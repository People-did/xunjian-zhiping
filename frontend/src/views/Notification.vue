<template>
  <div class="page-container">
    <h2 class="page-title">消息通知</h2>
    
    <!-- 工具栏 -->
    <div class="toolbar" v-if="pagination.total > 0">
      <el-button type="primary" @click="markAllRead">全部标为已读</el-button>
    </div>
    
    <!-- 通知列表 -->
    <div class="notification-list" v-loading="loading">
      <el-card 
        v-for="item in tableData" 
        :key="item.id" 
        class="notification-card"
        :class="{ unread: item.isRead === 0 }"
        shadow="hover"
        @click="handleClick(item)"
      >
        <div class="notification-content">
          <div class="notification-icon">
            <el-badge is-dot :hidden="item.isRead === 1">
              <el-icon :size="24" :color="item.type === 1 ? '#409eff' : '#67c23a'">
                <Bell v-if="item.type === 1" />
                <Warning v-else />
              </el-icon>
            </el-badge>
          </div>
          <div class="notification-text">
            <div class="notification-title">{{ item.title }}</div>
            <div class="notification-body">{{ item.content }}</div>
            <div class="notification-time">{{ item.createTime }}</div>
          </div>
          <div class="notification-action">
            <el-tag :type="item.type === 1 ? 'primary' : 'success'" size="small">
              {{ item.type === 1 ? '报告通知' : '系统通知' }}
            </el-tag>
          </div>
        </div>
      </el-card>
      
      <el-empty v-if="!loading && tableData.length === 0" description="暂无通知" />
    </div>
    
    <!-- 分页 -->
    <div class="pagination-container" v-if="pagination.total > 0">
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
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Bell, Warning } from '@element-plus/icons-vue'
import { getNotificationList, markAsRead, markAllAsRead } from '../api/notification'
import { useUserStoreHook } from '../stores'

const router = useRouter()
const userStore = useUserStoreHook()
const loading = ref(false)
const tableData = ref([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getNotificationList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleClick = async (item) => {
  if (item.isRead === 0) {
    try {
      await markAsRead(item.id)
      item.isRead = 1
    } catch (error) {
      console.error(error)
    }
  }
  
  // 如果是报告通知且有关联ID，跳转到报告要求
  if (item.type === 1 && item.relatedId) {
    router.push({ path: '/my-course' })
  }
}

const markAllRead = async () => {
  try {
    await markAllAsRead()
    ElMessage.success('已全部标为已读')
    loadData()
  } catch (error) {
    console.error(error)
  }
}

// 定时刷新通知
let timer = null
onMounted(() => {
  loadData()
  // 每30秒刷新一次
  timer = setInterval(loadData, 30000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-card {
  cursor: pointer;
  transition: all 0.3s;
}

.notification-card:hover {
  transform: translateX(4px);
}

.notification-card.unread {
  border-left: 4px solid #409eff;
  background: #f0f9ff;
}

.notification-content {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.notification-icon {
  flex-shrink: 0;
  padding-top: 4px;
}

.notification-text {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.notification-body {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
  white-space: pre-wrap;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.notification-action {
  flex-shrink: 0;
}
</style>
