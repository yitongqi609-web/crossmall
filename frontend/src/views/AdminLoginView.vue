<template>
  <div class="login-page">
    <div class="form-side">
      <div class="login-card">
        <div class="card-logo">🌊 潮汐全球购 · 管理后台</div>
        <el-form :model="form" label-position="top" size="large" @keyup.enter="submit">
          <el-form-item :label="t('admin.username')">
            <el-input v-model="form.username" maxlength="64" :prefix-icon="User" />
          </el-form-item>
          <el-form-item :label="t('admin.password')">
            <el-input v-model="form.password" type="password" show-password maxlength="64" :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" class="cm-btn-primary submit-btn" size="large" round :loading="loading"
                     @click="submit">
            {{ t('admin.login') }}
          </el-button>
        </el-form>
        <el-divider>
          <span class="text-sub">{{ locale === 'en' ? 'Fill demo account' : '演示账号一键填充' }}</span>
        </el-divider>
        <div style="text-align: center">
          <el-button round @click="fill">🛠️ admin / admin123</el-button>
        </div>
      </div>
    </div>
    <div class="brand-side">
      <div class="brand-main">
        <div class="brand-logo">CrossMall Admin</div>
        <h1>订单履约 · 商品中台<br />汇率税则一站管理</h1>
        <ul class="features">
          <li><span class="f-icon">📊</span> GMV / 近 7 日趋势 / 状态分布 / 热销 Top5</li>
          <li><span class="f-icon">🚚</span> 状态机流转:备货→报关→干线→清关→派送→妥投</li>
          <li><span class="f-icon">🛃</span> 汇率 / 关税税则 / 物流线路灵活配置</li>
        </ul>
      </div>
      <div class="brand-emoji">📊🚚🛃📦</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { adminLogin } from '../api/admin'

const router = useRouter()
const { t, locale } = useI18n()

const form = reactive({ username: 'admin', password: '' })
const loading = ref(false)

function fill() {
  form.username = 'admin'
  form.password = 'admin123'
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning(`${t('admin.username')} / ${t('admin.password')}`)
    return
  }
  loading.value = true
  try {
    const data = await adminLogin(form)
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('admin_name', data.nickname || data.username)
    ElMessage.success(t('auth.loginSuccess'))
    router.push({ name: 'admin' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  background: #101a2c;
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
  max-width: 380px;
  background: #fff;
  border-radius: 18px;
  padding: 30px 30px 22px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}

.card-logo {
  text-align: center;
  font-size: 16px;
  font-weight: 800;
  color: var(--cm-primary-deep);
  margin-bottom: 18px;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.brand-side {
  flex: 1.1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 6vw 5vw;
}

.brand-logo {
  font-size: 20px;
  font-weight: 800;
  color: #7fb0ff;
  margin-bottom: 30px;
}

.brand-side h1 {
  font-size: 38px;
  line-height: 1.4;
  margin: 0 0 14px;
  color: #fff;
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
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(127, 176, 255, 0.25);
  border-radius: 12px;
  padding: 10px 16px;
  font-size: 14px;
  color: #b9c6dd;
  width: fit-content;
}

.f-icon {
  margin-right: 8px;
}

.brand-emoji {
  font-size: 48px;
  letter-spacing: 12px;
  margin-top: 36px;
}

@media (max-width: 860px) {
  .brand-side {
    display: none;
  }
}
</style>
