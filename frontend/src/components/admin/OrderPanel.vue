<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminOrderPage, adminOrderDetail, adminOrderTransit, adminCloseExpired } from '../../api/admin'
import { money, STATUS_TAG, TRACK_STATUS, formatAttrs } from '../../utils/format'

const { t, locale } = useI18n()

const tabs = [
  { value: '', label: 'all' },
  { value: 'PENDING_PAYMENT', label: null },
  { value: 'PAID', label: null },
  { value: 'STOCKED', label: null },
  { value: 'IN_TRANSIT', label: null },
  { value: 'CUSTOMS_CLEARANCE', label: null },
  { value: 'COMPLETED', label: null }
]

const activeTab = ref('')
const orderNo = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const drawerVisible = ref(false)
const detail = ref(null)

/** 各状态下允许的下一步流转(与后端状态机一致,仅用于展示按钮) */
const NEXT_ACTIONS = {
  PAID: [
    { target: 'STOCKED', zh: '备货完成', en: 'Mark Stocked' },
    { target: 'REFUNDED', zh: '退款', en: 'Refund', danger: true }
  ],
  STOCKED: [{ target: 'DECLARED', zh: '出口报关', en: 'Declare Export' }],
  DECLARED: [{ target: 'IN_TRANSIT', zh: '干线启运', en: 'Depart' }],
  IN_TRANSIT: [{ target: 'CUSTOMS_CLEARANCE', zh: '到达清关', en: 'Arrive Customs' }],
  CUSTOMS_CLEARANCE: [{ target: 'DELIVERING', zh: '清关放行·派送', en: 'Out for Delivery' }],
  DELIVERING: [{ target: 'COMPLETED', zh: '妥投签收', en: 'Mark Delivered' }]
}

