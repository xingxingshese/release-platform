import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../../api/client'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../../pages/auth/LoginPage.vue') },
    {
      path: '/',
      component: () => import('../../layouts/MainLayout.vue'),
      children: [
        { path: '', name: 'dashboard', component: () => import('../../pages/dashboard/DashboardPage.vue') },
        { path: 'releases', name: 'releases', component: () => import('../../pages/releases/ReleaseListPage.vue') },
        {
          path: 'releases/:id',
          name: 'release-detail',
          component: () => import('../../pages/releases/ReleaseDetailPage.vue')
        },
        { path: 'projects', name: 'projects', component: () => import('../../pages/projects/ProjectListPage.vue') },
        {
          path: 'requirements',
          name: 'requirements',
          component: () => import('../../pages/requirements/RequirementListPage.vue')
        },
        { path: 'alerts', name: 'alerts', component: () => import('../../pages/alerts/AlertListPage.vue') },
        {
          path: 'admin/configs',
          name: 'admin-configs',
          component: () => import('../../pages/admin/ConfigCenterPage.vue')
        }
      ]
    }
  ]
})

// 简单路由守卫：未登录跳转登录页
router.beforeEach((to) => {
  if (to.name !== 'login' && !getToken()) {
    return { name: 'login' }
  }
})
