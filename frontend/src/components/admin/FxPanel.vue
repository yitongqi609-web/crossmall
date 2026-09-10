<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { fxAdminList, fxAdminUpdate, fxPull } from '../../api/admin'

const { t } = useI18n()
const list = ref([])
const dialogVisible = ref(false)
const pulling = ref(false)
const form = reactive({ currency: '', symbol: '', rate: 1 })

async function load() {
  list.value = await fxAdminList()
}

function openEdit(row) {
  Object.assign(form, { currency: row.currency, symbol: row.symbol, rate: Number(row.rate) })
  dialogVisible.value = true
}

async function save() {
  if (!form.rate || form.rate <= 0) {
    ElMessage.warning('rate > 0')
    return
  }
  await fxAdminUpdate(form.currency, form.rate)
  ElMessage.success(`${form.currency} = ${form.rate}`)
  dialogVisible.value = false
  load()
}

async function pull() {
  pulling.value = true
  try {
    const n = await fxPull()
    ElMessage.success(`${t('admin.fx.pull')}: ${n}`)
    load()
  } finally {
    pulling.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <span class="page-title">{{ t('admin.fx') }}</span>
      <el-button type="primary" :loading="pulling" :icon="'Refresh'" @click="pull">
        {{ t('admin.fx.pull') }}
      </el-button>
    </div>
    <el-table :data="list" stripe>
      <el-table-column prop="currency" :label="t('admin.fx.currency')" width="100" />
      <el-table-column prop="currencyEn" label="Currency" />
      <el-table-column prop="symbol" :label="t('admin.fx.symbol')" width="90" />
      <el-table-column prop="rate" :label="t('admin.fx.rate')" width="140" />
      <el-table-column :label="t('admin.fx.source')" width="110">
        <template #default="{ row }">
          <el-tag :type="row.source === 'AUTO' ? 'success' : 'info'" size="small">
            {{ row.source === 'AUTO' ? t('admin.fx.auto') : t('admin.fx.manual') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" :label="t('admin.fx.updateTime')" width="180" />
      <el-table-column :label="t('admin.order.actions')" width="110">
        <template #default="{ row }">
          <el-button v-if="row.currency !== 'USD'" size="small" @click="openEdit(row)">
            {{ t('admin.fx.editRate') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="`${t('admin.fx.editRate')} ${form.currency}`" width="380px">
      <el-form label-width="110px">
        <el-form-item :label="t('admin.fx.rate')">
          <el-input-number v-model="form.rate" :precision="6" :step="0.01" :min="0.000001" style="width: 100%" />
        </el-form-item>
      </el-form>
      <div class="rate-tip">1 USD = {{ form.rate }} {{ form.currency }} {{ form.symbol }}</div>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('admin.action.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ t('admin.action.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.page-title { font-weight: 700; font-size: 16px; }
.rate-tip { text-align: center; color: #909399; font-size: 13px; }
</style>
