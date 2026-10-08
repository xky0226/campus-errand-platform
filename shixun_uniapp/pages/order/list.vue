<template>
	<view class="page">
		<view class="tabs">
			<view :class="['tab', currentTab === 'available' ? 'active' : '']" @tap="switchTab('available')">可接订单</view>
			<view :class="['tab', currentTab === 'published' ? 'active' : '']" @tap="switchTab('published')">我发布的</view>
			<view :class="['tab', currentTab === 'running' ? 'active' : '']" @tap="switchTab('running')">我接取的</view>
		</view>

		<view v-if="!orders||orders.length === 0" class="empty">
			<!-- 讲义 (2) 新增的空状态插画 -->
			<view class="empty-illus">
				<view class="illus-card">
					<view class="illus-line w70"></view>
					<view class="illus-line w90"></view>
					<view class="illus-line w60"></view>
				</view>
			</view>
			<text class="empty-title">暂无订单</text>
			<text class="empty-desc">切换分类或稍后再来看看</text>
		</view>

		<!-- 遍历订单列表 -->
		<view v-for="item in orders" :key="item.id" class="order-card" hover-class="card-hover"
			@tap="goDetail(item.id)">
			<view class="row between">
				<view class="type-chip"><text>{{ item.taskType }}</text></view>
				<view class="reward-pill"><text>¥{{ item.reward }}</text></view>
			</view>
			<view class="addr pickup">取：{{ item.pickupLocation }}</view>
			<view class="addr delivery">送：{{ item.deliveryLocation }}</view>
			<view class="desc">{{ item.description || '暂无描述' }}</view>
			<view class="row between footer-row">
				<text class="status">{{ statusMap[item.status] }}</text>
				<view v-if="currentTab === 'available'" class="accept-btn" @tap.stop="handleAccept(item.id)">
					<text>接单</text></view>
			</view>
		</view>
	</view>
</template>

