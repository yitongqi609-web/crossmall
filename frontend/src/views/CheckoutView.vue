<template>
  <div v-if="preview">
    <section class="card">
      <h3 style="margin: 0 0 14px">🧾 {{ t('checkout.title') }}</h3>

      <!-- 地址 -->
      <div class="block">
        <div class="block-label">📍 {{ t('checkout.address') }}</div>
        <el-alert v-if="addresses.length === 0" type="warning" :closable="false"
                  :title="t('address.empty')">
          <el-button size="small" type="primary" round @click="$router.push({ name: 'profile' })">
            {{ t('checkout.addAddress') }}
          </el-button>
        </el-alert>
        <div v-else class="addr-grid">
          <div v-for="a in addresses" :key="a.id" class="addr-card"
               :class="{ active: a.id === addressId }" @click="addressId = a.id; doPreview()">
            <div class="addr-top">
              <b>{{ a.receiverName }}</b>
              <el-tag v-if="a.isDefault === 1" size="small" effect="plain">{{ t('address.isDefault') }}</el-tag>
            </div>
            <div class="text-sub" style="margin-top: 4px">
              {{ a.phone }} · {{ a.countryName }} · {{ a.stateProvince || '' }}{{ a.city }} · {{ a.street }} · {{ a.postcode }}
            </div>
          </div>
        </div>
      </div>

      <!-- 物流线路 -->
      <div v-if="preview" class="block">
        <div class="block-label">
          🚚 {{ t('checkout.line') }}
          <span class="text-sub" style="margin-left: 8px">
            {{ t('checkout.weight') }} {{ preview.totalWeightGrams }}g({{ t('checkout.firstWeight') }} {{ preview.firstWeightGrams }}g)
          </span>
        </div>
        <div class="line-grid">
          <div v-for="line in preview.lineOptions" :key="line.lineId" class="line-card"
               :class="{ active: line.lineId === lineId }" @click="pickLine(line.lineId)">
            <div class="line-top">
              <b>{{ locale === 'en' ? line.nameEn : line.name }}</b>
              <span class="price">{{ money(line.displaySymbol, line.shippingDisplayPrice) }}</span>
            </div>
            <div class="text-sub" style="margin-top: 4px">
              {{ line.carrier }} · {{ t('checkout.eta') }} {{ line.etaMinDays }}-{{ line.etaMaxDays }}d
            </div>
          </div>
        </div>
      </div>

      <!-- 商品清单 -->
      <div class="block">
        <div class="block-label">📦 {{ t('order.items') }}</div>
        <div v-for="item in preview.items" :key="item.skuId" class="item-row">
          <span class="item-name">
            {{ locale === 'en' ? item.titleEn : item.title }}
            <span class="text-sub">{{ formatAttrs(item.attrs, locale) }}</span>
          </span>
          <span class="text-sub">x{{ item.quantity }}</span>
          <span class="price item-sub">{{ money(item.displaySymbol, item.subtotalDisplayPrice) }}</span>
        </div>
      </div>

      <!-- 币种 + 明细 -->
      <div class="block">
        <div class="block-label">💱 {{ t('checkout.currency') }}</div>
        <el-radio-group v-model="currency" @change="doPreview">
          <el-radio v-for="c in CURRENCIES" :key="c" :value="c" border>{{ c }}</el-radio>
        </el-radio-group>
        <div class="text-sub" style="margin-top: 8px">
          {{ t('order.fxSnapshot') }}: 1 USD = {{ preview.fxRate }} {{ currency }}
          ({{ locale === 'en' ? 'locked into the order on submit' : '提交订单时锁定该汇率快照' }})
        </div>
      </div>

      <!-- 费用汇总 -->
      <div class="summary">
        <div class="sum-row"><span>{{ t('checkout.goodsFee') }}</span>
          <span>{{ money(preview.displaySymbol, preview.goodsFeeCents) }}</span></div>
        <div class="sum-row"><span>{{ t('checkout.shippingFee') }}</span>
          <span>{{ money(preview.displaySymbol, preview.shippingFeeCents) }}</span></div>
        <div class="sum-row">
          <span>
            {{ t('checkout.taxFee') }}
            <el-tooltip placement="top">
              <template #content>
                <div v-for="(h, i) in preview.taxHits" :key="i">
                  {{ h.hsCode }} {{ h.hsName }} × {{ (h.rate * 100).toFixed(1) }}%
                </div>
                <div v-if="preview.deMinimisCents > 0">
                  {{ t('checkout.deMinimis') }}: ${{ (preview.deMinimisCents / 100).toFixed(0) }}
                </div>
              </template>
              <span style="color: var(--cm-primary-deep); cursor: help">[{{ t('checkout.taxDetail') }}]</span>
            </el-tooltip>
          </span>
          <span>{{ money(preview.displaySymbol, preview.taxFeeCents) }}</span>
        </div>
        <div class="sum-row total">
          <span>{{ t('checkout.total') }}</span>
          <span class="price" style="font-size: 22px">{{ money(preview.displaySymbol, preview.totalLocalCents) }}</span>
        </div>
      </div>

      <div style="text-align: right; margin-top: 14px">
        <el-button size="large" round @click="$router.back()">{{ locale === 'en' ? 'Back' : '再逛逛' }}</el-button>
        <el-button type="primary" size="large" round class="cm-btn-primary" :loading="submitting"
                   :disabled="!addressId" @click="submitOrder">
          {{ t('checkout.submit') }}
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { cartList, orderPreview, orderCreate, addressList, fxRates } from '../api/mall'
import { useAppStore } from '../store/app'
import { money, formatAttrs, CURRENCIES } from '../utils/format'

