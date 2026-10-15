<template>
	<view class="page">
		<view v-if="!token" class="empty">
			<text class="empty-title">还没有登录</text>
			<text class="empty-desc">登录后才能查看钱包</text>
			<button class="btn primary" @tap="goLogin">去登录</button>
		</view>

		<template v-else>
			<!-- 余额卡片 -->
			<view class="wallet-card">
				<text class="wallet-label">可用余额（元）</text>
				<text class="wallet-amount">{{ money(balance.balance) }}</text>
				<view class="wallet-sub">
					<text class="sub-item">冻结 {{ money(balance.frozenBalance) }}</text>
					<text class="sub-item">累计收益 {{ money(balance.totalEarnings) }}</text>
				</view>
				<view class="wallet-actions">
					<view class="wallet-btn" @tap="openRecharge">
						<text class="wallet-btn-text">充值</text>
					</view>
					<view class="wallet-btn" @tap="openWithdraw">
						<text class="wallet-btn-text">提现</text>
					</view>
				</view>
			</view>

			<view class="tip-card">
				订单支付的赏金会先冻结在平台（托管），等发布者确认收货后才结算给跑腿员；
				取消或仲裁退款的订单，钱会原路回到可用余额。
			</view>

			<!-- 交易记录 -->
			<view class="section-card">
				<view class="section-head">
					<text class="section-title">交易记录</text>
					<text class="link" @tap="loadAll">刷新</text>
				</view>

				<!-- 类型筛选 -->
				<scroll-view class="filter-bar" scroll-x>
					<view v-for="item in filters" :key="item.value" class="filter-item"
						:class="{ active: filter === item.value }" @tap="changeFilter(item.value)">
						<text class="filter-text">{{ item.label }}</text>
					</view>
				</scroll-view>

				<view v-if="loading" class="empty-line">加载中...</view>
				<view v-else-if="!records.length" class="empty-line">还没有交易记录</view>

				<view v-for="record in records" :key="record.id" class="record-row">
					<view class="record-left">
						<text class="record-title">{{ typeName(record.type) }}</text>
						<text class="record-desc">{{ record.remark || '-' }}</text>
						<text class="record-time">{{ formatTime(record.createdAt) }}</text>
					</view>
					<view class="record-right">
						<text class="record-amount" :class="record.direction > 0 ? 'income' : 'outcome'">
							{{ record.direction > 0 ? '+' : '-' }}{{ money(record.amount) }}
						</text>
						<text class="record-status" :class="'status-' + record.status">{{ statusName(record) }}</text>
					</view>
				</view>
			</view>
		</template>

		<!-- 充值的弹层 -->
		<view v-if="showRecharge" class="mask" @tap="showRecharge = false">
			<view class="sheet" @tap.stop>
				<text class="sheet-title">充值</text>
				<view class="amount-row">
					<text class="amount-label">金额（元）</text>
					<input class="input" type="digit" v-model="rechargeForm.amount" placeholder="请输入充值金额" />
				</view>
				<view class="quick-row">
					<view v-for="q in quickAmounts" :key="q" class="quick-item"
						:class="{ active: rechargeForm.amount === String(q) }" @tap="rechargeForm.amount = String(q)">
						<text class="quick-text">{{ q }} 元</text>
					</view>
				</view>
				<view class="amount-row">
					<text class="amount-label">支付方式</text>
					<picker mode="selector" :range="payMethods" @change="onPayMethodChange">
						<view class="picker-value">{{ rechargeForm.payMethod }}</view>
					</picker>
				</view>
				<view class="tip">本次实训没有接真实支付渠道，点确认即视为支付成功</view>
				<button class="btn primary" :loading="submitting" @tap="submitRecharge">
					{{ submitting ? '处理中...' : '确认充值' }}
				</button>
			</view>
		</view>

		<!-- 提现的弹层 -->
		<view v-if="showWithdraw" class="mask" @tap="showWithdraw = false">
			<view class="sheet" @tap.stop>
				<text class="sheet-title">提现申请</text>
				<view class="amount-row">
					<text class="amount-label">金额（元）</text>
					<input class="input" type="digit" v-model="withdrawForm.amount" placeholder="最低 10 元" />
				</view>
				<view class="amount-row">
					<text class="amount-label">收款账号</text>
					<input class="input" v-model="withdrawForm.account" placeholder="支付宝账号" />
				</view>
				<view class="tip">
					提交后钱会先冻结，等管理员在财务模块审核通过才真正转出；被驳回会退回可用余额
				</view>
				<button class="btn primary" :loading="submitting" @tap="submitWithdraw">
					{{ submitting ? '提交中...' : '提交申请' }}
				</button>
			</view>
		</view>
	</view>
