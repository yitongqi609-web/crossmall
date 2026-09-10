<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import { useCartStore } from '../store/cart'
import { useAppStore } from '../store/app'
import { CURRENCIES } from '../utils/format'
import { fxRates } from '../api/mall'

const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n()
const userStore = useUserStore()
const cartStore = useCartStore()
const appStore = useAppStore()

const keyword = ref('')
const rates = ref([])

const isLogin = computed(() => !!userStore.token)

async function loadRates() {
  try {
    rates.value = await fxRates()
  } catch (e) { /* 忽略 */ }
}
loadRates()

function onSearch() {
  router.push({ name: 'goods', query: { keyword: keyword.value || undefined } })
}

async function onCurrencyChange(currency) {
  const rate = rates.value.find(r => r.currency === currency)
  if (rate) {
    appStore.setCurrency({ currency, symbol: rate.symbol })
    // 整页刷新以切换所有展示币种
    window.location.reload()
  }
}

function onLocaleChange(value) {
  appStore.setLocale(value)
  locale.value = value
}

function onUserCommand(command) {
  if (command === 'orders') router.push({ name: 'orders' })
  else if (command === 'profile') router.push({ name: 'profile' })
  else if (command === 'admin') window.location.href = '/admin/login'
  else if (command === 'logout') {
    userStore.logout()
    ElMessage.success(t('nav.logout'))
    router.push({ name: 'home' })
  }
}

userStore.fetchMe()
if (isLogin.value) cartStore.refresh()
</script>

<template>
  <div class="mall-layout">
    <header class="mall-header">
      <div class="header-inner">
        <div class="logo" @click="router.push({ name: 'home' })">
          🌊 <span class="logo-text">TideMall</span>
          <span class="logo-sub">{{ t('nav.slogan') }}</span>
        </div>
        <div class="search-box">
          <el-input
            v-model="keyword"
            :placeholder="t('nav.searchPlaceholder')"
            clearable
            @keyup.enter="onSearch"
          >
            <template #append>
              <el-button :icon="'Search'" @click="onSearch" />
            </template>
          </el-input>
        </div>
        <div class="header-actions">
          <el-select
            :model-value="appStore.currency"
            size="default"
            style="width: 96px"
            @change="onCurrencyChange"
          >
            <el-option v-for="c in CURRENCIES" :key="c" :value="c" :label="c" />
          </el-select>
          <el-select
            :model-value="appStore.locale"
            size="default"
            style="width: 88px"
            @change="onLocaleChange"
          >
            <el-option value="zh" label="中文" />
            <el-option value="en" label="English" />
          </el-select>
          <el-badge :value="cartStore.count" :hidden="!cartStore.count" class="cart-badge">
            <el-button circle :icon="'ShoppingCart'" @click="router.push({ name: 'cart' })" />
          </el-badge>
          <template v-if="isLogin">
            <el-dropdown @command="onUserCommand">
              <span class="user-name">
                <el-icon><User /></el-icon>
                {{ userStore.userInfo?.nickname || 'Buyer' }}
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="orders">{{ t('nav.myOrders') }}</el-dropdown-item>
                  <el-dropdown-item command="profile">{{ t('nav.profile') }}</el-dropdown-item>
                  <el-dropdown-item command="admin" divided>{{ t('nav.adminEntry') }}</el-dropdown-item>
                  <el-dropdown-item command="logout">{{ t('nav.logout') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push({ name: 'login' })">{{ t('nav.login') }}</el-button>
            <el-button type="primary" plain @click="router.push({ name: 'register' })">
              {{ t('nav.register') }}
            </el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="mall-main">
      <router-view :key="route.fullPath" />
    </main>

    <footer class="mall-footer">
      <div>CrossMall · 潮汐全球购 TideMall — Global Cross-border E-commerce Demo</div>
      <div class="footer-tip">多币种 · 关税试算 · 国际物流 · 履约状态机 · 支付幂等</div>
    </footer>
  </div>
</template>

<style scoped>
.mall-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.mall-header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 20px;
}
.logo {
  cursor: pointer;
  display: flex;
  align-items: baseline;
  gap: 8px;
  white-space: nowrap;
  font-size: 22px;
}
.logo-text {
  font-weight: 700;
  color: #1f6feb;
}
.logo-sub {
  font-size: 12px;
  color: #909399;
}
.search-box {
  flex: 1;
  max-width: 420px;
}
.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}
.cart-badge {
  margin-right: 4px;
}
.user-name {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #303133;
  font-size: 14px;
}
.mall-main {
  flex: 1;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 16px;
  box-sizing: border-box;
}
.mall-footer {
  background: #1f2937;
  color: #9ca3af;
  text-align: center;
  padding: 20px;
  font-size: 13px;
  line-height: 1.8;
}
.footer-tip {
  font-size: 12px;
  color: #6b7280;
}
</style>