<script>
	import {
		getAvailableOrders,
		getMyPublishedOrders,
		getMyRunningOrders,
		acceptOrder
	} from '@/services/order.js'

	export default {
		data() {
			return {
				currentTab: 'available',
				orders: [],
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
		onShow() {
			// 个人中心的「我发布的 / 我接取的」快捷入口要指定切到哪个 tab，
			// 但 uni.switchTab 不支持带参数，所以那边先用缓存写好目标 tab
			const target = uni.getStorageSync('orderListTab')
			if (target) {
				uni.removeStorageSync('orderListTab')
				this.currentTab = target
			}
			this.loadOrders()
		},
		methods: {
			async switchTab(tab) {
				this.currentTab = tab
				await this.loadOrders()
			},
			async loadOrders() {
				if (this.currentTab === 'available') {
					this.orders = await getAvailableOrders() || []
				} else if (this.currentTab === 'published') {
					this.orders = await getMyPublishedOrders() || []
				} else {
					this.orders = await getMyRunningOrders() || []
				}
			},
			goDetail(id) {
				uni.navigateTo({
					url: '/pages/order/detail?id=' + id
				})
			},
			async handleAccept(id) {
				const res = await acceptOrder(id)
				if (!res) {
					uni.showToast({
						title: '接单失败',
						icon: 'none'
					})
					return
				}
				uni.showToast({
					title: '接单成功',
					icon: 'success'
				})
				await this.loadOrders()
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

	/* 分段选项卡 */
	.tabs {
		display: flex;
		gap: 10rpx;
		padding: 10rpx;
		margin-bottom: 26rpx;
		border-radius: 26rpx;
		background: #fff;
		box-shadow: 0 12rpx 32rpx rgba(30, 41, 59, .06);
	}

	.tab {
		flex: 1;
		height: 72rpx;
		line-height: 72rpx;
		border-radius: 20rpx;
		color: #94a3b8;
		font-size: 26rpx;
		text-align: center;
	}

	.tab.active {
		color: #fff;
		font-weight: 700;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		box-shadow: 0 10rpx 22rpx rgba(102, 126, 234, .32);
	}

	/* 订单卡片 */
	.order-card {
		position: relative;
		padding: 28rpx;
		margin-bottom: 24rpx;
		border-radius: 30rpx;
		background: #fff;
		box-shadow: 0 14rpx 38rpx rgba(30, 41, 59, .07);
		transition: transform .15s ease;
	}

	.card-hover {
		transform: scale(.98);
		opacity: .95;
	}

	.row {
		display: flex;
		align-items: center;
	}

	.between {
		justify-content: space-between;
	}

	.type-chip {
		padding: 8rpx 22rpx;
		border-radius: 14rpx;
		background: linear-gradient(135deg, rgba(102, 126, 234, .14), rgba(118, 75, 162, .14));
	}

	.type-chip text {
		color: #667eea;
		font-size: 28rpx;
		font-weight: 700;
	}

	.reward-pill {
		padding: 8rpx 22rpx;
		border-radius: 999rpx;
		background: linear-gradient(135deg, #fb7185, #f43f5e);
		box-shadow: 0 8rpx 18rpx rgba(244, 63, 94, .3);
	}

	.reward-pill text {
		color: #fff;
		font-size: 30rpx;
		font-weight: 800;
	}

	.addr {
		position: relative;
		margin-top: 20rpx;
		padding-left: 34rpx;
		color: #334155;
		font-size: 27rpx;
		line-height: 42rpx;
	}

	.addr::before {
		position: absolute;
		left: 0;
		top: 13rpx;
		width: 14rpx;
		height: 14rpx;
		border-radius: 50%;
		content: '';
	}

	.pickup::before {
		background: #34d399;
		box-shadow: 0 0 0 6rpx rgba(52, 211, 153, .18);
	}

	.delivery::before {
		background: #fb923c;
		box-shadow: 0 0 0 6rpx rgba(251, 146, 60, .18);
	}

	.desc {
		margin-top: 20rpx;
		padding: 18rpx 22rpx;
		border-radius: 18rpx;
		color: #64748b;
		background: #f6f8fc;
		font-size: 25rpx;
		line-height: 40rpx;
	}

	.footer-row {
		margin-top: 24rpx;
	}

	.status {
		padding: 8rpx 22rpx;
		border-radius: 999rpx;
		color: #7c6ee8;
		background: #eef0ff;
		font-size: 24rpx;
		font-weight: 600;
	}

	/* 讲义 (2) 把接单按钮从 <button> 换成了 <view>，样式跟着拆成外层 + 内层 text */
	.accept-btn {
		padding: 0 34rpx;
		height: 60rpx;
		border-radius: 999rpx;
		background: linear-gradient(135deg, #34d399, #10b981);
		box-shadow: 0 10rpx 20rpx rgba(52, 211, 153, .35);
	}

	.accept-btn text {
		color: #fff;
		font-size: 25rpx;
		font-weight: 700;
		line-height: 60rpx;
	}

	/* 空状态 */
	.empty {
		margin-top: 130rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.empty-illus {
		width: 200rpx;
		height: 200rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, #eef0ff, #e5e9ff);
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 36rpx;
	}

	.illus-card {
		width: 100rpx;
		height: 120rpx;
		border-radius: 16rpx;
		background: #fff;
		box-shadow: 0 10rpx 24rpx rgba(102, 126, 234, .18);
		padding: 22rpx 16rpx;
		display: flex;
		flex-direction: column;
		gap: 12rpx;
	}

	.illus-line {
		height: 10rpx;
		border-radius: 999rpx;
		background: #d7defc;
	}

	.w70 {
		width: 70%;
	}

	.w90 {
		width: 90%;
	}

	.w60 {
		width: 60%;
	}

	.empty-title {
		color: #334155;
		font-size: 32rpx;
		font-weight: 700;
	}

	.empty-desc {
		margin-top: 14rpx;
		color: #94a3b8;
		font-size: 26rpx;
	}
</style>
