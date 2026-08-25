import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../../api/client'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../../pages/auth/LoginPage.vue') },
    { path: '/', name: 'dashboard', component: () => import('../../pages/dashboard/DashboardPage.vue') },
    { path: '/releases', name: 'releases', component: () => import('../../pages/releases/ReleaseListPage.vue') },
    { path: '/alerts', name: 'alerts', component: () => import('../../pages/alerts/AlertListPage.vue') }
  ]
})

// 简单路由守卫：未登录跳转登录页
router.beforeEach((to) => {
  if (to.name !== 'login' && !getToken()) {
    return { name: 'login' }
  }
})
