<template>
  <el-container style="height: 100%">
    <!-- 侧边栏 -->
    <el-aside width="200px" style="background-color: #304156">
      <div class="logo">
        <h3>实训评价系统</h3>
      </div>
      <el-menu
        :default-active="$route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        
        <el-menu-item index="/report" v-if="userInfo.role !== 1">
          <el-icon><Document /></el-icon>
          <span>报告管理</span>
        </el-menu-item>
        
        <el-menu-item index="/my-course" v-if="userInfo.role === 3">
          <el-icon><Reading /></el-icon>
          <span>我的课程</span>
        </el-menu-item>
        
        <el-menu-item index="/my-score" v-if="userInfo.role === 3">
          <el-icon><DataLine /></el-icon>
          <span>我的成绩</span>
        </el-menu-item>
        
        <el-menu-item index="/report-requirement" v-if="userInfo.role === 2">
          <el-icon><DocumentAdd /></el-icon>
          <span>报告要求</span>
        </el-menu-item>
        
        <el-menu-item index="/teacher-class" v-if="userInfo.role === 2">
          <el-icon><School /></el-icon>
          <span>我的班级</span>
        </el-menu-item>

        <el-menu-item index="/teacher-course" v-if="userInfo.role === 2">
          <el-icon><Reading /></el-icon>
          <span>我的课程</span>
        </el-menu-item>
        
        <el-menu-item index="/evaluation">
          <el-icon><Star /></el-icon>
          <span>评价管理</span>
        </el-menu-item>
        
        <el-menu-item index="/statistics">
          <el-icon><DataLine /></el-icon>
          <span>统计报表</span>
        </el-menu-item>
        
        <el-menu-item index="/user" v-if="userInfo.role === 1">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        
        <el-menu-item index="/class" v-if="userInfo.role === 1">
          <el-icon><School /></el-icon>
          <span>班级管理</span>
        </el-menu-item>
        
        <el-menu-item index="/course" v-if="userInfo.role === 1">
          <el-icon><Reading /></el-icon>
          <span>课程管理</span>
        </el-menu-item>
        
        <el-menu-item index="/config" v-if="userInfo.role === 1">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <!-- 顶部 -->
      <el-header style="display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #e6e6e6">
        <div style="display: flex; align-items: center; gap: 20px;">
          <span class="role-tag" :type="roleType">{{ roleName }}</span>
          
          <!-- 通知入口 -->
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
            <el-button type="primary" link @click="goToNotifications">
              <el-icon :size="20"><Bell /></el-icon>
              <span style="margin-left: 4px;">消息</span>
            </el-button>
          </el-badge>
        </div>
        
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" style="margin-right: 8px">{{ userInfo.realName?.charAt(0) }}</el-avatar>
            <span>{{ userInfo.realName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      
      <!-- 主内容 -->
      <el-main>
        <router-view :key="$route.fullPath" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores'
import { HomeFilled, Document, School, Star, DataLine, User, Setting, Reading, ArrowDown, Bell, DocumentAdd, Warning } from '@element-plus/icons-vue'
import { getUnreadCount } from '../api/notification'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo || {})
const roleName = computed(() => {
  const roles = { 1: '管理员', 2: '教师', 3: '学生' }
  return roles[userInfo.value.role] || '未知'
})
const roleType = computed(() => {
  const types = { 1: 'danger', 2: 'primary', 3: 'success' }
  return types[userInfo.value.role] || 'info'
})

const activeMenu = computed(() => route.path)

// 未读通知数量
const unreadCount = ref(0)

const loadUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data?.count || 0
  } catch (error) {
    console.error(error)
  }
}

const goToNotifications = () => {
  router.push('/notification')
}

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

// 定时刷新未读数量
let timer = null
onMounted(() => {
  loadUnreadCount()
  timer = setInterval(loadUnreadCount, 10000) // 每10秒刷新
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2b3a4d;
  
  h3 {
    color: #fff;
    font-size: 18px;
    font-weight: normal;
  }
}

.el-aside {
  .el-menu {
    border-right: none;
  }
}

.role-tag {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}
</style>
