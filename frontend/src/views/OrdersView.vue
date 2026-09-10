<template>
  <section class="card">
    <div class="head-tip">
      💡 {{ locale === 'en'
        ? 'Unpaid orders are auto-closed by a RocketMQ delayed message when the countdown ends'
        : '待支付订单超时将被 RocketMQ 延迟消息自动关单并回补库存' }}
    </div>
    <el-tabs v-model="activeTab" @tab-change="page = 1; load()">
      <el-tab-pane :label="t('order.all')" name="" />
      <el-tab-pane :label="locale === 'en' ? 'Pending Payment' : '待付款'" name="PENDING_PAYMENT" />
      <el-tab-pane :label="locale === 'en' ? 'Paid' : '已付款'" name="PAID" />
      <el-tab-pane :label="locale === 'en' ? 'In Transit' : '运输中'" name="IN_TRANSIT" />
      <el-tab-pane :label="locale === 'en' ? 'Delivered' : '已妥投'" name="COMPLETED" />
    </el-tabs>

    <div v-loading="loading" class="order-list">
      <div v-for="o in list" :key="o.orderNo" class="order-card">
        <div class="oc-head">
          <span class="oc-no" @click="$router.push(`/orders/${o.orderNo}`)">{{ o.orderNo }}</span>
          <div style="display: flex; align-items: center; gap: 10px">
            <span class="text-sub">{{ o.createTime }}</span>
            <el-tag :type="STATUS_TAG[o.status]" effect="plain" size="small">
              {{ locale === 'en' ? o.statusLabelEn : o.statusLabel }}
            </el-tag>
          </div>
        </div>
        <div class="oc-items text-sub">
          <span v-for="(it, idx) in o.items.slice(0, 3)" :key="it.skuId">
            {{ locale === 'en' ? it.titleEn : it.title }} ×{{ it.quantity }}<template v-if="idx < Math.min(o.items.length, 3) - 1">、</template>
          </span>
          <span v-if="o.items.length > 3"> {{ locale === 'en' ? `etc. ${o.items.length} items` : `等 ${o.items.length} 件` }}</span>
        </div>
        <div class="oc-foot">
          <span class="text-sub">{{ o.lineName }} · {{ t('checkout.taxFee') }} ${{ (o.taxFeeCents / 100).toFixed(2) }}</span>
          <span class="price" style="font-size: 16px">{{ money(o.displaySymbol, o.totalDisplayPrice) }}</span>
        </div>
        <div class="oc-actions">
          <el-button size="small" round @click="$router.push(`/orders/${o.orderNo}`)">{{ t('order.view') }}</el-button>
          <el-button v-if="o.status === 'PENDING_PAYMENT'" size="small" round @click="cancel(o)">
            {{ t('order.cancel') }}
          </el-button>
          <el-button v-if="o.status === 'PENDING_PAYMENT'" size="small" round class="cm-btn-primary"
                     @click="$router.push(`/pay/${o.orderNo}`)">
            {{ t('order.goPay') }}
          </el-button>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" :description="t('order.empty')" />
    </div>

    <div style="display: flex; justify-content: flex-end; margin-top: 14px">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize"
                     :current-page="page" @current-change="onPage" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderPage, orderCancel } from '../api/mall'
import { money, STATUS_TAG } from '../utils/format'

const { t, locale } = useI18n()

const activeTab = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 5
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await orderPage({
      status: activeTab.value || undefined,
      page: page.value,
      size: pageSize
    })
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function onPage(p) {
  page.value = p
  load()
}

async function cancel(o) {
  const { value } = await ElMessageBox.prompt(t('order.cancelReason'), t('order.cancelConfirm'), {
    inputPlaceholder: 'changed my mind',
    type: 'warning'
  })
  await orderCancel(o.orderNo, value || '用户主动取消')
  ElMessage.success(t('order.cancel') + ' ✓')
  load()
}

onMounted(load)
</script>

<style scoped>
.order-list {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 14px 16px;
}

.oc-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.oc-no {
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  color: var(--cm-primary-deep);
}

.oc-items {
  margin: 8px 0;
}

.oc-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.oc-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
