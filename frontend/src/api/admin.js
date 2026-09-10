import request from './request'

// ===== 管理员 =====
export const adminLogin = data => request.post('/admin/auth/login', data)

// ===== 商品/分类/线路 =====
export const goodsAdminPage = params => request.get('/admin/goods/page', { params })
export const goodsSave = data => request.post('/admin/goods', data)
export const goodsUpdateStatus = (spuId, status) =>
  request.put(`/admin/goods/${spuId}/status`, null, { params: { status } })
export const categoryList = () => request.get('/admin/category')
export const categorySave = data => request.post('/admin/category', data)
export const categoryDelete = id => request.delete(`/admin/category/${id}`)
export const lineList = () => request.get('/admin/logistics/lines')
export const lineSave = data => request.post('/admin/logistics/lines', data)
export const lineDelete = id => request.delete(`/admin/logistics/lines/${id}`)

// ===== 汇率/税则 =====
export const fxAdminList = () => request.get('/admin/fx/rates')
export const fxAdminUpdate = (currency, rate) =>
  request.put(`/admin/fx/rates/${currency}`, null, { params: { rate } })
export const fxPull = () => request.post('/admin/fx/pull')
export const taxList = params => request.get('/admin/tax', { params })
export const taxSave = data => request.post('/admin/tax', data)
export const taxDelete = id => request.delete(`/admin/tax/${id}`)

// ===== 订单履约/看板 =====
export const adminOrderPage = params => request.get('/admin/order/page', { params })
export const adminOrderDetail = orderNo => request.get(`/admin/order/${orderNo}`)
export const adminOrderTransit = (orderNo, data) => request.post(`/admin/order/${orderNo}/transit`, data)
export const adminCloseExpired = limit =>
  request.post('/admin/order/close-expired', null, { params: { limit } })
export const dashboard = () => request.get('/admin/dashboard')
