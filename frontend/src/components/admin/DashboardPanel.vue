<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import { dashboard } from '../../api/admin'
import { usd } from '../../utils/format'

const { t, locale } = useI18n()

const stats = ref({})
const trendEl = ref(null)
const distEl = ref(null)
const topEl = ref(null)
let charts = []

function render() {
  // 近 7 日趋势
  const trend = stats.value.trend || []
  const trendChart = echarts.init(trendEl.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: [t('admin.overview.orders'), 'GMV($)'] },
    grid: { left: 48, right: 48, bottom: 28, top: 40 },
    xAxis: { type: 'category', data: trend.map(d => d.date) },
    yAxis: [
      { type: 'value', name: t('admin.overview.orders') },
      { type: 'value', name: 'GMV($)', axisLabel: { formatter: v => (v / 100).toFixed(0) } }
    ],
    series: [
      { name: t('admin.overview.orders'), type: 'bar', data: trend.map(d => d.orders), barMaxWidth: 26, itemStyle: { color: '#1f6feb' } },
      { name: 'GMV($)', type: 'line', yAxisIndex: 1, smooth: true, data: trend.map(d => d.gmvUsdCents / 100), itemStyle: { color: '#e02e24' } }
    ]
  })
  charts.push(trendChart)

  // 状态分布
  const dist = stats.value.statusDist || []
  const distChart = echarts.init(distEl.value)
  distChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 0, top: 'middle', textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['36%', '50%'],
      label: { show: false },
      data: dist.map(d => ({
        name: locale.value === 'en' ? d.labelEn : d.label,
        value: d.count
      }))
    }]
  })
  charts.push(distChart)

  // 热销 Top5
  const top = stats.value.topGoods || []
  const topChart = echarts.init(topEl.value)
  topChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 150, right: 40, bottom: 28, top: 16 },
    xAxis: { type: 'value' },
    yAxis: {
      type: 'category',
      data: top.map(g => (locale.value === 'en' ? g.titleEn : g.title)).reverse(),
      axisLabel: { width: 140, overflow: 'truncate', fontSize: 11 }
    },
    series: [{
      type: 'bar',
      data: top.map(g => g.quantity).reverse(),
      barMaxWidth: 18,
      itemStyle: { color: '#67c23a' }
    }]
  })
  charts.push(topChart)
}

onMounted(async () => {
  stats.value = await dashboard()
  render()
  window.addEventListener('resize', onResize)
})

function onResize() {
  charts.forEach(c => c.resize())
}

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach(c => c.dispose())
})
</script>

<template>
  <div>
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">{{ t('admin.overview.gmv') }}</div>
        <div class="stat-value">{{ usd(stats.gmvUsdCents || 0) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ t('admin.overview.paidOrders') }}</div>
        <div class="stat-value">{{ stats.paidOrders ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ t('admin.overview.pendingPayment') }}</div>
        <div class="stat-value warn">{{ stats.pendingPayment ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ t('admin.overview.inFulfillment') }}</div>
        <div class="stat-value">{{ stats.inFulfillment ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ t('admin.overview.completed') }}</div>
        <div class="stat-value ok">{{ stats.completed ?? 0 }}</div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-card wide">
        <div class="chart-title">📈 {{ t('admin.overview.trend') }}</div>
        <div ref="trendEl" class="chart-box" />
      </div>
      <div class="chart-card">
        <div class="chart-title">🍩 {{ t('admin.overview.statusDist') }}</div>
        <div ref="distEl" class="chart-box" />
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-card wide">
        <div class="chart-title">🏆 {{ t('admin.overview.topGoods') }}</div>
        <div ref="topEl" class="chart-box" style="height: 260px" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; margin-bottom: 14px; }
.stat-card { background: #fff; border-radius: 10px; padding: 18px; border: 1px solid #f0f0f0; }
.stat-label { color: #909399; font-size: 12px; }
.stat-value { font-size: 26px; font-weight: 800; margin-top: 8px; }
.stat-value.warn { color: #e6a23c; }
.stat-value.ok { color: #67c23a; }
.chart-row { display: grid; grid-template-columns: 2fr 1fr; gap: 14px; margin-bottom: 14px; }
.chart-card { background: #fff; border-radius: 10px; padding: 16px; border: 1px solid #f0f0f0; }
.chart-title { font-weight: 600; margin-bottom: 8px; }
.chart-box { height: 300px; }
</style>