</template>

<script>
	import {
		getBalance,
		getTransactions,
		recharge,
		withdraw
	} from '@/services/wallet.js'

	export default {
		data() {
			return {
				token: '',
				balance: {},
				records: [],
				loading: false,
				submitting: false,
				filter: '',
				filters: [{
					label: '全部',
					value: ''
				}, {
					label: '充值',
					value: 'recharge'
				}, {
					label: '订单支付',
					value: 'order_pay'
				}, {
					label: '订单收益',
					value: 'order_earn'
				}, {
					label: '退款',
					value: 'order_refund'
				}, {
					label: '提现',
					value: 'withdraw'
				}],
				typeNames: {
					recharge: '充值',
					order_pay: '订单支付',
					order_earn: '订单收益',
					order_refund: '订单退款',
					withdraw: '提现'
				},
				showRecharge: false,
				showWithdraw: false,
				quickAmounts: [20, 50, 100, 200],
				payMethods: ['支付宝', '微信支付'],
				rechargeForm: {
					amount: '',
					payMethod: '支付宝'
				},
				withdrawForm: {
					amount: '',
					account: ''
				}
			}
		},
		onShow() {
			this.token = uni.getStorageSync('token') || ''
			if (this.token) {
				this.loadAll()
			}
		},
		methods: {
			async loadAll() {
				await Promise.all([this.loadBalance(), this.loadRecords()])
			},
			async loadBalance() {
				const res = await getBalance()
				if (!res) {
					// token 失效当成未登录处理
					this.token = ''
					return
				}
				this.balance = res
				// 提现弹层的收款账号默认填上次绑定的
				if (!this.withdrawForm.account && res.alipayAccount) {
					this.withdrawForm.account = res.alipayAccount
				}
			},
			async loadRecords() {
				this.loading = true
				try {
					const res = await getTransactions(this.filter)
					this.records = Array.isArray(res) ? res : []
				} finally {
					this.loading = false
				}
			},
			changeFilter(value) {
				if (this.filter === value) return
				this.filter = value
				this.loadRecords()
			},
			money(value) {
				if (value == null) return '0.00'
				return Number(value).toFixed(2)
			},
			typeName(type) {
				return this.typeNames[type] || type
			},
			statusName(record) {
				// 只有提现会停留在「处理中」，其他类型的流水写完就是已完成
				if (record.type !== 'withdraw') return '已完成'
				if (record.status === 0) return '待审核'
				if (record.status === 1) return '已通过'
				return '已驳回'
			},
			formatTime(text) {
				if (!text) return ''
				// 后端返回的是 2026-10-15T10:20:30 这种 ISO 串，换成更好读的写法
				return String(text).replace('T', ' ').slice(0, 16)
			},
			openRecharge() {
				this.rechargeForm.amount = ''
				this.showRecharge = true
			},
			openWithdraw() {
				this.withdrawForm.amount = ''
				this.showWithdraw = true
			},
			onPayMethodChange(e) {
				this.rechargeForm.payMethod = this.payMethods[e.detail.value]
			},
			checkAmount(value, min, max) {
				const amount = Number(value)
				if (!value || isNaN(amount) || amount <= 0) {
					uni.showToast({
						title: '请输入正确的金额',
						icon: 'none'
					})
					return null
				}
				if (min != null && amount < min) {
					uni.showToast({
						title: '金额不能低于 ' + min + ' 元',
						icon: 'none'
					})
					return null
				}
				if (max != null && amount > max) {
					uni.showToast({
						title: '金额不能超过 ' + max + ' 元',
						icon: 'none'
					})
					return null
				}
				return amount.toFixed(2)
			},
			async submitRecharge() {
				const amount = this.checkAmount(this.rechargeForm.amount, 1, 5000)
				if (amount == null) return

				this.submitting = true
				try {
					const res = await recharge(amount, this.rechargeForm.payMethod)
					if (!res) return
					this.showRecharge = false
					uni.showToast({
						title: '充值成功',
						icon: 'success'
					})
					this.filter = ''
					await this.loadAll()
				} finally {
					this.submitting = false
				}
			},
			async submitWithdraw() {
				const amount = this.checkAmount(this.withdrawForm.amount, 10, null)
				if (amount == null) return
				if (!this.withdrawForm.account || !this.withdrawForm.account.trim()) {
					uni.showToast({
						title: '请填写收款账号',
						icon: 'none'
					})
					return
				}
				// 前端先挡一道，后端还会再校验一次余额
				if (Number(amount) > Number(this.balance.balance || 0)) {
					uni.showToast({
						title: '可用余额不足',
						icon: 'none'
					})
					return
				}

				this.submitting = true
				try {
					const res = await withdraw(amount, this.withdrawForm.account.trim())
					if (!res) return
					this.showWithdraw = false
					uni.showToast({
						title: '已提交，等待审核',
						icon: 'none'
					})
					this.filter = ''
					await this.loadAll()
				} finally {
					this.submitting = false
				}
			},
			goLogin() {
				uni.navigateTo({
					url: '/pages/auth/login'
				})
			}
		}
	}