async function load() {
  loading.value = true
  try {
    const data = await adminOrderPage({
      status: activeTab.value || undefined,
      orderNo: orderNo.value || undefined,
      page: page.value,
      size: 10
    })
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function tabLabel(tab) {
  if (tab.label) return t('order.' + tab.label)
  return locale.value === 'en'
    ? ({ PENDING_PAYMENT: 'Pending', PAID: 'Paid', STOCKED: 'Stocked', IN_TRANSIT: 'In Transit',
         CUSTOMS_CLEARANCE: 'Customs', COMPLETED: 'Delivered' })[tab.value]
    : ({ PENDING_PAYMENT: '待付款', PAID: '已付款', STOCKED: '已备货', IN_TRANSIT: '运输中',
         CUSTOMS_CLEARANCE: '清关中', COMPLETED: '已妥投' })[tab.value]
}

async function openDetail(order) {
  detail.value = await adminOrderDetail(order.orderNo)
  drawerVisible.value = true
}

async function transit(action) {
  await ElMessageBox.confirm(
    `${detail.value.orderNo} → 「${locale.value === 'en' ? action.en : action.zh}」,确认执行?`,
    t('admin.order.transit'), { type: 'warning' })
  await adminOrderTransit(detail.value.orderNo, { targetStatus: action.target })
  ElMessage.success(`${action.zh} ✓`)
  detail.value = await adminOrderDetail(detail.value.orderNo)
  load()
}

async function closeExpired() {
  const { value } = await ElMessageBox.prompt('扫描上限(单)', t('admin.order.closeExpired'),
    { inputValue: '100', type: 'warning' })
  const n = await adminCloseExpired(Number(value) || 100)
  ElMessage.success(t('admin.order.closeResult', { n }))
  load()
}

function trackName(status) {
  const name = TRACK_STATUS[status] || {}
  return locale.value === 'en' ? name.en || status : name.zh || status
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <span class="page-title">{{ t('admin.order.title') }}</span>
      <div class="toolbar-right">
        <el-input v-model="orderNo" placeholder="订单号" clearable style="width: 220px"
                  @keyup.enter="page = 1; load()" />
        <el-button :icon="'Search'" @click="page = 1; load()" />
        <el-button type="warning" :icon="'Timer'" @click="closeExpired">
          {{ t('admin.order.closeExpired') }}
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="page = 1; load()">
      <el-tab-pane v-for="tab in tabs" :key="tab.value" :name="tab.value" :label="tabLabel(tab)" />
    </el-tabs>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="190" />
      <el-table-column label="收件人" width="130">
        <template #default="{ row }">{{ row.address.receiverName }} ({{ row.address.countryCode }})</template>
      </el-table-column>
      <el-table-column :label="t('order.statusLabel')" width="110">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status]" size="small">
            {{ locale === 'en' ? row.statusLabelEn : row.statusLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="160">
        <template #default="{ row }">
          <div v-for="item in row.items" :key="item.skuId" class="mini-item">
            {{ (locale === 'en' ? item.titleEn : item.title).slice(0, 16) }} ×{{ item.quantity }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="lineName" label="线路" width="110" />
      <el-table-column label="金额" width="110">
        <template #default="{ row }">{{ money(row.displaySymbol, row.totalDisplayPrice) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="160" />
      <el-table-column :label="t('admin.order.actions')" width="130" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openDetail(row)">
            {{ t('admin.order.detail') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination v-model:current-page="page" :total="total" :page-size="10"
                     layout="total, prev, pager, next" background @current-change="load" />
    </div>

    <el-drawer v-model="drawerVisible" size="560px" :title="detail?.orderNo">
      <template v-if="detail">
        <div class="drawer-status">
          <el-tag :type="STATUS_TAG[detail.status]">{{ locale === 'en' ? detail.statusLabelEn : detail.statusLabel }}</el-tag>
          <span class="drawer-amount">{{ money(detail.displaySymbol, detail.totalDisplayPrice) }}</span>
        </div>

        <div class="section-title">{{ t('admin.order.transit') }}</div>
        <div class="action-row">
          <template v-if="NEXT_ACTIONS[detail.status]">
            <el-button v-for="action in NEXT_ACTIONS[detail.status]" :key="action.target"
                       size="small" :type="action.danger ? 'danger' : 'primary'"
                       @click="transit(action)">
              → {{ locale === 'en' ? action.en : action.zh }}
            </el-button>
          </template>
          <el-tag v-else-if="detail.status === 'PENDING_PAYMENT'" type="warning" size="small">
            {{ locale === 'en' ? 'Awaiting buyer payment' : '等待买家支付(超时自动关单)' }}
          </el-tag>
          <el-tag v-else type="info" size="small">
            {{ locale === 'en' ? 'Final state' : '终态' }}
          </el-tag>
        </div>
        <div class="state-note">{{ t('admin.order.illegalTransit') }}</div>

        <div class="section-title">{{ t('admin.order.tracks') }}</div>
        <el-timeline>
          <el-timeline-item v-for="(track, i) in detail.tracks || []" :key="i"
                            :timestamp="`${track.occurredAt} ${track.location || ''}`"
                            :type="i === (detail.tracks?.length || 0) - 1 ? 'primary' : undefined">
            <b>{{ trackName(track.status) }}</b>
            <div class="track-desc">{{ locale === 'en' ? track.descriptionEn : track.description }}</div>
          </el-timeline-item>
        </el-timeline>

        <div class="section-title">{{ t('order.items') }}</div>
        <div v-for="item in detail.items" :key="item.skuId" class="drawer-item">
          <span>{{ locale === 'en' ? item.titleEn : item.title }} {{ formatAttrs(item.attrs, locale) }}</span>
          <b>×{{ item.quantity }}</b>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.page-title { font-weight: 700; font-size: 16px; }
.toolbar-right { display: flex; gap: 8px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.mini-item { font-size: 12px; color: #606266; line-height: 1.5; }
.drawer-status { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.drawer-amount { font-size: 18px; font-weight: 700; color: #e02e24; }
.section-title { font-weight: 600; margin: 18px 0 10px; padding-top: 12px; border-top: 1px solid #f0f0f0; }
.action-row { display: flex; gap: 8px; flex-wrap: wrap; }
.state-note { color: #c0c4cc; font-size: 12px; margin-top: 8px; }
.track-desc { color: #606266; font-size: 12px; margin-top: 2px; }
.drawer-item { display: flex; justify-content: space-between; font-size: 13px; padding: 6px 0; }
</style>
