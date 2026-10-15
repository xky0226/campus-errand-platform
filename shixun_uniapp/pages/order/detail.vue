<template>
	<view class="page">
		<view v-if="loading" class="loading">加载中...</view>

		<view v-else-if="!order" class="empty">
			<text class="empty-title">订单不存在</text>
			<text class="empty-desc">请返回订单列表重新查看</text>
		</view>

		<view v-else>
			<view class="header-card">
				<!-- 讲义 (2) 新增的两个装饰圆 -->
				<view class="hblob hb1"></view>
				<view class="hblob hb2"></view>
				<view class="row between">
					<text class="task-type">{{ order.taskType || '订单详情' }}</text>
					<text class="status">{{ statusText }}</text>
				</view>
				<view class="reward">¥{{ order.reward }}</view>
				<text class="order-no">订单号：{{ order.orderNo || '-' }}</text>
			</view>

			<view class="section-card">
				<view class="section-title">配送信息</view>
				<!-- 讲义 (2) 把两个地址拆成彩色地址块：取件绿底、送达橙底 -->
				<view class="address-box pickup-box">
					<view class="address-item">
						<text class="address-label">取件地点</text>
						<text class="address-text">{{ order.pickupLocation || '-' }}</text>
					</view>
				</view>
				<view class="address-box delivery-box">
					<view class="address-item">
						<text class="address-label">送达地点</text>
						<text class="address-text">{{ order.deliveryLocation || '-' }}</text>
					</view>
				</view>
				<view class="info-row">
					<text class="info-label">预期完成</text>
					<text class="info-value">{{ order.expectedTime || '-' }}</text>
				</view>
			</view>

			<view class="section-card">
				<view class="section-title">任务描述</view>
				<text class="description">{{ order.description || '暂无描述' }}</text>
			</view>

			<view class="section-card">
				<view class="section-title">订单信息</view>
				<view class="info-row">
					<text class="info-label">发布者</text>
					<text class="info-value">{{ order.publisherName || order.publisherId || '-' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">接单人</text>
					<text class="info-value">{{ order.runnerName || order.runnerId || '暂未接单' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">支付状态</text>
					<text class="info-value">{{ order.paymentStatus === 1 ? '已支付' : '未支付' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">创建时间</text>
					<text class="info-value">{{ order.createdAt || '-' }}</text>
				</view>
			</view>

			<!-- 订单相关的操作 -->
			<view class="action-card" v-if="hasActions">
				<!-- 待支付时先亮一下钱包够不够，省得点了才发现余额不足 -->
				<view v-if="canPay && walletLoaded && insufficient" class="pay-warn">
					<text class="pay-warn-text">
						钱包余额 {{ money(walletBalance) }} 元，不足以支付本单 {{ money(order.reward) }} 元
					</text>
					<text class="pay-warn-link" @tap="goWallet">去充值</text>
				</view>
				<button v-if="canPay" class="action-btn primary" :loading="actionLoading" @tap="handlePay">
					钱包支付 ¥{{ money(order.reward) }}
				</button>
				<button v-if="canAccept" class="action-btn success" :loading="actionLoading" @tap="handleAccept">接单</button>
				<button v-if="canDeliver" class="action-btn primary" :loading="actionLoading" @tap="handleDeliver">确认送达</button>
				<button v-if="canConfirm" class="action-btn primary" :loading="actionLoading" @tap="handleConfirm">确认完成</button>
				<button v-if="canCancel" class="action-btn danger" :loading="actionLoading" @tap="handleCancel">取消订单</button>
				<button v-if="canAppeal" class="action-btn warn" :loading="actionLoading" @tap="openAppeal">申请仲裁</button>
				<button v-if="canChat" class="action-btn chat" @tap="goChat">联系对方</button>
			</view>
		</view>

		<!-- 申请仲裁的弹层：写理由 + 传证据图，提交后订单会变成「申诉中」 -->
		<view v-if="showAppeal" class="mask" @tap="closeAppeal">
			<view class="sheet" @tap.stop>
				<text class="sheet-title">申请仲裁</text>
				<text class="sheet-sub">订单号：{{ order.orderNo || '-' }}</text>
				<textarea class="textarea" v-model="appealForm.reason" maxlength="500"
					placeholder="请把遇到的问题写清楚，至少 5 个字" />
				<view class="evidence-row">
					<view v-for="(url, index) in appealForm.evidenceUrls" :key="url" class="evidence-item">
						<image class="evidence-img" :src="fileUrl(url)" mode="aspectFill" />
						<text class="evidence-del" @tap="removeEvidence(index)">×</text>
					</view>
					<view v-if="appealForm.evidenceUrls.length < 3" class="evidence-add" @tap="pickEvidence">
						<text class="evidence-add-text">＋</text>
						<text class="evidence-add-tip">证据图</text>
					</view>
				</view>
				<view class="tip">最多 3 张。提交后订单会进入「申诉中」，等管理员核实处理</view>
				<button class="btn primary" :loading="actionLoading" @tap="submitAppealForm">
					{{ actionLoading ? '提交中...' : '提交申诉' }}
				</button>
			</view>
		</view>
	</view>
</template>

<script>
	import {
		getOrderDetail,
		payOrder,
		acceptOrder,
		deliverOrder,
		confirmOrder,
		cancelOrder
	} from '@/services/order.js'
	import {
		getBalance
	} from '@/services/wallet.js'
	import {
		submitAppeal
	} from '@/services/appeal.js'
	import {
		chooseAndUpload
	} from '@/services/upload.js'
	// 证据图存的是 /files/... 相对路径，预览时要补上后端地址
	import {
		BASE_URL
	} from '@/utils/request.js'

	export default {
		data() {
			return {
				id: '',
				order: null,
				loading: false,
				actionLoading: false,
				// 发布者待支付时用来提示余额够不够
				walletBalance: 0,
				walletLoaded: false,
				// 申请仲裁的弹层
				showAppeal: false,
				appealForm: {
					reason: '',
					evidenceUrls: []
				},
				statusMap: {
					0: '待支付',
					1: '待接单',
					2: '进行中',
					3: '待确认',
					4: '已完成',
					5: '已取消',
					6: '申诉中'
				}
			}
		},
		computed: {
			currentUserId() {
				const userInfo = uni.getStorageSync('userInfo') || {}
				return userInfo.id
			},
			statusText() {
				return this.statusMap[this.order.status] || '未知状态'
			},
			isPublisher() {
				return this.order && this.currentUserId === this.order.publisherId
			},
			isRunner() {
				return this.order && this.currentUserId === this.order.runnerId
			},
			canPay() {
				return this.isPublisher && this.order.status === 0
			},
			canAccept() {
				return this.order.status === 1 && !this.isPublisher
			},
			canDeliver() {
				return this.isRunner && this.order.status === 2
			},
			canConfirm() {
				return this.isPublisher && this.order.status === 3
			},
			canCancel() {
				return (this.isPublisher || this.isRunner) && this.order.status !== 4 && this.order.status !== 5
			},
			// 只有订单当事人、且订单处于「进行中 / 待确认 / 已完成」时才能申请仲裁。
			// 待支付、待接单还没人干活，没有可申诉的对象；已取消、申诉中也不能重复提交。
			// 口径和后端 AppealsServiceImpl.APPEALABLE_STATUS 保持一致
			canAppeal() {
				if (!this.order) return false
				if (!this.isPublisher && !this.isRunner) return false
				return [2, 3, 4].indexOf(this.order.status) >= 0
			},
			// 聊天入口：得先是订单当事人，而且订单已经有人接单（没人接单就没有对话对象）。
			// 后端 MessagesServiceImpl 的规则一样：不按订单状态卡，
			// 已完成 / 申诉中的单也该能继续沟通（申诉那单尤其需要，管理员要看聊天记录）
			canChat() {
				if (!this.order) return false
				if (!this.isPublisher && !this.isRunner) return false
				return !!this.order.runnerId
			},
			// 钱包余额够不够付这一单。余额还没取回来时不算，免得闪一下「不足」
			insufficient() {
				if (!this.walletLoaded || !this.order) return false
				return Number(this.walletBalance) < Number(this.order.reward || 0)
			},
			hasActions() {
				return this.canPay || this.canAccept || this.canDeliver || this.canConfirm
					|| this.canCancel || this.canAppeal || this.canChat
			}
		},
		onLoad(options) {
			this.id = options.id
			this.loadDetail()
		},
		onPullDownRefresh() {
			this.loadDetail().finally(() => uni.stopPullDownRefresh())
		},
		methods: {
			async loadDetail() {
				if (!this.id) return
				this.loading = true
				try {
					this.order = await getOrderDetail(this.id)
					// 详情拿到之后再取钱包余额，够不够付这一单一目了然
					if (this.order && this.order.status === 0) {
						await this.loadWallet()
					}
				} finally {
					this.loading = false
				}
			},
			async loadWallet() {
				const res = await getBalance()
				// 拿不到就当作「未知」，不显示余额不足的提示，别误报
				this.walletLoaded = !!res
				if (res) {
					this.walletBalance = Number(res.balance || 0)
				}
			},
			money(value) {
				if (value == null) return '0.00'
				return Number(value).toFixed(2)
			},
			goWallet() {
				uni.navigateTo({
					url: '/pages/wallet/wallet'
				})
			},
			goChat() {
				uni.navigateTo({
					url: '/pages/order/chat?orderId=' + this.id
				})
			},
			async runAction(action, successTitle) {
				if (this.actionLoading) return false
				this.actionLoading = true
				try {
					const res = await action()
					if (!res) {
						// 失败原因（比如余额不足）后端写在 message 里，
						// utils/request.js 已经统一弹过 toast 了，这里不再重复提示
						return false
					}
					uni.showToast({
						title: successTitle,
						icon: 'success'
					})
					await this.loadDetail()
					return true
				} finally {
					this.actionLoading = false
				}
			},
			handlePay() {
				this.runAction(() => payOrder(this.id), '支付成功')
			},
			handleAccept() {
				this.runAction(() => acceptOrder(this.id), '接单成功')
			},
			handleDeliver() {
				this.runAction(() => deliverOrder(this.id), '已确认送达')
			},
			handleConfirm() {
				this.runAction(() => confirmOrder(this.id), '订单已完成')
			},
			handleCancel() {
				uni.showModal({
					title: '取消订单',
					content: '确定要取消该订单吗？',
					success: res => {
						if (res.confirm) {
							this.runAction(() => cancelOrder(this.id), '订单已取消')
						}
					}
				})
			},
			// ============ 申请仲裁 ============
			openAppeal() {
				this.appealForm = {
					reason: '',
					evidenceUrls: []
				}
				this.showAppeal = true
			},
			closeAppeal() {
				this.showAppeal = false
			},
			// 后端存的是 /files/yyyy/MM/dd/xxx.png 这种相对路径，预览时要补上后端地址
			fileUrl(url) {
				if (!url) return ''
				return url.indexOf('http') === 0 ? url : BASE_URL + url
			},
			async pickEvidence() {
				const left = 3 - this.appealForm.evidenceUrls.length
				if (left <= 0) return
				const urls = await chooseAndUpload(left)
				if (urls && urls.length) {
					this.appealForm.evidenceUrls = this.appealForm.evidenceUrls.concat(urls)
				}
			},
			removeEvidence(index) {
				this.appealForm.evidenceUrls.splice(index, 1)
			},
			async submitAppealForm() {
				const reason = (this.appealForm.reason || '').trim()
				if (reason.length < 5) {
					uni.showToast({
						title: '请把申诉理由写清楚（至少 5 个字）',
						icon: 'none'
					})
					return
				}
				const done = await this.runAction(
					() => submitAppeal(this.id, reason, this.appealForm.evidenceUrls),
					'申诉已提交')
				// 失败时保留弹层，理由和图片都还在，用户改一改能直接重提
				if (done) {
					this.showAppeal = false
				}
			}
		}
	}
</script>

<style scoped>
	.page {
		min-height: 100vh;
		padding: 24rpx 24rpx 60rpx;
		box-sizing: border-box;
		background: linear-gradient(180deg, #eef0ff 0%, #f3f4fa 240rpx);
	}

	.loading,
	.empty {
		margin-top: 220rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		color: #94a3b8;
		font-size: 28rpx;
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

	.header-card,
	.section-card,
	.action-card {
		position: relative;
		margin-bottom: 24rpx;
		padding: 32rpx;
		border-radius: 30rpx;
		background: #fff;
		box-shadow: 0 14rpx 38rpx rgba(30, 41, 59, .07);
	}

	/* 渐变头部 */
	.header-card {
		overflow: hidden;
		color: #fff;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		box-shadow: 0 22rpx 46rpx rgba(102, 126, 234, .32);
	}

	.hblob {
		position: absolute;
		border-radius: 50%;
		background: rgba(255, 255, 255, .13);
	}

	.hb1 {
		width: 260rpx;
		height: 260rpx;
		right: -90rpx;
		top: -110rpx;
	}

	.hb2 {
		width: 150rpx;
		height: 150rpx;
		left: -60rpx;
		bottom: -80rpx;
	}

	.row {
		position: relative;
		display: flex;
		align-items: center;
	}

	.between {
		justify-content: space-between;
	}

	.task-type {
		max-width: 430rpx;
		font-size: 38rpx;
		font-weight: 800;
	}

	.status {
		padding: 8rpx 22rpx;
		border-radius: 999rpx;
		color: #667eea;
		background: rgba(255, 255, 255, .95);
		font-size: 24rpx;
		font-weight: 600;
	}

	.reward {
		position: relative;
		margin-top: 32rpx;
		font-size: 60rpx;
		font-weight: 800;
	}

	.order-no {
		position: relative;
		display: block;
		margin-top: 16rpx;
		opacity: .85;
		font-size: 24rpx;
	}

	/* 分区标题：渐变竖条 */
	.section-title {
		margin-bottom: 26rpx;
		padding-left: 20rpx;
		color: #0f172a;
		font-size: 32rpx;
		font-weight: 800;
		position: relative;
	}

	.section-title::before {
		position: absolute;
		left: 0;
		top: 6rpx;
		width: 8rpx;
		height: 32rpx;
		border-radius: 999rpx;
		background: linear-gradient(180deg, #667eea, #764ba2);
		content: '';
	}

	/* 彩色地址块 */
	.address-box {
		border-radius: 20rpx;
		padding: 24rpx 26rpx;
		margin-bottom: 20rpx;
	}

	.pickup-box {
		background: #ecfdf5;
	}

	.delivery-box {
		background: #fff7ed;
	}

	.address-item {
		position: relative;
		padding-left: 30rpx;
	}

	.address-item::before {
		position: absolute;
		left: 0;
		top: 8rpx;
		width: 14rpx;
		height: 14rpx;
		border-radius: 50%;
		content: '';
	}

	.pickup-box .address-item::before {
		background: #10b981;
	}

	.delivery-box .address-item::before {
		background: #f97316;
	}

	.address-label {
		display: block;
		margin-bottom: 8rpx;
		color: #94a3b8;
		font-size: 24rpx;
	}

	.address-text {
		color: #334155;
		font-size: 30rpx;
		line-height: 44rpx;
	}

	.description {
		display: block;
		padding: 24rpx;
		border-radius: 20rpx;
		color: #475569;
		background: #f6f8fc;
		font-size: 28rpx;
		line-height: 46rpx;
	}

	.info-row {
		display: flex;
		justify-content: space-between;
		gap: 30rpx;
		padding: 22rpx 0;
		border-bottom: 1rpx solid #f1f5f9;
	}

	.info-row:last-child {
		border-bottom: 0;
	}

	.info-label {
		flex-shrink: 0;
		color: #94a3b8;
		font-size: 26rpx;
	}

	.info-value {
		color: #334155;
		font-size: 26rpx;
		text-align: right;
		word-break: break-all;
	}

	/* 操作按钮 */
	.action-card {
		display: flex;
		flex-direction: column;
		gap: 20rpx;
		box-shadow: none;
		padding: 0;
		background: transparent;
	}

	.action-btn {
		width: 100%;
		height: 92rpx;
		line-height: 92rpx;
		margin: 0;
		border-radius: 46rpx;
		color: #fff;
		font-size: 30rpx;
		font-weight: 700;
	}

	.pay-warn {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 20rpx;
		padding: 22rpx 26rpx;
		border-radius: 18rpx;
		background: #fef3c7;
	}

	.pay-warn-text {
		flex: 1;
		color: #92400e;
		font-size: 24rpx;
		line-height: 1.5;
	}

	.pay-warn-link {
		flex-shrink: 0;
		padding: 8rpx 24rpx;
		border-radius: 999rpx;
		color: #fff;
		background: #f59e0b;
		font-size: 24rpx;
	}

	.primary {
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		box-shadow: 0 16rpx 30rpx rgba(102, 126, 234, .35);
	}

	.success {
		background: linear-gradient(135deg, #34d399, #10b981);
		box-shadow: 0 16rpx 30rpx rgba(52, 211, 153, .32);
	}

	.danger {
		background: linear-gradient(135deg, #fb7185, #f43f5e);
		box-shadow: 0 16rpx 30rpx rgba(244, 63, 94, .3);
	}

	.warn {
		background: linear-gradient(135deg, #fbbf24, #f59e0b);
		box-shadow: 0 16rpx 30rpx rgba(245, 158, 11, .3);
	}

	/* 「联系对方」用浅色描边，和上面几个实心动作按钮区分开：
	   它是随时能点的沟通入口，不像支付/接单那样是个阶段性的主操作 */
	.chat {
		background: #fff;
		color: #6366f1;
		border: 2rpx solid #c7d2fe;
		box-shadow: 0 10rpx 24rpx rgba(99, 102, 241, .12);
	}

	/* ============ 申请仲裁弹层（样式跟 pages/wallet/wallet.vue 的弹层保持一致）============ */

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
		color: #0f172a;
		font-size: 34rpx;
		font-weight: 700;
	}

	.sheet-sub {
		display: block;
		margin: 10rpx 0 26rpx;
		color: #94a3b8;
		font-size: 24rpx;
	}

	.textarea {
		width: 100%;
		height: 200rpx;
		padding: 22rpx;
		box-sizing: border-box;
		border-radius: 14rpx;
		background: #f1f5f9;
		color: #334155;
		font-size: 28rpx;
	}

	.evidence-row {
		display: flex;
		flex-wrap: wrap;
		gap: 20rpx;
		margin: 24rpx 0;
	}

	.evidence-item {
		position: relative;
		width: 160rpx;
		height: 160rpx;
	}

	.evidence-img {
		width: 160rpx;
		height: 160rpx;
		border-radius: 14rpx;
	}

	.evidence-del {
		position: absolute;
		right: -10rpx;
		top: -10rpx;
		width: 40rpx;
		height: 40rpx;
		line-height: 36rpx;
		border-radius: 50%;
		background: rgba(15, 23, 42, .7);
		color: #fff;
		font-size: 28rpx;
		text-align: center;
	}

	.evidence-add {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		width: 160rpx;
		height: 160rpx;
		border-radius: 14rpx;
		border: 2rpx dashed #cbd5e1;
		background: #f8fafc;
	}

	.evidence-add-text {
		color: #94a3b8;
		font-size: 40rpx;
		line-height: 1;
	}

	.evidence-add-tip {
		margin-top: 8rpx;
		color: #94a3b8;
		font-size: 22rpx;
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
		margin: 0;
		border-radius: 44rpx;
		color: #fff;
		font-size: 30rpx;
		font-weight: 700;
	}
</style>
