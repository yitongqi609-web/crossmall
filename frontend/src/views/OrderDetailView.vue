<template>
  <div v-if="order">
    <section class="card head-card">
      <div class="head-left">
        <div class="head-no">📦 {{ order.orderNo }}</div>
        <div style="margin-top: 8px">
          <el-tag :type="STATUS_TAG[order.status]" effect="plain">
            {{ locale === 'en' ? order.statusLabelEn : order.statusLabel }}
          </el-tag>
        </div>
        <div v-if="order.cancelReason" class="text-sub" style="margin-top: 8px">
          {{ t('order.reason') }}: {{ order.cancelReason }}
        </div>
      </div>
      <div class="head-right">
        <div class="price" style="font-size: 26px">{{ money(order.displaySymbol, order.totalDisplayPrice) }}</div>
        <div class="text-sub">
          ≈ ${{ (order.totalFeeCents / 100).toFixed(2) }} = {{ t('checkout.goodsFee') }} ${{ (order.goodsFeeCents / 100).toFixed(2) }}
          + {{ t('checkout.shippingFee') }} ${{ (order.shippingFeeCents / 100).toFixed(2) }}
          + {{ t('checkout.taxFee') }} ${{ (order.taxFeeCents / 100).toFixed(2) }}
        </div>
        <div class="text-sub">{{ t('order.fxSnapshot') }}: 1 USD = {{ order.fxRate }} {{ order.currency }}</div>
        <div style="margin-top: 12px; display: flex; gap: 8px; justify-content: flex-end">
          <el-button v-if="order.status === 'PENDING_PAYMENT'" size="small" round class="cm-btn-primary"
                     type="primary" @click="$router.push(`/pay/${order.orderNo}`)">
            {{ t('order.goPay') }}
          </el-button>
        </div>
      </div>
    </section>

    <div class="grid-2">
      <section class="card">
        <h3 style="margin: 0 0 14px">🚚 {{ t('order.tracks') }}</h3>
        <el-timeline v-if="order.tracks && order.tracks.length">
          <el-timeline-item v-for="(track, i) in order.tracks" :key="i"
                            :timestamp="`${track.occurredAt} ${track.location || ''}`"
                            :type="i === order.tracks.length - 1 ? 'primary' : undefined">
            <b>{{ trackName(track.status) }}</b>
            <div class="text-sub" style="margin-top: 2px">
              {{ locale === 'en' ? track.descriptionEn : track.description }}
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else :image-size="60" :description="locale === 'en' ? 'No track yet' : '暂无轨迹'" />
      </section>

      <div>
        <section class="card">
          <h3 style="margin: 0 0 10px">📍 {{ t('order.address') }}</h3>
          <div class="kv"><span>{{ t('address.receiver') }}</span><b>{{ order.address.receiverName }}</b></div>
          <div class="kv"><span>{{ t('address.phone') }}</span><b>{{ order.address.phone }}</b></div>
          <div class="kv"><span>{{ t('address.country') }}</span>
            <b>{{ order.address.countryName }} ({{ order.address.countryCode }})</b></div>
          <div class="kv"><span>{{ t('address.street') }}</span>
            <b>{{ order.address.stateProvince || '-' }} · {{ order.address.city }} · {{ order.address.street }} · {{ order.address.postcode }}</b></div>
          <div class="kv"><span>{{ t('order.line') }}</span><b>{{ order.lineName }}({{ order.etaText }})</b></div>
          <div class="kv"><span>{{ t('order.createTime') }}</span><b>{{ order.createTime }}</b></div>
          <div v-if="order.payTimeoutAt" class="kv"><span>{{ t('order.timeoutAt') }}</span><b>{{ order.payTimeoutAt }}</b></div>
          <div v-if="order.paidAt" class="kv"><span>{{ t('order.paidAt') }}</span><b>{{ order.paidAt }}</b></div>
          <div v-if="order.completedAt" class="kv"><span>{{ t('order.completedAt') }}</span><b>{{ order.completedAt }}</b></div>
        </section>

        <section class="card" style="margin-top: 14px">
          <h3 style="margin: 0 0 8px">🧺 {{ t('order.items') }}</h3>
          <div v-for="item in order.items" :key="item.skuId" class="item-row">
            <span class="item-name">
              {{ locale === 'en' ? item.titleEn : item.title }}
              <span class="text-sub">{{ formatAttrs(item.attrs, locale) }}</span>
            </span>
            <span class="text-sub">×{{ item.quantity }}</span>
            <span class="price item-sub">{{ money(item.displaySymbol, item.subtotalDisplayPrice) }}</span>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { orderDetail } from '../api/mall'
import { money, STATUS_TAG, TRACK_STATUS, formatAttrs } from '../utils/format'

const route = useRoute()
const { t, locale } = useI18n()

const order = ref(null)
const orderNo = computed(() => route.params.orderNo)

function trackName(status) {
  const name = TRACK_STATUS[status] || {}
  return locale.value === 'en' ? name.en || status : name.zh || status
}

onMounted(async () => {
  order.value = await orderDetail(orderNo.value)
})
</script>

<style scoped>
.head-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

.head-no {
  font-size: 16px;
  font-weight: 700;
}

.head-right {
  text-align: right;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 14px;
  align-items: start;
}

.kv {
  display: flex;
  gap: 12px;
  padding: 5px 0;
  font-size: 13px;
}

.kv span {
  width: 90px;
  color: var(--cm-text-sub);
  flex-shrink: 0;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
}

.item-name {
  flex: 1;
  font-size: 13px;
}

.item-sub {
  font-size: 14px;
}

@media (max-width: 860px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