</script>

<style scoped>
	.page {
		min-height: 100vh;
		padding: 24rpx;
		box-sizing: border-box;
		background: #f5f7fb;
	}

	.empty {
		margin-top: 200rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.empty-title {
		color: #334155;
		font-size: 34rpx;
		font-weight: 700;
	}

	.empty-desc {
		margin-top: 16rpx;
		color: #94a3b8;
		font-size: 26rpx;
	}

	.wallet-card {
		display: flex;
		flex-direction: column;
		padding: 40rpx 34rpx;
		margin-bottom: 22rpx;
		border-radius: 26rpx;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		box-shadow: 0 12rpx 32rpx rgba(102, 126, 234, .3);
	}

	.wallet-label {
		color: rgba(255, 255, 255, .8);
		font-size: 26rpx;
	}

	.wallet-amount {
		margin-top: 14rpx;
		color: #fff;
		font-size: 68rpx;
		font-weight: 700;
	}

	.wallet-sub {
		display: flex;
		gap: 34rpx;
		margin-top: 16rpx;
	}

	.sub-item {
		color: rgba(255, 255, 255, .85);
		font-size: 24rpx;
	}

	.wallet-actions {
		display: flex;
		gap: 22rpx;
		margin-top: 32rpx;
	}

	.wallet-btn {
		flex: 1;
		height: 76rpx;
		line-height: 76rpx;
		border-radius: 38rpx;
		background: rgba(255, 255, 255, .95);
		text-align: center;
	}

	.wallet-btn-text {
		color: #667eea;
		font-size: 30rpx;
		font-weight: 600;
	}

	.tip-card {
		padding: 24rpx 28rpx;
		margin-bottom: 22rpx;
		border-radius: 20rpx;
		color: #92400e;
		background: #fef3c7;
		font-size: 24rpx;
		line-height: 1.6;
	}

	.section-card {
		padding: 30rpx;
		margin-bottom: 22rpx;
		border-radius: 26rpx;
		background: #fff;
		box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, .08);
	}

	.section-head {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.section-title {
		color: #0f172a;
		font-size: 32rpx;
		font-weight: 700;
	}

	.link {
		color: #667eea;
		font-size: 26rpx;
	}

	.filter-bar {
		margin: 24rpx 0 6rpx;
		white-space: nowrap;
	}

	.filter-item {
		display: inline-block;
		padding: 10rpx 26rpx;
		margin-right: 16rpx;
		border-radius: 999rpx;
		background: #f1f5f9;
	}

	.filter-item.active {
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	}

	.filter-text {
		color: #64748b;
		font-size: 24rpx;
	}

	.filter-item.active .filter-text {
		color: #fff;
	}

	.empty-line {
		padding: 60rpx 0;
		color: #94a3b8;
		font-size: 26rpx;
		text-align: center;
	}

	.record-row {
		display: flex;
		justify-content: space-between;
		gap: 24rpx;
		padding: 24rpx 0;
		border-bottom: 1rpx solid #f1f5f9;
	}

	.record-row:last-child {
		border-bottom: 0;
	}

	.record-left {
		flex: 1;
		display: flex;
		flex-direction: column;
		min-width: 0;
	}

	.record-title {
		color: #334155;
		font-size: 28rpx;
		font-weight: 600;
	}

	.record-desc {
		margin-top: 8rpx;
		color: #94a3b8;
		font-size: 24rpx;
		word-break: break-all;
	}

	.record-time {
		margin-top: 8rpx;
		color: #cbd5e1;
		font-size: 22rpx;
	}

	.record-right {
		flex-shrink: 0;
		display: flex;
		flex-direction: column;
		align-items: flex-end;
	}

	.record-amount {
		font-size: 32rpx;
		font-weight: 700;
	}

	.income {
		color: #16a34a;
	}

	.outcome {
		color: #ef4444;
	}

	.record-status {
		margin-top: 10rpx;
		font-size: 22rpx;
	}

	.status-0 {
		color: #f59e0b;
	}

	.status-1 {
		color: #94a3b8;
	}

	.status-2 {
		color: #94a3b8;
	}

	.mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		display: flex;
		align-items: flex-end;
		background: rgba(15, 23, 42, .45);
		z-index: 99;
	}

	.sheet {
		width: 100%;
		padding: 40rpx 34rpx 50rpx;
		box-sizing: border-box;
		border-radius: 30rpx 30rpx 0 0;
		background: #fff;
	}

	.sheet-title {
		display: block;
		margin-bottom: 30rpx;
		color: #0f172a;
		font-size: 34rpx;
		font-weight: 700;
	}

	.amount-row {
		display: flex;
		align-items: center;
		margin-bottom: 24rpx;
	}

	.amount-label {
		width: 180rpx;
		color: #334155;
		font-size: 28rpx;
	}

	.input {
		flex: 1;
		height: 82rpx;
		padding: 0 22rpx;
		box-sizing: border-box;
		border-radius: 14rpx;
		background: #f1f5f9;
		font-size: 28rpx;
	}

	.picker-value {
		height: 82rpx;
		line-height: 82rpx;
		padding: 0 22rpx;
		border-radius: 14rpx;
		background: #f1f5f9;
		color: #334155;
		font-size: 28rpx;
	}

	.quick-row {
		display: flex;
		gap: 18rpx;
		margin: 0 0 28rpx 180rpx;
	}

	.quick-item {
		padding: 12rpx 22rpx;
		border-radius: 999rpx;
		background: #f1f5f9;
	}

	.quick-item.active {
		background: #e0e7ff;
	}

	.quick-text {
		color: #64748b;
		font-size: 24rpx;
	}

	.quick-item.active .quick-text {
		color: #4f46e5;
	}

	.tip {
		margin-bottom: 26rpx;
		color: #94a3b8;
		font-size: 24rpx;
		line-height: 1.6;
	}

	.btn {
		width: 100%;
		height: 88rpx;
		line-height: 88rpx;
		margin: 0 0 22rpx;
		border-radius: 44rpx;
		color: #fff;
		font-size: 30rpx;
	}

	.primary {
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	}
</style>
