<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taxList, taxSave, taxDelete } from '../../api/admin'
import { COUNTRIES } from '../../utils/format'

const { t } = useI18n()
const list = ref([])
const filterCountry = ref('')
const dialogVisible = ref(false)
const form = reactive({
  id: null, countryCode: 'US', countryName: 'United States',
  hsCode: '*', hsName: '', taxRate: 0.1, deMinimisCents: 0
})

async function load() {
  list.value = await taxList({ countryCode: filterCountry.value || undefined })
}

function countryLabel(code) {
  if (code === '*') return t('admin.tax.globalDefault')
  const c = COUNTRIES.find(x => x.code === code)
  return c ? `${c.name} (${code})` : code
}

function openEdit(row) {
  Object.assign(form, row || {
    id: null, countryCode: 'US', countryName: 'United States',
    hsCode: '*', hsName: '通用税率', taxRate: 0.1, deMinimisCents: 0
  })
  dialogVisible.value = true
}

function onCountryChange(code) {
  const c = COUNTRIES.find(x => x.code === code)
  if (c) form.countryName = c.name
}

async function save() {
  if (!form.hsName) {
    ElMessage.warning('品类名必填')
    return
  }
  await taxSave({ ...form, hsCode: form.hsCode.toUpperCase() })
  dialogVisible.value = false
  load()
}

async function remove(id) {
  await ElMessageBox.confirm(t('address.deleteConfirm'), { type: 'warning' })
  await taxDelete(id)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <span class="page-title">{{ t('admin.tax') }}</span>
      <div class="toolbar-right">
        <el-select v-model="filterCountry" clearable placeholder="全部国家" style="width: 180px"
                   @change="load">
          <el-option v-for="c in COUNTRIES" :key="c.code" :value="c.code" :label="c.name" />
        </el-select>
        <el-button type="primary" @click="openEdit(null)">+ {{ t('admin.tax.add') }}</el-button>
      </div>
    </div>
    <el-table :data="list" stripe>
      <el-table-column :label="t('admin.tax.country')" min-width="160">
        <template #default="{ row }">{{ countryLabel(row.countryCode) }}</template>
      </el-table-column>
      <el-table-column prop="hsCode" :label="t('admin.tax.hsCode')" width="110" />
      <el-table-column prop="hsName" :label="t('admin.tax.hsName')" min-width="140" />
      <el-table-column :label="t('admin.tax.taxRate')" width="100">
        <template #default="{ row }">{{ (row.taxRate * 100).toFixed(2) }}%</template>
      </el-table-column>
      <el-table-column :label="t('admin.tax.deMinimis')" width="160">
        <template #default="{ row }">
          {{ row.deMinimisCents > 0 ? `$${(row.deMinimisCents / 100).toFixed(0)}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('admin.order.actions')" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">{{ t('admin.action.edit') }}</el-button>
          <el-button size="small" type="danger" text @click="remove(row.id)">
            {{ t('admin.action.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? t('admin.action.edit') : t('admin.tax.add')" width="480px">
      <el-form label-width="130px">
        <el-form-item :label="t('admin.tax.country')">
          <el-select v-model="form.countryCode" style="width: 100%" @change="onCountryChange">
            <el-option v-for="c in COUNTRIES" :key="c.code" :value="c.code" :label="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.tax.hsCode')">
          <el-input v-model="form.hsCode" placeholder="* 或 8517.13" />
        </el-form-item>
        <el-form-item :label="t('admin.tax.hsName')">
          <el-input v-model="form.hsName" />
        </el-form-item>
        <el-form-item :label="t('admin.tax.taxRate')">
          <el-input-number v-model="form.taxRate" :precision="4" :step="0.01" :min="0" :max="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('admin.tax.deMinimis')">
          <el-input-number v-model="form.deMinimisCents" :min="0" :step="1000" style="width: 100%" />
        </el-form-item>
      </el-form>
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
.toolbar-right { display: flex; gap: 8px; }
</style>
