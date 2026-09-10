<template>
  <div v-if="payInfo" class="pay-wrap">
    <div class="card pay-card">
      <div class="pay-title">💳 {{ t('pay.title') }}({{ locale === 'en' ? 'Mock' : '模拟' }})</div>
      <div class="pay-amount price">{{ money(payInfo.symbol, payInfo.amountLocal) }}</div>
      <div class="text-sub">{{ t('order.no') }} {{ orderNo }} · {{ payInfo.channel === 'PAYPAL' ? t('pay.paypal') : t('pay.card') }}</div>
      <div class="text-sub" style="margin-top: 6px">
        ≈ ${{ (payInfo.amountLocal / (fxRate || 1) / 100).toFixed(2) }} USD · 1 USD = {{ fxRate }} {{ payInfo.currency }}
      </div>

      <div class="channel-row">
        <div class="channel-card" :class="{ active: channel === 'PAYPAL' }"
             @click="channel = 'PAYPAL'; createPay()">
          <div class="channel-icon">🅿️</div>
          <div>PayPal</div>
        </div>
        <div class="channel-card" :class="{ active: channel === 'CARD' }"
             @click="channel = 'CARD'; createPay()">
          <div class="channel-icon">💳</div>
          <div>{{ t('pay.card') }}</div>
        </div>
      </div>

      <div class="head-tip" style="margin-top: 16px">
        ⏰ {{ t('pay.payIn') }}
        <b style="color: var(--cm-danger); font-variant-numeric: tabular-nums">
          {{ String(Math.floor(remainSeconds / 60)).padStart(2, '0') }}:{{ String(remainSeconds % 60).padStart(2, '0') }}
        </b>
        · {{ locale === 'en'
          ? 'mock gateway posts a signed async callback (idempotent replay allowed)'
          : '模拟网关签名异步回调,可重复回调验证幂等' }}
      </div>

      <div style="display: flex; gap: 10px; justify-content: center; margin-top: 18px">
        <el-button round @click="$router.push({ name: 'orders' })">{{ locale === 'en' ? 'Pay Later' : '稍后支付' }}</el-button>
        <el-button type="primary" round class="cm-btn-primary" size="large" :loading="paying"
                   :disabled="remainSeconds <= 0" @click="payNow">
          {{ t('pay.payNow') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { orderDetail, payCreate, mockPaySubmit } from '../api/mall'
import { money } from '../utils/format'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()

const orderNo = route.params.orderNo
const payInfo = ref(null)
const fxRate = ref(1)
const channel = ref('PAYPAL')
const paying = ref(false)
const remainSeconds = ref(0)
let timer = null

function startCountdown(expireAt) {
  if (!expireAt) return
  clearInterval(timer)
  const target = new Date(expireAt.includes('T') ? expireAt : expireAt.replace(/-/g, '/')).getTime()
  const tick = () => {
    const diff = Math.floor((target - Date.now()) / 1000)
    remainSeconds.value = Number.isFinite(diff) ? Math.max(0, diff) : 0
    if (remainSeconds.value <= 0) clearInterval(timer)
  }
  tick()
  timer = setInterval(tick, 1000)
}

async function createPay() {
  payInfo.value = await payCreate(orderNo, channel.value)
  startCountdown(payInfo.value.expireAt)
}

async function payNow() {
  paying.value = true
  try {
    await mockPaySubmit(payInfo.value.txnNo)
    ElMessage.success(t('pay.success'))
    router.replace({ name: 'orderDetail', params: { orderNo } })
  } finally {
    paying.value = false
  }
}

onMounted(async () => {
  const order = await orderDetail(orderNo)
  if (order.status !== 'PENDING_PAYMENT') {
    router.replace({ name: 'orderDetail', params: { orderNo } })
    return
  }
  fxRate.value = Number(order.fxRate)
  await createPay()
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.pay-wrap {
  display: flex;
  justify-content: center;
  padding-top: 24px;
}

.pay-card {
  width: 460px;
  text-align: center;
  padding: 28px;
}

.pay-title {
  font-weight: 700;
  font-size: 16px;
}

.pay-amount {
  font-size: 34px;
  margin: 10px 0 4px;
}

.channel-row {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.channel-card {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  text-align: center;
  padding: 14px;
  cursor: pointer;
  transition: all 0.15s;
  font-size: 13px;
}

.channel-card.active {
  border-color: var(--cm-primary);
  background: var(--cm-primary-light);
}

.channel-icon {
  font-size: 24px;
  margin-bottom: 4px;
}
</style>
