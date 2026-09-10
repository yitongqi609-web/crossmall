<template>
  <!-- 登录页独立全屏布局,不带导航和页脚 -->
  <router-view v-if="route.meta.standalone" />
  <template v-else>
    <AppNavbar />
    <main class="page-main">
      <router-view />
    </main>
    <footer class="page-footer">
      CrossMall · 潮汐全球购 — 多币种结算 · 关税试算 · 国际物流 · 履约状态机 · 支付幂等
    </footer>
  </template>
</template>

<script setup>
import { useRoute } from 'vue-router'
import AppNavbar from './components/AppNavbar.vue'

const route = useRoute()
</script>
