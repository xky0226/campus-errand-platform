import { get, post } from '@/utils/request.js'

// 余额概览：{ balance, frozenBalance, totalEarnings, alipayAccount }
export function getBalance() {
	return get('/wallet/balance')
}

// 交易记录。type 可传 recharge / order_pay / order_earn / order_refund / withdraw，不传查全部
export function getTransactions(type = '', limit = 50) {
	let url = '/wallet/transactions?limit=' + limit
	if (type) {
		url += '&type=' + type
	}
	return get(url)
}

// 充值（模拟）：调用成功就到账
export function recharge(amount, payMethod = '支付宝') {
	return post('/wallet/recharge', { amount, payMethod })
}

// 申请提现：生成一条待审核流水，等管理端财务模块审核
export function withdraw(amount, account) {
	return post('/wallet/withdraw', { amount, account })
}
