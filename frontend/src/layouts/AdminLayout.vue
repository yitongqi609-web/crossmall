<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const adminName = ref(localStorage.getItem('admin_name') || 'Admin')

const menus = [
  { path: '/admin', key: 'dashboard', icon: 'DataBoard' },
  { path: '/admin/goods', key: 'goods', icon: 'Goods' },
  { path: '/admin/category', key: 'category', icon: 'Menu' },
  { path: '/admin/fx', key: 'fx', icon: 'Money' },
  { path: '/admin/tax', key: 'tax', icon: 'Tickets' },
  { path: '/admin/logistics', key: 'logistics', icon: 'Van' },
  { path: '/admin/orders', key: 'order', icon: 'List' }
]

const activeMenu = ref(route.path)

function onSelect(path) {
  router.push(path)
}

function logout() {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_name')
  ElMessage.success(t('admin.logout'))
  router.push({ name: 'adminLogin' })
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="210px" class="admin-aside">
      <div class="admin-logo" @click="router.push('/')">🌊 TideMall</div>
      <el-menu :default-active="activeMenu" background-color="#001529" text-color="#a6adb4"
               active-text-color="#ffffff" @select="onSelect">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ t('admin.' + m.key) }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <span class="admin-title">{{ t('admin.title') }}</span>
        <div class="admin-user">
          <el-icon><User /></el-icon>
          {{ adminName }}
          <el-button size="small" text type="danger" @click="logout">{{ t('admin.logout') }}</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100vh;
}
.admin-aside {
  background: #001529;
}
.admin-logo {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  padding: 18px 20px;
  cursor: pointer;
}
.admin-aside :deep(.el-menu) {
  border-right: none;
}
.admin-aside :deep(.el-menu-item.is-active) {
  background: #1f6feb !important;
}
.admin-header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.admin-title {
  font-size: 15px;
  font-weight: 600;
}
.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.admin-main {
  background: #f5f6f8;
}
</style>
