<template>
  <div>
    <!-- Banner -->
    <section class="hero card">
      <div class="hero-left">
        <h1>{{ locale === 'en' ? 'Global Goods · Delivered to Your Door' : '全球好物直邮 · 关税透明试算' }}</h1>
        <p class="text-sub">
          {{ locale === 'en'
            ? 'Multi-currency settlement, duty estimate, fulfilment state machine — cross-border e-commerce demo'
            : '6 币种实时结算 · HS 关税试算 · 履约状态机全程追踪,跨境电商演示项目' }}
        </p>
        <div class="hero-btns">
          <el-button type="primary" size="large" class="cm-btn-primary" round @click="$router.push('/goods')">
            {{ locale === 'en' ? 'Start Shopping →' : '去逛好物 →' }}
          </el-button>
          <el-button size="large" round @click="$router.push('/cart')">{{ locale === 'en' ? 'My Cart' : '看看购物车' }}</el-button>
        </div>
        <div class="hero-tip">
          💱 当前结算币种 <b class="hl">{{ app.currency }}</b> {{ app.symbol }}
          · 下单将按实时汇率锁定快照
        </div>
      </div>
      <div class="hero-emoji">🛍️🚢✈️📦🌏</div>
    </section>

    <!-- 分类入口 -->
    <section class="card cat-section">
      <div class="section-head">
        <h2>🧭 {{ locale === 'en' ? 'Shop by Category' : '按分类逛' }}</h2>
        <router-link to="/goods" class="more-link">{{ locale === 'en' ? 'All products →' : '全部商品 →' }}</router-link>
      </div>
      <div class="cat-grid">
        <div v-for="c in categories" :key="c.id" class="cat-tile"
             @click="$router.push({ path: '/goods', query: { categoryId: c.id } })">
          <span class="cat-emoji">{{ c.icon }}</span>
          <span class="cat-name">{{ locale === 'en' ? c.nameEn : c.name }}</span>
        </div>
      </div>
    </section>

    <!-- 热销 -->
    <section class="card hot-section">
      <div class="section-head">
        <h2>🔥 {{ locale === 'en' ? 'Hot Sales' : '热销好物' }}</h2>
        <router-link to="/goods" class="more-link">{{ locale === 'en' ? 'More →' : '更多 →' }}</router-link>
      </div>
      <div class="goods-grid">
        <div v-for="g in hotGoods" :key="g.spuId" class="goods-card" @click="$router.push(`/goods/${g.spuId}`)">
          <div class="goods-img">
            <img v-if="g.image" :src="g.image" alt="" />
            <span v-else class="goods-placeholder">{{ categoryIcon(g.categoryId) }}</span>
          </div>
          <div class="goods-info">
            <div class="goods-title">{{ locale === 'en' ? g.titleEn : g.title }}</div>
            <div class="goods-brand text-sub">{{ g.brand }}</div>
            <div class="goods-bottom">
              <span class="price">{{ money(app.symbol, g.displayPrice) }}</span>
              <span class="text-sub">{{ t('goods.sales') }} {{ g.sales }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { home } from '../api/mall'
import { useAppStore } from '../store/app'
import { categoryIcon, money } from '../utils/format'

const { t, locale } = useI18n()
const app = useAppStore()

const categories = ref([])
const hotGoods = ref([])

onMounted(async () => {
  const data = await home(app.currency)
  categories.value = data.categories
  hotGoods.value = data.hotGoods
})
</script>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(120deg, #eef5ff, #dcecfe);
  border: 1px solid #cfe2ff;
}

.hero-left h1 {
  margin: 0 0 6px;
  font-size: 26px;
}

.hero-btns {
  margin-top: 16px;
  display: flex;
  gap: 10px;
}

.hero-tip {
  margin-top: 18px;
  font-size: 13px;
  background: #fff;
  display: inline-block;
  padding: 8px 14px;
  border-radius: 20px;
  border: 1px dashed var(--cm-primary);
  color: #595959;
}

.hl {
  color: var(--cm-primary-deep);
}

.hero-emoji {
  font-size: 44px;
  letter-spacing: 8px;
  filter: drop-shadow(0 6px 10px rgba(31, 111, 235, 0.22));
}

.cat-section {
  margin-top: 16px;
}

.cat-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
}

.cat-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 0;
  border-radius: 12px;
  background: #f3f8ff;
  border: 1px solid #dcecfe;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.cat-tile:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(31, 111, 235, 0.14);
}

.cat-emoji {
  font-size: 38px;
}

.cat-name {
  font-size: 14px;
  font-weight: 600;
  color: #595959;
}

.hot-section {
  margin-top: 16px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.goods-card {
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
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
  .cat-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .goods-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .hero-emoji {
    display: none;
  }
}
</style>
