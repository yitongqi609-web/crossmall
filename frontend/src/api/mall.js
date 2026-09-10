import request from './request'

// ===== 账号 =====
export const register = data => request.post('/auth/register', data)
export const login = data => request.post('/auth/login', data)
export const me = () => request.get('/user/me')

// ===== 首页/商品/汇率(公开) =====
export const home = currency => request.get('/home', { params: { currency } })
export const goodsPage = params => request.get('/goods', { params })
export const goodsDetail = (spuId, currency) => request.get(`/goods/${spuId}`, { params: { currency } })
export const fxRates = () => request.get('/fx/rates')

// ===== 地址 =====
export const addressList = () => request.get('/address')
export const addressSave = data => request.post('/address', data)
export const addressUpdate = (id, data) => request.put(`/address/${id}`, data)
export const addressDelete = id => request.delete(`/address/${id}`)
export const addressSetDefault = id => request.put(`/address/${id}/default`)

// ===== 购物车 =====
export const cartList = currency => request.get('/cart', { params: { currency } })
export const cartCount = () => request.get('/cart/count')
export const cartAdd = data => request.post('/cart/items', data)
export const cartUpdateQty = (skuId, quantity) => request.put(`/cart/items/${skuId}`, null, { params: { quantity } })
export const cartRemove = skuIds => request.delete('/cart/items', { params: { skuIds: skuIds.join(',') } })
export const cartClear = () => request.post('/cart/clear')

// ===== 订单 =====
export const orderPreview = data => request.post('/order/preview', data)
export const orderCreate = data => request.post('/order', data)
export const orderPage = params => request.get('/order/page', { params })
export const orderDetail = orderNo => request.get(`/order/${orderNo}`)
export const orderCancel = (orderNo, reason) => request.post(`/order/${orderNo}/cancel`, null, { params: { reason } })

// ===== 支付 =====
export const payCreate = (orderNo, channel) => request.post(`/pay/${orderNo}`, { channel })
export const mockPaySubmit = txnNo => request.post('/mock/pay/submit', null, { params: { txnNo } })
