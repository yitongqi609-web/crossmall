<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-side">
      <div class="brand-main">
        <div class="brand-logo">🌊 潮汐全球购</div>
        <h1>全球好物直邮<br />关税汇率全透明</h1>
        <p class="brand-sub">跨境电商独立站 · 前后端全链路演示平台</p>
        <ul class="features">
          <li><span class="f-icon">💱</span> 6 币种实时汇率结算,下单锁定汇率快照</li>
          <li><span class="f-icon">🛃</span> HS 编码关税试算,de minimis 免税额度判定</li>
          <li><span class="f-icon">🚚</span> 履约状态机全程追踪:备货→报关→干线→清关→妥投</li>
        </ul>
      </div>
      <div class="brand-emoji">🛍️🚢✈️📦🌏</div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-side">
      <div class="login-card">
        <el-tabs v-model="mode" stretch>
          <el-tab-pane :label="t('auth.login')" name="login" />
          <el-tab-pane :label="t('auth.register')" name="register" />
        </el-tabs>
        <el-form :model="form" label-position="top" size="large" @keyup.enter="submit">
          <el-form-item :label="t('auth.email')">
            <el-input v-model="form.email" placeholder="you@example.com" maxlength="128" :prefix-icon="Message" />
          </el-form-item>
          <el-form-item v-if="mode === 'register'" :label="t('auth.nickname')">
            <el-input v-model="form.nickname" maxlength="64" placeholder="Tide Demo" />
          </el-form-item>
          <el-form-item :label="t('auth.password')">
            <el-input v-model="form.password" type="password" show-password placeholder="6~32 位" maxlength="32"
                      :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" class="cm-btn-primary submit-btn" size="large" round :loading="submitting"
                     @click="submit">
            {{ mode === 'login' ? t('auth.login') : t('auth.register') }}
          </el-button>
        </el-form>

        <el-divider>
          <span class="text-sub">{{ locale === 'en' ? 'Fill demo account' : '演示账号一键填充' }}</span>
        </el-divider>
        <div class="demo-accounts">
          <el-button round @click="fill">👤 demo@crossmall.com</el-button>
        </div>
        <p class="text-sub demo-tip">密码 demo123 · {{ locale === 'en' ? 'Sign in to shop' : '登录后即可下单体验全链路' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Message, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const user = useUserStore()

const mode = ref('login')
const submitting = ref(false)
const form = reactive({ email: '', password: '', nickname: '' })

function fill() {
  mode.value = 'login'
  form.email = 'demo@crossmall.com'
  form.password = 'demo123'
}

async function submit() {
  if (!form.email || !form.password) {
    ElMessage.warning(`${t('auth.email')} / ${t('auth.password')}`)
    return
  }
  submitting.value = true
  try {
    if (mode.value === 'login') {
      await user.login({ email: form.email, password: form.password })
      ElMessage.success(`${t('auth.loginSuccess')}, ${user.userInfo?.nickname || ''}`)
      await user.fetchMe()
      router.push(route.query.redirect || '/')
    } else {
      await user.register({ email: form.email, password: form.password, nickname: form.nickname || null })
      ElMessage.success(t('auth.registerSuccess'))
      mode.value = 'login'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  background: linear-gradient(120deg, #eef5ff 0%, #dcecfe 55%, #b9d7fd 100%);
}

.brand-side {
  flex: 1.1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 6vw 5vw;
  position: relative;
  overflow: hidden;
}

.brand-logo {
  font-size: 26px;
  font-weight: 800;
  color: var(--cm-primary-deep);
  margin-bottom: 34px;
}

.brand-side h1 {
  font-size: 44px;
  line-height: 1.35;
  margin: 0 0 14px;
  color: #262626;
}

.brand-sub {
  color: #5a6b85;
  font-size: 15px;
  margin: 0 0 30px;
}

.features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.features li {
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid #cfe2ff;
  border-radius: 12px;
  padding: 10px 16px;
  font-size: 14px;
  color: #595959;
  backdrop-filter: blur(4px);
  width: fit-content;
}

.f-icon {
  margin-right: 8px;
}

.brand-emoji {
  font-size: 54px;
  letter-spacing: 12px;
  margin-top: 36px;
  filter: drop-shadow(0 8px 14px rgba(31, 111, 235, 0.22));
}

.form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 5vw;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 18px;
  padding: 30px 30px 22px;
  box-shadow: 0 12px 40px rgba(31, 111, 235, 0.14);
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.demo-accounts {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.demo-tip {
  text-align: center;
  margin: 12px 0 0;
}

@media (max-width: 860px) {
  .login-page {
    flex-direction: column;
  }

  .brand-side {
    padding: 48px 24px 8px;
  }

  .brand-side h1 {
    font-size: 32px;
  }

  .brand-emoji {
    display: none;
  }

  .form-side {
    padding: 24px;
  }
}
</style>
