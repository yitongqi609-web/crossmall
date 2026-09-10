<template>
  <section class="card">
    <div class="toolbar">
      <el-radio-group v-model="categoryId" @change="page = 1; load()">
        <el-radio-button :value="undefined">{{ t('nav.allGoods') }}</el-radio-button>
        <el-radio-button v-for="c in categories" :key="c.id" :value="c.id">
          {{ c.icon }} {{ locale === 'en' ? c.nameEn : c.name }}
        </el-radio-button>
      </el-radio-group>
      <el-input v-model="keyword" :placeholder="t('nav.searchPlaceholder')" clearable style="width: 240px"
                :prefix-icon="Search" @keyup.enter="doSearch" @clear="doSearch" />
    </div>

    <div v-loading="loading" class="goods-grid">
      <div v-for="g in list" :key="g.spuId" class="goods-card" @click="$router.push(`/goods/${g.spuId}`)">
        <div class="goods-img">
          <img v-if="g.image" :src="g.image" alt="" />
          <span v-else class="goods-placeholder">{{ categoryIcon(g.categoryId) }}</span>
        </div>
        <div class="goods-info">
          <div class="goods-title">{{ locale === 'en' ? g.titleEn : g.title }}</div>
          <div class="goods-brand text-sub">{{ g.brand }}</div>
          <div class="goods-bottom">
            <span class="price" style="font-size: 16px">{{ money(app.symbol, g.displayPrice) }}</span>
            <span class="text-sub">{{ t('goods.sales') }} {{ g.sales }}</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" :description="t('goods.noGoods')" class="grid-empty" />
    </div>

    <div style="display: flex; justify-content: flex-end; margin-top: 14px">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize"
                     :current-page="page" @current-change="onPage" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import { goodsPage, home } from '../api/mall'
import { useAppStore } from '../store/app'
import { categoryIcon, money } from '../utils/format'

const route = useRoute()
const { t, locale } = useI18n()
const app = useAppStore()

const list = ref([])
const categories = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 12
const categoryId = ref(route.query.categoryId ? Number(route.query.categoryId) : undefined)
const keyword = ref(route.query.keyword || '')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await goodsPage({
      categoryId: categoryId.value,
      keyword: keyword.value || undefined,
      page: page.value,
      size: pageSize,
      currency: app.currency
    })
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function doSearch() {
  page.value = 1
  load()
}

function onPage(p) {
  page.value = p
  load()
}

onMounted(async () => {
  load()
  const data = await home(app.currency)
  categories.value = data.categories
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  min-height: 220px;
}

.grid-empty {
  grid-column: 1 / -1;
}

.goods-card {
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  background: #fff;
}

.goods-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(31, 111, 235, 0.12);
}

.goods-img {
  height: 150px;
  background: linear-gradient(135deg, #f3f8ff, #fafcff);
  display: flex;
  align-items: center;
  justify-content: center;
}

.goods-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-placeholder {
  font-size: 44px;
}

.goods-info {
  padding: 10px 12px 12px;
}

.goods-title {
  font-size: 13px;
  line-height: 1.45;
  height: 38px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.goods-brand {
  margin-top: 2px;
}

.goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 6px;
}

@media (max-width: 900px) {
  .goods-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
