<template>
  <section class="card">
    <div class="user-block">
      <el-avatar :size="52" style="background: var(--cm-gold); font-size: 24px">🙋</el-avatar>
      <div>
        <div class="user-nickname">{{ user.userInfo?.nickname }}</div>
        <div class="text-sub">{{ user.userInfo?.email }}</div>
      </div>
    </div>
  </section>

  <section class="card" style="margin-top: 14px">
    <div class="head-tip">
      🌍 {{ locale === 'en'
        ? 'Address country decides shipping lines & duty rules at checkout'
        : '地址中的国家将决定结算页可选的物流线路与关税税则' }}
    </div>
    <div class="addr-head">
      <b>📍 {{ t('address.title') }}</b>
      <el-button type="primary" class="cm-btn-primary" round size="small" @click="openDialog(null)">
        + {{ t('address.add') }}
      </el-button>
    </div>
    <el-empty v-if="addresses.length === 0" :description="t('address.empty')" :image-size="70" />
    <div v-for="a in addresses" :key="a.id" class="addr-row">
      <div>
        <b>{{ a.receiverName }}</b> · {{ a.phone }}
        <el-tag v-if="a.isDefault === 1" size="small" effect="plain" style="margin-left: 8px">
          {{ t('address.isDefault') }}
        </el-tag>
        <div class="text-sub" style="margin-top: 4px">
          {{ countryNameOf(a.countryCode) }} · {{ a.stateProvince || '-' }} · {{ a.city }} · {{ a.street }} · {{ a.postcode }}
        </div>
      </div>
      <div class="addr-actions">
        <el-button size="small" round text type="primary" @click="openDialog(a)">{{ t('address.edit') }}</el-button>
        <el-button v-if="a.isDefault !== 1" size="small" round text @click="setDefault(a.id)">
          {{ t('address.setDefault') }}
        </el-button>
        <el-button size="small" round text type="danger" @click="remove(a.id)">{{ t('address.delete') }}</el-button>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? t('address.edit') : t('address.add')" width="520px">
      <el-form label-position="top" class="addr-form">
        <el-form-item :label="t('address.receiver')"><el-input v-model="form.receiverName" /></el-form-item>
        <el-form-item :label="t('address.country')">
          <el-select v-model="form.countryCode" style="width: 100%" @change="onCountryChange">
            <el-option v-for="c in COUNTRIES" :key="c.code" :value="c.code"
                       :label="locale === 'en' ? c.name : `${c.name} ${c.zh}`">
              <span class="opt-row">
                <span>{{ locale === 'en' ? c.name : `${c.name} ${c.zh}` }}</span>
                <span class="option-sub">{{ c.dial }}</span>
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="t('address.phone')">
          <el-input v-model="form.phoneLocal" :placeholder="phoneDial + ' 13800138000'">
            <template #prepend>
              <span class="dial-prefix">{{ phoneDial }}</span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('address.state')"><el-input v-model="form.stateProvince" /></el-form-item>
        <el-form-item :label="t('address.city')"><el-input v-model="form.city" /></el-form-item>
        <el-form-item :label="t('address.street')"><el-input v-model="form.street" /></el-form-item>
        <el-form-item :label="t('address.postcode')"><el-input v-model="form.postcode" /></el-form-item>
        <el-form-item :label="t('address.isDefault')">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="dialogVisible = false">{{ t('admin.action.cancel') }}</el-button>
        <el-button type="primary" round class="cm-btn-primary" :loading="saving" @click="save">
          {{ t('admin.action.save') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addressList, addressSave, addressUpdate, addressDelete, addressSetDefault } from '../api/mall'
import { useUserStore } from '../store/user'
import { COUNTRIES, dialOf } from '../utils/format'

const { t, locale } = useI18n()
const user = useUserStore()

const addresses = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: null, receiverName: '', phone: '', phoneLocal: '', countryCode: 'CN',
  countryName: 'China', stateProvince: '', city: '', street: '', postcode: '', isDefault: 0
})

/** 当前国家的区号(随国家切换联动) */
const phoneDial = computed(() => dialOf(form.countryCode))

/**
 * 从完整国际格式号码(如 "+44 20 7946 0958")中剥离区号,
 * 返回不带区号的本地号码;未识别的区号按 +86 处理
 */
function splitPhone(full) {
  const cleaned = (full || '').trim()
  const hit = COUNTRIES.find(c => cleaned.replace(/\s/g, '').startsWith(c.dial.replace(/\s/g, '')))
  return hit ? cleaned.trim().slice(hit.dial.length).trim() : cleaned
}

function countryNameOf(code) {
  const c = COUNTRIES.find(x => x.code === code)
  return c ? (locale.value === 'en' ? c.name : c.zh) : code
}

function openDialog(address) {
  if (address) {
    Object.assign(form, address)
    form.phoneLocal = splitPhone(address.phone)
  } else {
    Object.assign(form, {
      id: null, receiverName: '', phone: '', phoneLocal: '', countryCode: 'CN',
      countryName: 'China', stateProvince: '', city: '', street: '', postcode: '', isDefault: 0
    })
  }
  dialogVisible.value = true
}

function onCountryChange(code) {
  const c = COUNTRIES.find(x => x.code === code)
  if (c) form.countryName = c.name
  // 若号码里残留了旧区号则剥离,前缀由 prepend 展示,避免出现 "+86 +1 202..."
  form.phoneLocal = splitPhone(form.phoneLocal)
}

async function load() {
  addresses.value = await addressList()
}

async function save() {
  if (!form.receiverName || !form.phoneLocal || !form.city || !form.street || !form.postcode) {
    ElMessage.warning(t('address.title') + '*')
    return
  }
  saving.value = true
  try {
    // 存完整国际格式:"+86 13800138000"
    form.phone = `${dialOf(form.countryCode)} ${form.phoneLocal.trim()}`.trim()
    if (form.id) {
      await addressUpdate(form.id, form)
    } else {
      await addressSave(form)
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await ElMessageBox.confirm(t('address.deleteConfirm'), { type: 'warning' })
  await addressDelete(id)
  load()
}

async function setDefault(id) {
  await addressSetDefault(id)
  load()
}

onMounted(async () => {
  if (!user.userInfo) {
    await user.fetchMe()
  }
  load()
})
</script>

<style scoped>
.user-block {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-nickname {
  font-size: 17px;
  font-weight: 700;
}

.addr-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.addr-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.addr-row:last-child {
  border-bottom: none;
}

.dial-prefix {
  min-width: 44px;
  display: inline-flex;
  justify-content: center;
  font-variant-numeric: tabular-nums;
  color: var(--cm-primary-deep);
  font-weight: 600;
}

.opt-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
}

.option-sub {
  color: #8c8c8c;
  font-size: 12px;
}

/* 双语 label 长度差异大,防止表单控件溢出对话框 */
.addr-form :deep(.el-form-item__content) {
  min-width: 0;
}
</style>
