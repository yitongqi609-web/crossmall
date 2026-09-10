<template>
  <header class="nav">
    <div class="nav-inner">
      <router-link to="/" class="brand">
        <span class="brand-icon">🌊</span>
        <span>潮汐全球购</span>
      </router-link>
      <nav class="links">
        <router-link to="/" :class="{ active: route.name === 'home' }">{{ t('nav.home') }}</router-link>
        <router-link to="/goods" :class="{ active: route.name === 'goods' }">{{ t('nav.allGoods') }}</router-link>
        <router-link to="/cart" :class="{ active: route.name === 'cart' }">
          {{ t('nav.cart') }}
          <el-badge v-if="cart.count" :value="cart.count" class="nav-badge" />
        </router-link>
        <router-link v-if="user.isLogin" to="/orders" :class="{ active: route.name === 'orders' }">
          {{ t('nav.myOrders') }}
        </router-link>
        <router-link v-if="isAdmin" to="/admin" :class="{ active: route.path.startsWith('/admin') }">
          {{ t('nav.adminEntry') }}
        </router-link>
      </nav>
      <div class="right">
        <el-select :model-value="app.currency" size="default" class="mini-select" @change="onCurrency">
          <template #label>
            <span class="switch-label">
              <span class="switch-tag">{{ t('nav.currencyTag') }}</span>
              <span class="switch-value">{{ app.currency }}</span>
            </span>
          </template>
          <el-option v-for="r in rates" :key="r.currency" :value="r.currency">
            <span class="opt-row">
              <span>{{ r.currency }} {{ currencyZh(r.currency) }}</span>
              <span class="option-sub">{{ r.symbol }} · {{ r.currencyEn }}</span>
            </span>
          </el-option>
        </el-select>
        <el-select :model-value="app.locale" size="default" class="mini-select" @change="onLocale">
          <template #label>
            <span class="switch-label">
              <span class="switch-tag">{{ t('nav.languageTag') }}</span>
              <span class="switch-value">{{ app.locale === 'en' ? 'EN' : '中文' }}</span>
            </span>
          </template>
          <el-option value="zh">
            <span class="opt-row">
              <span>中文</span>
              <span class="option-sub">简体中文</span>
            </span>
          </el-option>
          <el-option value="en">
            <span class="opt-row">
              <span>English</span>
              <span class="option-sub">英语</span>
            </span>
          </el-option>
        </el-select>
        <template v-if="user.isLogin">
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="26" style="background: var(--cm-gold)">{{ nickname.slice(0, 1) }}</el-avatar>
              <span class="nickname">{{ nickname }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="orders">{{ t('nav.myOrders') }}</el-dropdown-item>
                <el-dropdown-item command="profile">{{ t('nav.profile') }}</el-dropdown-item>
                <el-dropdown-item divided command="logout">{{ t('nav.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <el-button v-else type="primary" class="cm-btn-primary" round size="small" @click="goLogin">
          {{ t('nav.login') }} / {{ t('nav.register') }}
        </el-button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import { useCartStore } from '../store/cart'
import { useAppStore } from '../store/app'
import { fxRates } from '../api/mall'

const CURRENCY_ZH = { USD: '美元', EUR: '欧元', GBP: '英镑', JPY: '日元', AUD: '澳元', CNY: '人民币' }
const currencyZh = c => CURRENCY_ZH[c] || c

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const user = useUserStore()
const cart = useCartStore()
const app = useAppStore()

const rates = ref([])
const isAdmin = computed(() => !!localStorage.getItem('admin_token'))
const nickname = computed(() => user.userInfo?.nickname || 'Buyer')

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

async function onCurrency(currency) {
  const rate = rates.value.find(r => r.currency === currency)
  if (rate) {
    app.setCurrency({ currency: rate.currency, symbol: rate.symbol })
    window.location.reload()
  }
}

function onLocale(value) {
  app.setLocale(value)
  locale.value = value
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    user.logout()
    ElMessage.success(t('nav.logout'))
    router.push('/')
    return
  }
  router.push(cmd === 'orders' ? '/orders' : '/profile')
}

onMounted(async () => {
  user.fetchMe()
  if (user.isLogin) cart.refresh()
  try {
    rates.value = await fxRates()
  } catch (e) { /* 忽略 */ }
})
</script>

<style scoped>
.nav {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 16px;
  height: 58px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 19px;
  font-weight: 800;
  color: var(--cm-primary-deep);
  text-decoration: none;
  white-space: nowrap;
}

.brand-icon {
  font-size: 22px;
}

.links {
  display: flex;
  gap: 20px;
  flex: 1;
  align-items: center;
}

.links a {
  color: #595959;
  text-decoration: none;
  font-size: 14px;
  padding: 4px 2px;
  border-bottom: 2px solid transparent;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.links a.active,
.links a:hover {
  color: var(--cm-primary-deep);
  border-bottom-color: var(--cm-primary);
}

.nav-badge {
  transform: translateY(-8px);
}

.right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mini-select {
  width: 96px;
}

/* 允许触发器显示两行(说明 + 值) */
.mini-select :deep(.el-select__wrapper) {
  min-height: 46px;
  padding-top: 4px;
  padding-bottom: 4px;
}

.mini-select :deep(.el-select__selection) {
  overflow: visible;
}

/* 两行切换器:上行小字说明(币种/语言),下行加粗值 */
.switch-label {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.25;
  gap: 1px;
  white-space: nowrap;
  overflow: visible;
  width: max-content;
}

.switch-tag {
  font-size: 10px;
  color: #8c8c8c;
  transform: scale(0.92);
  transform-origin: left center;
}

.switch-value {
  font-size: 13px;
  font-weight: 700;
  color: var(--cm-primary-deep);
}

.opt-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
}

.option-sub {
  color: #8c8c8c;
  font-size: 12px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.nickname {
  font-size: 14px;
  color: #262626;
}
</style>
