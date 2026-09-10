<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { lineList, lineSave, lineDelete } from '../../api/admin'
import { COUNTRIES } from '../../utils/format'
import { usd } from '../../utils/format'

const { t } = useI18n()
const list = ref([])
const dialogVisible = ref(false)
const form = reactive({
  id: null, name: '', nameEn: '', carrier: '', supportedCountries: ['*'],
  firstWeightGrams: 500, firstFeeCents: 500,
  continueWeightGrams: 500, continueFeeCents: 250,
  etaMinDays: 7, etaMaxDays: 15, status: 1
})

const countryOptions = [{ code: '*', name: t('admin.line.all') },
  ...COUNTRIES.map(c => ({ code: c.code, name: c.name }))]

async function load() {
  list.value = await lineList()
}

function openEdit(row) {
  const countries = row ? JSON.parse(row.supportedCountries) : ['*']
  Object.assign(form, row || {}, { supportedCountries: countries, id: row?.id || null })
  dialogVisible.value = true
}

async function save() {
  if (!form.name || !form.nameEn || !form.carrier || form.supportedCountries.length === 0) {
    ElMessage.warning('名称/承运商/目的国必填')
    return
  }
  await lineSave(form)
  dialogVisible.value = false
  load()
}

async function remove(id) {
  await ElMessageBox.confirm(t('address.deleteConfirm'), { type: 'warning' })
  await lineDelete(id)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <span class="page-title">{{ t('admin.logistics') }}</span>
      <el-button type="primary" @click="openEdit(null)">+ {{ t('admin.line.add') }}</el-button>
    </div>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" :label="t('admin.line.name')" min-width="130" />
      <el-table-column prop="nameEn" label="Name (EN)" min-width="140" />
      <el-table-column prop="carrier" :label="t('admin.line.carrier')" min-width="120" />
      <el-table-column :label="t('admin.line.countries')" min-width="150">
        <template #default="{ row }">
          <el-tag v-if="JSON.parse(row.supportedCountries).includes('*')" type="success" size="small">
            {{ t('admin.line.all') }}
          </el-tag>
          <span v-else>{{ JSON.parse(row.supportedCountries).join(', ') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="首重/首重费" width="150">
        <template #default="{ row }">
          {{ row.firstWeightGrams }}g / {{ usd(row.firstFeeCents) }}
        </template>
      </el-table-column>
      <el-table-column label="续重" width="130">
        <template #default="{ row }">
          {{ row.continueWeightGrams }}g / {{ usd(row.continueFeeCents) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('admin.line.eta')" width="90">
        <template #default="{ row }">{{ row.etaMinDays }}-{{ row.etaMaxDays }}</template>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? t('admin.action.edit') : t('admin.line.add')" width="620px">
      <el-form label-width="130px">
        <el-form-item :label="t('admin.line.name')"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Name (EN)"><el-input v-model="form.nameEn" /></el-form-item>
        <el-form-item :label="t('admin.line.carrier')"><el-input v-model="form.carrier" /></el-form-item>
        <el-form-item :label="t('admin.line.countries')">
          <el-select v-model="form.supportedCountries" multiple style="width: 100%">
            <el-option v-for="c in countryOptions" :key="c.code" :value="c.code" :label="c.name" />
          </el-select>
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item :label="t('admin.line.firstWeight')">
              <el-input-number v-model="form.firstWeightGrams" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('admin.line.firstFee')">
              <el-input-number v-model="form.firstFeeCents" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item :label="t('admin.line.continueWeight')">
              <el-input-number v-model="form.continueWeightGrams" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('admin.line.continueFee')">
              <el-input-number v-model="form.continueFeeCents" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="6">
            <el-form-item :label="t('admin.line.eta')">
              <el-input-number v-model="form.etaMinDays" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="~">
              <el-input-number v-model="form.etaMaxDays" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0"
                         active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>
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
</style>
