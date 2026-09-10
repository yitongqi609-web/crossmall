<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { goodsAdminPage, goodsSave, goodsUpdateStatus, categoryList } from '../../api/admin'
import { usd } from '../../utils/format'

const { t } = useI18n()

const list = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const categories = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: null, categoryId: null, title: '', titleEn: '', description: '', descriptionEn: '',
  brand: '', hsCode: '', weightGrams: 500, mainImage: '', status: 1, skus: []
})

async function load() {
  loading.value = true
  try {
    const data = await goodsAdminPage({ keyword: keyword.value || undefined, page: page.value, size: 10 })
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(form, {
    id: row?.id || null,
    categoryId: row?.categoryId || categories.value[0]?.id,
    title: row?.title || '',
    titleEn: row?.titleEn || '',
    description: row?.description || '',
    descriptionEn: row?.descriptionEn || '',
    brand: row?.brand || '',
    hsCode: row?.hsCode || '',
    weightGrams: row?.weightGrams || 500,
    mainImage: row?.mainImage || '',
    status: row?.status ?? 1,
    skus: row?.skus ? JSON.parse(JSON.stringify(row.skus)) : []
  })
  dialogVisible.value = true
}

function addSku() {
  form.skus.push({ id: null, attrs: '[{"k":"颜色","kEn":"Color","v":"默认","vEn":"Default"}]',
    priceCents: 1000, stock: 100, weightGrams: 500, status: 1 })
}

function removeSku(index) {
  form.skus.splice(index, 1)
}

async function save() {
  if (!form.title || !form.titleEn || !form.hsCode || !form.categoryId) {
    ElMessage.warning('标题/HS 编码/分类必填')
    return
  }
  if (form.skus.length === 0) {
    ElMessage.warning('至少一个 SKU')
    return
  }
  for (const sku of form.skus) {
    try {
      JSON.parse(sku.attrs)
    } catch (e) {
      ElMessage.warning('SKU 规格 JSON 格式错误')
      return
    }
  }
  saving.value = true
  try {
    await goodsSave(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  await goodsUpdateStatus(row.id, row.status === 1 ? 0 : 1)
  load()
}

onMounted(async () => {
  load()
  categories.value = await categoryList()
})
</script>

<template>
  <div>
    <div class="toolbar">
      <span class="page-title">{{ t('admin.goods.title') }}</span>
      <div class="toolbar-right">
        <el-input v-model="keyword" :placeholder="t('admin.goods.keyword')" clearable
                  style="width: 220px" @keyup.enter="page = 1; load()" />
        <el-button :icon="'Search'" @click="page = 1; load()" />
        <el-button type="primary" @click="openEdit(null)">{{ t('admin.goods.add') }}</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题(中文)" min-width="180" show-overflow-tooltip />
      <el-table-column prop="titleEn" label="Title (EN)" min-width="180" show-overflow-tooltip />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">{{ row.categoryName }}</template>
      </el-table-column>
      <el-table-column prop="hsCode" label="HS" width="90" />
      <el-table-column label="SKU数" width="70" prop="skuCount" />
      <el-table-column label="最低价" width="100">
        <template #default="{ row }">{{ usd(row.minPriceCents) }}</template>
      </el-table-column>
      <el-table-column prop="totalStock" label="库存" width="80" />
      <el-table-column prop="sales" label="销量" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? t('admin.goods.onSale') : t('admin.goods.offSale') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('admin.order.actions')" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">{{ t('admin.action.edit') }}</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? t('admin.goods.offSale') : t('admin.goods.onSale') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination v-model:current-page="page" :total="total" :page-size="10"
                     layout="total, prev, pager, next" background @current-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? t('admin.goods.edit') : t('admin.goods.add')"
               width="860px" top="4vh">
      <el-divider content-position="left">{{ t('admin.goods.basic') }}</el-divider>
      <el-form label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="form.brand" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标题(中文)">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="详情(中文)">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>

        <el-divider content-position="left">{{ t('admin.goods.basicEn') }}</el-divider>
        <el-form-item label="Title (EN)">
          <el-input v-model="form.titleEn" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="form.descriptionEn" type="textarea" :rows="2" />
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="HS 编码">
              <el-input v-model="form.hsCode" placeholder="8517.13" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="t('admin.goods.weight')">
              <el-input-number v-model="form.weightGrams" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0"
                         active-text="上架" inactive-text="下架" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">{{ t('admin.goods.skus') }}</el-divider>
        <div v-for="(sku, i) in form.skus" :key="i" class="sku-row">
          <el-input v-model="sku.attrs" :placeholder="t('admin.goods.attrsJson')" style="flex: 2" />
          <el-input-number v-model="sku.priceCents" :min="1" :placeholder="t('admin.goods.price')" controls-position="right" />
          <el-input-number v-model="sku.stock" :min="0" :placeholder="t('admin.goods.stock')" controls-position="right" />
          <el-input-number v-model="sku.weightGrams" :min="1" :placeholder="t('admin.goods.weight')" controls-position="right" />
          <el-button type="danger" text @click="removeSku(i)">✕</el-button>
        </div>
        <el-button size="small" @click="addSku">+ {{ t('admin.goods.addSku') }}</el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('admin.action.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ t('admin.action.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.page-title { font-weight: 700; font-size: 16px; }
.toolbar-right { display: flex; gap: 8px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.sku-row { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
</style>
