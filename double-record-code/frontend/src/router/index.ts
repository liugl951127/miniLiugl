/**
 * Vue Router 配置
 */
import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/order/list',
  },
  {
    path: '/order',
    name: 'OrderLayout',
    component: () => import('@/views/Layout.vue'),
    children: [
      {
        path: 'list',
        name: 'OrderList',
        component: () => import('@/views/OrderList.vue'),
        meta: { title: '订单列表' },
      },
      {
        path: 'create',
        name: 'OrderCreate',
        component: () => import('@/views/OrderCreate.vue'),
        meta: { title: '创建订单' },
      },
      {
        path: ':orderId',
        name: 'DualRecord',
        component: () => import('@/views/DualRecord.vue'),
        meta: { title: '双录办理' },
      },
    ],
  },
  {
    path: '/qa',
    name: 'QADashboard',
    component: () => import('@/views/QADashboard.vue'),
    meta: { title: '质检驾驶舱' },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, _from, next) => {
  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - 双录一体化平台`;
  }
  // 简单的登录态校验
  const token = localStorage.getItem('auth_token');
  if (to.name !== 'Login' && !token) {
    // next({ name: 'Login' });
  }
  next();
});

export default router;