const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n()
const app = useAppStore()

const addresses = ref([])
const addressId = ref(null)
const lineId = ref(null)
const preview = ref(null)
const currency = ref(app.currency)
const items = ref([])
const submitting = ref(false)

async function resolveItems() {
  if (route.query.skuIds) {
    const list = await cartList(currency.value)
    const ids = String(route.query.skuIds).split(',').map(Number)
    items.value = list.filter(i => ids.includes(i.skuId) && i.valid)
      .map(i => ({ skuId: i.skuId, quantity: i.quantity }))
  } else if (route.query.skuId) {
    items.value = [{ skuId: Number(route.query.skuId), quantity: Number(route.query.qty || 1) }]
  }
}

async function doPreview() {
  if (!addressId.value || items.value.length === 0) return
  preview.value = await orderPreview({
    addressId: addressId.value,
    lineId: lineId.value,
    currency: currency.value,
    items: items.value
  })
  if (!lineId.value && preview.value.lineId) {
    lineId.value = preview.value.lineId
  }
}

function pickLine(id) {
  lineId.value = id
  doPreview()
}

async function submitOrder() {
  submitting.value = true
  try {
    const orderNo = await orderCreate({
      addressId: addressId.value,
      lineId: lineId.value,
      currency: currency.value,
      items: items.value
    })
    ElMessage.success(`${t('checkout.submit')} ✓ ${orderNo}`)
    router.push({ name: 'pay', params: { orderNo } })
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const [rates] = await Promise.all([fxRates(), addressList()])
  const match = rates.find(r => r.currency === currency.value)
  if (match) app.setCurrency({ currency: match.currency, symbol: match.symbol })
  addresses.value = await addressList()
  const def = addresses.value.find(a => a.isDefault === 1)
  addressId.value = def?.id ?? addresses.value[0]?.id ?? null
  await resolveItems()
  if (items.value.length === 0) {
    ElMessage.warning(t('cart.empty'))
    router.replace({ name: 'cart' })
    return
  }
  await doPreview()
})
</script>

<style scoped>
.block {
  margin-bottom: 20px;
}

.block-label {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 14px;
}

.addr-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.addr-card {
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.addr-card.active {
  border-color: var(--cm-primary);
  background: var(--cm-primary-light);
}

.addr-top {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.line-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.line-card {
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.line-card.active {
  border-color: var(--cm-primary);
  background: var(--cm-primary-light);
}

.line-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
}

.item-name {
  flex: 1;
  font-size: 14px;
}

.item-sub {
  font-size: 14px;
}

.summary {
  border-top: 1px dashed #eee;
  padding-top: 12px;
  margin-left: auto;
  width: 300px;
}

.sum-row {
  display: flex;
  justify-content: space-between;
  color: #595959;
  font-size: 13px;
  padding: 3px 0;
}

.sum-row.total {
  font-weight: 700;
  color: #262626;
  font-size: 14px;
}

@media (max-width: 860px) {
  .addr-grid,
  .line-grid {
    grid-template-columns: 1fr;
  }
}
</style>
