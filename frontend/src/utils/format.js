/**
 * 金额工具:后端金额一律为「分」,展示 = 分/100 保留两位(按所选币种符号)
 */
export function money(symbol, cents) {
  if (cents === null || cents === undefined) return '-'
  return `${symbol || ''}${(cents / 100).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}

/** USD 分展示(订单内部金额) */
export function usd(cents) {
  return money('$', cents)
}

/**
 * 常见目的国(地址表单/税则展示):dial = 国际区号,flag = 国旗 emoji
 */
export const COUNTRIES = [
  { code: 'CN', name: 'China', zh: '中国', dial: '+86', flag: '🇨🇳' },
  { code: 'US', name: 'United States', zh: '美国', dial: '+1', flag: '🇺🇸' },
  { code: 'CA', name: 'Canada', zh: '加拿大', dial: '+1', flag: '🇨🇦' },
  { code: 'GB', name: 'United Kingdom', zh: '英国', dial: '+44', flag: '🇬🇧' },
  { code: 'DE', name: 'Germany', zh: '德国', dial: '+49', flag: '🇩🇪' },
  { code: 'FR', name: 'France', zh: '法国', dial: '+33', flag: '🇫🇷' },
  { code: 'SE', name: 'Sweden', zh: '瑞典', dial: '+46', flag: '🇸🇪' },
  { code: 'JP', name: 'Japan', zh: '日本', dial: '+81', flag: '🇯🇵' },
  { code: 'KR', name: 'South Korea', zh: '韩国', dial: '+82', flag: '🇰🇷' },
  { code: 'AU', name: 'Australia', zh: '澳大利亚', dial: '+61', flag: '🇦🇺' },
  { code: 'NZ', name: 'New Zealand', zh: '新西兰', dial: '+64', flag: '🇳🇿' },
  { code: 'SG', name: 'Singapore', zh: '新加坡', dial: '+65', flag: '🇸🇬' },
  { code: 'AE', name: 'United Arab Emirates', zh: '阿联酋', dial: '+971', flag: '🇦🇪' }
]

/** 币种 → 国旗/地区标识(导航栏币种切换器展示) */
export const CURRENCY_FLAGS = {
  USD: '🇺🇸', EUR: '🇪🇺', GBP: '🇬🇧', JPY: '🇯🇵', AUD: '🇦🇺', CNY: '🇨🇳'
}

/** 按国家码取区号 */
export function dialOf(countryCode) {
  const c = COUNTRIES.find(x => x.code === countryCode)
  return c ? c.dial : '+86'
}

export const CURRENCIES = ['USD', 'EUR', 'GBP', 'JPY', 'AUD', 'CNY']

/**
 * 订单状态 → el-tag 类型
 */
export const STATUS_TAG = {
  PENDING_PAYMENT: 'warning',
  PAID: 'primary',
  STOCKED: 'primary',
  DECLARED: 'primary',
  IN_TRANSIT: 'primary',
  CUSTOMS_CLEARANCE: 'primary',
  DELIVERING: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info',
  REFUNDED: 'danger'
}

/** SKU 规格双语字段展示:按 locale 取 kEn/vEn */
export function formatAttrs(attrsJson, locale) {
  try {
    const attrs = JSON.parse(attrsJson)
    const useEn = locale === 'en'
    return attrs.map(a => `${useEn ? a.kEn || a.k : a.k}: ${useEn ? a.vEn || a.v : a.v}`).join(' / ')
  } catch (e) {
    return attrsJson
  }
}

/** 履约轨迹状态码 → 展示名 */
export const TRACK_STATUS = {
  PENDING_PAYMENT: { zh: '订单创建', en: 'Order Created' },
  PAID: { zh: '支付成功', en: 'Paid' },
  STOCKED: { zh: '备货完成', en: 'Stocked' },
  DECLARED: { zh: '出口报关', en: 'Declared' },
  IN_TRANSIT: { zh: '干线运输', en: 'In Transit' },
  CUSTOMS_CLEARANCE: { zh: '清关中', en: 'Customs' },
  DELIVERING: { zh: '派送中', en: 'Delivering' },
  COMPLETED: { zh: '已妥投', en: 'Delivered' },
  CANCELLED: { zh: '已取消', en: 'Cancelled' },
  REFUNDED: { zh: '已退款', en: 'Refunded' }
}

/** 分类占位图标(与种子数据分类一致) */
export function categoryIcon(categoryId) {
  const map = { 1: '📱', 2: '👕', 3: '🏠', 4: '💄', 5: '🏕️' }
  return map[categoryId] || '🛍️'
}
