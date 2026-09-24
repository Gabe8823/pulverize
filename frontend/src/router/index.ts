import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据看板' },
      },
      {
        path: 'activities',
        name: 'Activities',
        component: () => import('@/views/ActivityList.vue'),
        meta: { title: '跑步记录' },
      },
      {
        path: 'activities/:id',
        name: 'ActivityDetail',
        component: () => import('@/views/ActivityDetail.vue'),
        meta: { title: '跑步详情' },
      },
      {
        path: 'analysis',
        name: 'Analysis',
        component: () => import('@/views/Analysis.vue'),
        meta: { title: '跑步分析' },
      },
      {
        path: 'connect',
        name: 'Connect',
        component: () => import('@/views/Connect.vue'),
        meta: { title: '连接' },
      },
      {
        path: 'plan',
        name: 'PlanList',
        component: () => import('@/views/PlanList.vue'),
        meta: { title: '训练计划' },
      },
      {
        path: 'plan/generate',
        name: 'PlanGenerate',
        component: () => import('@/views/PlanGenerate.vue'),
        meta: { title: '生成计划' },
      },
      {
        path: 'plan/:id',
        name: 'PlanDetail',
        component: () => import('@/views/PlanDetail.vue'),
        meta: { title: '计划详情' },
      },
      {
        path: 'goal',
        name: 'Goal',
        component: () => import('@/views/GoalPage.vue'),
        meta: { title: '运动目标' },
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/Report.vue'),
        meta: { title: '训练报告' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人资料' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !token) {
    next('/login')
  } else if (!requiresAuth && token && (to.path === '/login' || to.path === '/register')) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
