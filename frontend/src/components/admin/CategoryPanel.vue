<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryList, categorySave, categoryDelete } from '../../api/admin'

const { t } = useI18n()
const list = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, name: '', nameEn: '', icon: '🛍️', sort: 0, status: 1 })

async function load() {
  list.value = await categoryList()
}

function openEdit(row) {
  Object.assign(form, row || { id: null, name: '', nameEn: '', icon: '🛍️', sort: 0, status: 1 })
  dialogVisible.value = true
}

async function save() {
  if (!form.name || !form.nameEn) {
    ElMessage.warning('中英文名必填')
    return
  }
  await categorySave(form)
  dialogVisible.value = false
  load()
}

async function remove(id) {
  await ElMessageBox.confirm(t('address.deleteConfirm'), { type: 'warning' })
  await categoryDelete(id)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <span class="page-title">{{ t('admin.category') }}</span>
      <el-button type="primary" @click="openEdit(null)">+ {{ t('admin.action.save') }}</el-button>
    </div>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="图标" width="70">
        <template #default="{ row }">{{ row.icon }}</template>
      </el-table-column>
      <el-table-column prop="name" label="中文名" />
      <el-table-column prop="nameEn" label="English" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? t('admin.action.edit') : t('admin.action.save')" width="440px">
      <el-form label-width="90px">
        <el-form-item label="图标(emoji)"><el-input v-model="form.icon" style="width: 100px" /></el-form-item>
        <el-form-item label="中文名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="English"><el-input v-model="form.nameEn" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
</style>
