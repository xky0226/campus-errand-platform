import { get, post, put } from '@/utils/request.js'

export function createOrder(data) {
	return post('/orders', data)
}

export function getAvailableOrders() {
	return get('/orders/available')
}

export function getMyPublishedOrders() {
	return get('/orders/my/published')
}

export function getMyRunningOrders() {
	return get('/orders/my/running')
}

export function getOrderDetail(id) {
	return get('/orders/' + id)
}

export function payOrder(id, payMethod = 'wallet') {
	return post('/orders/' + id + '/pay', { payMethod })
}

export function acceptOrder(id) {
	return get('/orders/' + id + '/accept')
}

export function deliverOrder(id) {
	return get('/orders/' + id + '/deliver')
}

export function confirmOrder(id) {
	return get('/orders/' + id + '/confirm')
}

export function cancelOrder(id, reason = '用户主动取消') {
	return get('/orders/' + id + '/cancel?reason=' + encodeURIComponent(reason))
}

export function updateOrder(id, data) {
	return put('/orders/' + id, data)
}

export function rateOrder(id, data) {
	return post('/orders/' + id + '/rate', data)
}
