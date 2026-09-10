import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由:登录/管理员登录为 standalone 全屏页;其余共用导航布局。
 * 守卫:购物车/结算/订单/收银台/个人中心 需买家登录;/admin/* 需管理员 token。
 */
const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { standalone: true } },
  { path: '/admin/login', name: 'adminLogin', component: () => import('../views/AdminLoginView.vue'), meta: { standalone: true } },
  {
    path: '/',
    children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue') },
      { path: 'goods', name: 'goods', component: () => import('../views/GoodsListView.vue') },
      { path: 'goods/:spuId', name: 'goodsDetail', component: () => import('../views/GoodsDetailView.vue') },
      { path: 'cart', name: 'cart', component: () => import('../views/CartView.vue'), meta: { auth: true } },
      { path: 'checkout', name: 'checkout', component: () => import('../views/CheckoutView.vue'), meta: { auth: true } },
      { path: 'pay/:orderNo', name: 'pay', component: () => import('../views/PayView.vue'), meta: { auth: true } },
      { path: 'orders', name: 'orders', component: () => import('../views/OrdersView.vue'), meta: { auth: true } },
      { path: 'orders/:orderNo', name: 'orderDetail', component: () => import('../views/OrderDetailView.vue'), meta: { auth: true } },
      { path: 'profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { auth: true } },
      { path: 'admin', name: 'admin', component: () => import('../views/AdminView.vue'), meta: { admin: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(to => {
  const mallToken = localStorage.getItem('mall_token')
  const adminToken = localStorage.getItem('admin_token')
  if (to.meta.auth && !mallToken) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && !adminToken) {
    return { name: 'adminLogin' }
  }
  return true
})

export default router
