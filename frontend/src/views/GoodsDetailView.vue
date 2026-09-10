<template>
  <div v-if="goods">
    <section class="card detail-card">
      <div class="detail-img">
        <img v-if="goods.image" :src="goods.image" alt="" />
        <span v-else class="detail-placeholder">{{ categoryIcon(goods.categoryId) }}</span>
      </div>
      <div class="detail-info">
        <h2 class="detail-title">{{ locale === 'en' ? goods.titleEn : goods.title }}</h2>
        <div class="text-sub">{{ goods.brand }} · {{ t('goods.sales') }} {{ goods.sales }}</div>
        <div class="price detail-price">
          <span class="price-symbol">{{ app.symbol }}</span>{{ ((selectedSku?.displayPrice ?? 0) / 100).toFixed(2) }}
          <span class="text-sub" style="font-weight: 400">(≈ ${{ ((selectedSku?.priceCents ?? 0) / 100).toFixed(2) }})</span>
        </div>
        <div class="detail-desc">{{ locale === 'en' ? goods.descriptionEn : goods.description }}</div>

        <div class="block">
          <div class="block-label">{{ t('goods.spec') }}</div>
          <div class="spec-list">
            <div v-for="sku in goods.skus" :key="sku.skuId" class="spec-item"
                 :class="{ active: sku.skuId === selectedSkuId, disabled: sku.stock <= 0 }"
                 @click="sku.stock > 0 && (selectedSkuId = sku.skuId)">
              {{ skuLabel(sku) }}
            </div>
          </div>
        </div>

        <div class="block">
          <div class="block-label">{{ t('goods.quantity') }}</div>
          <div class="qty-row">
            <el-input-number v-model="quantity" :min="1" :max="selectedSku?.stock || 1" />
            <span class="text-sub">{{ t('goods.stock') }}: {{ selectedSku?.stock }}</span>
          </div>
        </div>

        <div class="detail-actions">
          <el-button size="large" round :disabled="!selectedSku || selectedSku.stock <= 0"
                     :loading="adding" @click="addToCart">
            🛒 {{ t('goods.addToCart') }}
          </el-button>
          <el-button size="large" round type="primary" class="cm-btn-primary"
                     :disabled="!selectedSku || selectedSku.stock <= 0" @click="buyNow">
            ⚡ {{ t('goods.buyNow') }}
          </el-button>
        </div>

        <div class="head-tip" style="margin-top: 14px; margin-bottom: 0">
          🛃 {{ t('goods.taxHint') }}<template v-if="goods.hsCode"> · {{ t('goods.hsCode') }}: {{ goods.hsCode }}</template>
        </div>
      </div>
    </section>

    <section class="card" style="margin-top: 16px">
      <div class="section-head">
        <h2>📋 {{ locale === 'en' ? 'Product Details' : '商品详情' }}</h2>
      </div>
      <p class="extra-text">{{ locale === 'en' ? goods.descriptionEn : goods.description }}</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { goodsDetail, cartAdd } from '../api/mall'
import { useAppStore } from '../store/app'
import { useCartStore } from '../store/cart'
import { categoryIcon } from '../utils/format'

const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n()
const app = useAppStore()
const cart = useCartStore()

const goods = ref(null)
const selectedSkuId = ref(null)
const quantity = ref(1)
const adding = ref(false)

const selectedSku = computed(() =>
  goods.value?.skus.find(s => s.skuId === selectedSkuId.value))

function specGroups(sku) {
  try {
    const attrs = JSON.parse(sku.attrs)
    const useEn = locale.value === 'en'
    return attrs.map(a => ({ label: useEn ? a.kEn || a.k : a.k, value: useEn ? a.vEn || a.v : a.v }))
  } catch (e) {
    return []
  }
}

function skuLabel(sku) {
  return specGroups(sku).map(g => g.value).join(' / ')
}

async function addToCart() {
  if (!selectedSku.value) return
  adding.value = true
  try {
    await cartAdd({ skuId: selectedSku.value.skuId, quantity: quantity.value })
    ElMessage.success(`${t('goods.addToCart')} ✓`)
    cart.refresh()
  } finally {
    adding.value = false
  }
}

function buyNow() {
  if (!selectedSku.value) return
  router.push({ name: 'checkout', query: { skuId: selectedSku.value.skuId, qty: quantity.value } })
}

onMounted(async () => {
  goods.value = await goodsDetail(route.params.spuId, app.currency)
  if (goods.value.skus.length > 0) {
    selectedSkuId.value = goods.value.skus[0].skuId
  }
})
</script>

<style scoped>
.detail-card {
  display: flex;
  gap: 26px;
}

.detail-img {
  width: 340px;
  height: 340px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #f3f8ff, #fafcff);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.detail-placeholder {
  font-size: 100px;
}

.detail-info {
  flex: 1;
  min-width: 0;
}

.detail-title {
  margin: 0 0 4px;
  font-size: 22px;
}

.detail-price {
  font-size: 28px;
  margin: 12px 0;
}

.detail-desc {
  color: #595959;
  margin: 0 0 14px;
  line-height: 1.6;
  font-size: 14px;
}

.block {
  margin-bottom: 16px;
}

.block-label {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 14px;
}

.spec-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.spec-item {
  border: 1px solid #dcdfe6;
  border-radius: 18px;
  padding: 6px 16px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.15s;
}

.spec-item.active {
  border-color: var(--cm-primary);
  color: var(--cm-primary-deep);
  background: var(--cm-primary-light);
  font-weight: 600;
}

.spec-item.disabled {
  color: #c0c4cc;
  cursor: not-allowed;
  text-decoration: line-through;
}

.qty-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-actions {
  margin: 20px 0 0;
  display: flex;
  gap: 10px;
}

.extra-text {
  color: #595959;
  line-height: 1.8;
  margin: 0;
}
</style>
