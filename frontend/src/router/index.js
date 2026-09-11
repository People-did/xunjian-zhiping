import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('../views/User.vue'),
        meta: { title: '用户管理', roles: [1] }
      },
      {
        path: 'class',
        name: 'Class',
        component: () => import('../views/Class.vue'),
        meta: { title: '班级管理', roles: [1] }
      },
      {
        path: 'course',
        name: 'Course',
        component: () => import('../views/Course.vue'),
        meta: { title: '课程管理', roles: [1] }
      },
      {
        path: 'teacher-class',
        name: 'TeacherClass',
        component: () => import('../views/TeacherClass.vue'),
        meta: { title: '我的班级', roles: [2] }
      },
      {
        path: 'teacher-course',
        name: 'TeacherCourse',
        component: () => import('../views/TeacherCourse.vue'),
        meta: { title: '我的课程', roles: [2] }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('../views/Report.vue'),
        meta: { title: '报告管理' }
      },
      {
        path: 'my-course',
        name: 'StudentCourse',
        component: () => import('../views/StudentCourse.vue'),
        meta: { title: '我的课程', roles: [3] }
      },
      {
        path: 'my-score',
        name: 'StudentScore',
        component: () => import('../views/StudentScore.vue'),
        meta: { title: '我的成绩', roles: [3] }
      },
      {
        path: 'evaluation',
        name: 'Evaluation',
        component: () => import('../views/Evaluation.vue'),
        meta: { title: '评价管理' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('../views/Statistics.vue'),
        meta: { title: '统计报表' }
      },
        {
        path: 'config',
        name: 'Config',
        component: () => import('../views/Config.vue'),
        meta: { title: '系统设置', roles: [1] }
      },
      {
        path: 'report-requirement',
        name: 'ReportRequirement',
        component: () => import('../views/ReportRequirement.vue'),
        meta: { title: '报告要求', roles: [2] }
      },
      {
        path: 'report-requirement/edit',
        name: 'ReportRequirementEdit',
        component: () => import('../views/ReportRequirementEdit.vue'),
        meta: { title: '编辑报告要求' }
      },
      {
        path: 'notification',
        name: 'Notification',
        component: () => import('../views/Notification.vue'),
        meta: { title: '消息通知' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 实训评价系统` : '实训评价系统'
  
  const userStore = useUserStore()
  const token = userStore.token
  const userInfo = userStore.userInfo
  
  if (to.path !== '/login') {
    if (!token) {
      next('/login')
      return
    }
    
    // 权限校验 - 检查meta.roles是否包含用户角色
    if (to.meta.roles && to.meta.roles.length > 0) {
      if (!to.meta.roles.includes(userInfo?.role)) {
        next('/dashboard')
        return
      }
    }
  }
  
  next()
})

export default router
