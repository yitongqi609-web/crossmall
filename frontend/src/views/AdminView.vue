<template>
  <section class="card">
    <el-tabs v-model="tab">
      <!-- ============ 数据看板 ============ -->
      <el-tab-pane :label="`📊 ${t('admin.dashboard')}`" name="dashboard" />
      <!-- ============ 商品 ============ -->
      <el-tab-pane :label="`📦 ${t('admin.goods.title')}`" name="goods" />
      <el-tab-pane :label="`🧭 ${t('admin.category')}`" name="category" />
      <el-tab-pane :label="`💱 ${t('admin.fx.title')}`" name="fx" />
      <el-tab-pane :label="`🛃 ${t('admin.tax.title')}`" name="tax" />
      <el-tab-pane :label="`🚚 ${t('admin.logistics')}`" name="logistics" />
      <el-tab-pane :label="`🧾 ${t('admin.order.title')}`" name="order" />

      <DashboardPanel v-if="tab === 'dashboard'" />
      <GoodsPanel v-else-if="tab === 'goods'" />
      <CategoryPanel v-else-if="tab === 'category'" />
      <FxPanel v-else-if="tab === 'fx'" />
      <TaxPanel v-else-if="tab === 'tax'" />
      <LogisticsPanel v-else-if="tab === 'logistics'" />
      <OrderPanel v-else-if="tab === 'order'" />
    </el-tabs>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DashboardPanel from '../components/admin/DashboardPanel.vue'
import GoodsPanel from '../components/admin/GoodsPanel.vue'
import CategoryPanel from '../components/admin/CategoryPanel.vue'
import FxPanel from '../components/admin/FxPanel.vue'
import TaxPanel from '../components/admin/TaxPanel.vue'
import LogisticsPanel from '../components/admin/LogisticsPanel.vue'
import OrderPanel from '../components/admin/OrderPanel.vue'

const { t } = useI18n()
const tab = ref('dashboard')
</script>
