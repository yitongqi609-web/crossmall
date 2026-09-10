<template>
  <section v-if="items.length" class="card">
    <div class="head-tip">
      💡 {{ locale === 'en'
        ? 'Prices & stock are resolved live from the server at every view'
        : '购物车仅存「SKU + 数量」,每次读取实时解析最新价格与库存,天然规避快照过期' }}
    </div>

    <div v-for="item in items" :key="item.skuId" class="cart-row" :class="{ invalid: !item.valid }">
      <el-checkbox v-if="item.valid" :model-value="selected.includes(item.skuId)"
                   @change="v => v ? selected.push(item.skuId)
                     : (selected = selected.filter(id => id !== item.skuId))" />
      <div class="row-img" @click="$router.push(`/goods/${item.spuId}`)">
        <img v-if="item.image" :src="item.image" alt="" />
        <span v-else>🛍️</span>
      </div>
      <div class="row-info">
        <div class="row-title">{{ locale === 'en' ? item.titleEn : item.title }}</div>
        <div class="text-sub">{{ formatAttrs(item.attrs, locale) }}</div>
      </div>
      <div class="price row-price">{{ money(app.symbol, item.displayPrice) }}</div>
      <el-input-number v-if="item.valid" v-model="item.quantity" :min="1" size="small"
                       @change="onQtyChange(item)" />
      <el-tag v-else type="info" size="small" effect="plain">{{ t('cart.invalid') }}</el-tag>
    </div>

    <div class="cart-foot">
      <el-button text type="danger" @click="removeItems">{{ t('address.delete') }}</el-button>
      <div class="foot-right">
        <span class="text-sub">{{ selectedItems.length }} {{ locale === 'en' ? 'items' : '件' }}</span>
        <span class="price" style="font-size: 20px">{{ money(app.symbol, totalCents) }}</span>
        <el-button type="primary" size="large" round class="cm-btn-primary" @click="goCheckout">
          {{ t('cart.settle') }}
        </el-button>
      </div>
    </div>
  </section>
  <el-empty v-else :description="t('cart.empty')">
    <el-button type="primary" round class="cm-btn-primary" @click="$router.push('/goods')">
      {{ t('cart.goHome') }}
    </el-button>
  </el-empty>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartList, cartUpdateQty, cartRemove } from '../api/mall'
import { useAppStore } from '../store/app'
import { useCartStore } from '../store/cart'
import { money, formatAttrs } from '../utils/format'

const router = useRouter()
const { t, locale } = useI18n()
const app = useAppStore()
const cart = useCartStore()

const items = ref([])
const selected = ref([])

const validItems = computed(() => items.value.filter(i => i.valid))
const selectedItems = computed(() => validItems.value.filter(i => selected.value.includes(i.skuId)))
const totalCents = computed(() =>
  selectedItems.value.reduce((sum, i) => sum + i.displayPrice * i.quantity, 0))

async function load() {
  items.value = await cartList(app.currency)
  selected.value = items.value.filter(i => i.valid).map(i => i.skuId)
  cart.set(items.value.length)
}

async function onQtyChange(item) {
  if (item.quantity <= 0) return
  if (item.quantity > item.stock) {
    item.quantity = item.stock
    ElMessage.warning(`${t('goods.stock')}: ${item.stock}`)
    return
  }
  await cartUpdateQty(item.skuId, item.quantity)
}

async function removeItems() {
  if (selected.value.length === 0) return
  await ElMessageBox.confirm(t('cart.removed') + '?', { type: 'warning' })
  await cartRemove(selected.value)
  load()
}

function goCheckout() {
  if (selectedItems.value.length === 0) {
    ElMessage.warning(t('cart.empty'))
    return
  }
  router.push({ name: 'checkout', query: { skuIds: selected.value.join(',') } })
}

onMounted(load)
</script>

<style scoped>
.cart-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.cart-row.invalid {
  opacity: 0.5;
}

.row-img {
  width: 68px;
  height: 68px;
  border-radius: 10px;
  cursor: pointer;
  background: #f3f8ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  overflow: hidden;
  flex-shrink: 0;
}

.row-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.row-info {
  flex: 1;
  min-width: 0;
}

.row-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}

.row-price {
  width: 100px;
  font-size: 15px;
}

.cart-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
}

.foot-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
</style>
