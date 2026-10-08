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
				<button v-if="canPay" class="action-btn primary" :loading="actionLoading" @tap="handlePay">支付订单</button>
				<button v-if="canAccept" class="action-btn success" :loading="actionLoading" @tap="handleAccept">接单</button>
				<button v-if="canDeliver" class="action-btn primary" :loading="actionLoading" @tap="handleDeliver">确认送达</button>
				<button v-if="canConfirm" class="action-btn primary" :loading="actionLoading" @tap="handleConfirm">确认完成</button>
				<button v-if="canCancel" class="action-btn danger" :loading="actionLoading" @tap="handleCancel">取消订单</button>
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

	export default {
		data() {
			return {
				id: '',
				order: null,
				loading: false,
				actionLoading: false,
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
			hasActions() {
				return this.canPay || this.canAccept || this.canDeliver || this.canConfirm || this.canCancel
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
				} finally {
					this.loading = false
				}
			},
			async runAction(action, successTitle) {
				if (this.actionLoading) return
				this.actionLoading = true
				try {
					const res = await action()
					if (!res) {
						uni.showToast({
							title: '操作失败',
							icon: 'none'
						})
						return
					}
					uni.showToast({
						title: successTitle,
						icon: 'success'
					})
					await this.loadDetail()
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
</style>
