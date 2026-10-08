<template>
	<view class="page">
		<!-- 讲义 (2) 新增的顶部渐变横幅 -->
		<view class="banner">
			<view class="banner-blob"></view>
			<text class="banner-title">发布你的任务</text>
			<text class="banner-desc">写清楚取送地点和要求，同学接单更迅速</text>
		</view>

		<view class="form-card">
			<view class="form-item">
				<text class="label">任务类型</text>
				<picker :range="taskTypes" @change="changeTaskType">
					<view class="picker-value">
						<text>{{ form.taskType }}</text>
						<text class="picker-arrow">▾</text>
					</view>
				</picker>
			</view>

			<view class="form-item">
				<text class="label">取件地点</text>
				<input class="input" v-model="form.pickupLocation" placeholder="请输入取件地点" />
			</view>

			<view class="form-item">
				<text class="label">送达地点</text>
				<input class="input" v-model="form.deliveryLocation" placeholder="请输入送达地点" />
			</view>

			<view class="form-item">
				<text class="label">任务描述</text>
				<textarea class="textarea" v-model="form.description" placeholder="请填写任务要求、物品信息等" />
			</view>

			<view class="form-item">
				<text class="label">预期完成时间</text>
				<input class="input" v-model="form.expectedTime" placeholder="例：2026-06-15 18:00:00" />
			</view>

			<view class="form-item">
				<text class="label">悬赏金额</text>
				<view class="reward-box">
					<text class="reward-unit">¥</text>
					<input class="reward-input" v-model="form.reward" type="digit" placeholder="请输入悬赏金额" />
				</view>
			</view>

			<button class="submit-btn" :loading="submitting" @tap="submit">
				{{ submitting ? '发布中...' : '立即发布订单' }}
			</button>
		</view>
	</view>
</template>

<script>
	import {
		createOrder
	} from '@/services/order.js'

	export default {
		data() {
			return {
				submitting: false,
				form: {
					taskType: '代取快递',
					pickupLocation: '',
					deliveryLocation: '',
					description: '',
					expectedTime: '',
					reward: 8,
					userId: '0'
				},
				taskTypes: ['代取快递', '代购', '代办', '送物', '其他']
			}
		},
		onLoad() {
			const token = uni.getStorageSync('token')
			if (!token) {
				uni.showModal({
					title: '需要登录',
					content: '请先登录后再发布订单',
					success: () => uni.navigateTo({
						url: '/pages/auth/login'
					})
				})
			}
			// 讲义原文直接写 uni.getStorageSync('userInfo').id，
			// 未登录时 userInfo 是空对象，取 .id 会抛 TypeError，这里兜一下
			const userInfo = uni.getStorageSync('userInfo') || {}
			this.form.userId = userInfo.id || 0
			this.initDefaultTime()
		},
		methods: {
			changeTaskType(e) {
				this.form.taskType = this.taskTypes[e.detail.value]
			},
			initDefaultTime() {
				const date = new Date(Date.now() + 2 * 60 * 60 * 1000)
				this.form.expectedTime = this.formatDateTime(date)
			},
			formatDateTime(date) {
				const y = date.getFullYear()
				const m = String(date.getMonth() + 1).padStart(2, '0')
				const d = String(date.getDate()).padStart(2, '0')
				const h = String(date.getHours()).padStart(2, '0')
				const min = String(date.getMinutes()).padStart(2, '0')
				const s = String(date.getSeconds()).padStart(2, '0')
				return `${y}-${m}-${d} ${h}:${min}:${s}`
			},
			async submit() {
				const userInfo = uni.getStorageSync('userInfo') || {}
				if (userInfo.creditScore < 30) {
					uni.showToast({
						title: '信用分过低，无法发布订单',
						icon: 'none'
					})
					return
				}
				if (!this.form.pickupLocation || !this.form.deliveryLocation) {
					uni.showToast({
						title: '请填写地点信息',
						icon: 'none'
					})
					return
				}
				if (!this.form.expectedTime) {
					uni.showToast({
						title: '请选择预期完成时间',
						icon: 'none'
					})
					return
				}
				if (!this.form.reward || Number(this.form.reward) < 2) {
					uni.showToast({
						title: '悬赏金额不能低于2元',
						icon: 'none'
					})
					return
				}

				this.form.userId = userInfo.id || 0

				this.submitting = true
				try {
					const data = {
						taskType: this.form.taskType,
						pickupLocation: this.form.pickupLocation,
						deliveryLocation: this.form.deliveryLocation,
						description: this.form.description || '',
						imageUrls: '',
						expectedTime: this.form.expectedTime,
						userId: this.form.userId,
						reward: Number(this.form.reward)
					}
					//调用函数发起请求
					const res = await createOrder(data)
					// 讲义原文没判返回值，发失败也会弹「发布成功」然后返回上一页，
					// 这里补上判断
					if (!res) {
						uni.showToast({
							title: '发布失败',
							icon: 'none'
						})
						return
					}
					uni.showToast({
						title: '发布成功',
						icon: 'success'
					})
					setTimeout(() => uni.navigateBack(), 800)
				} finally {
					this.submitting = false
				}
			}
		}
	}
</script>

<style scoped>
	.page {
		min-height: 100vh;
		padding: 28rpx 28rpx 60rpx;
		box-sizing: border-box;
		background: linear-gradient(180deg, #eef0ff 0%, #f3f4fa 280rpx);
	}

	/* 顶部渐变横幅 */
	.banner {
		position: relative;
		overflow: hidden;
		padding: 40rpx 34rpx;
		border-radius: 32rpx;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		box-shadow: 0 22rpx 46rpx rgba(102, 126, 234, .32);
	}

	.banner-blob {
		position: absolute;
		width: 230rpx;
		height: 230rpx;
		border-radius: 50%;
		right: -80rpx;
		top: -100rpx;
		background: rgba(255, 255, 255, .14);
	}

	.banner-title {
		position: relative;
		display: block;
		color: #fff;
		font-size: 40rpx;
		font-weight: 800;
	}

	.banner-desc {
		position: relative;
		display: block;
		margin-top: 12rpx;
		color: rgba(255, 255, 255, .9);
		font-size: 25rpx;
	}

	.form-card {
		margin-top: 28rpx;
		padding: 38rpx 30rpx 34rpx;
		border-radius: 32rpx;
		background: #fff;
		box-shadow: 0 16rpx 42rpx rgba(30, 41, 59, .07);
	}

	.form-item {
		margin-bottom: 30rpx;
	}

	.label {
		display: block;
		margin-bottom: 14rpx;
		color: #475569;
		font-weight: 600;
		font-size: 26rpx;
	}

	.input,
	.picker-value {
		min-height: 88rpx;
		padding: 0 26rpx;
		box-sizing: border-box;
		border-radius: 20rpx;
		background: #f4f6fb;
		color: #1e293b;
		font-size: 28rpx;
		line-height: 88rpx;
	}

	.picker-value {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.picker-arrow {
		color: #94a3b8;
		font-size: 30rpx;
	}

	.textarea {
		width: 100%;
		min-height: 180rpx;
		padding: 22rpx 26rpx;
		box-sizing: border-box;
		border-radius: 20rpx;
		background: #f4f6fb;
		font-size: 28rpx;
		color: #1e293b;
		line-height: 44rpx;
	}

	/* 悬赏金额 */
	.reward-box {
		display: flex;
		align-items: center;
		height: 88rpx;
		padding: 0 26rpx;
		border-radius: 20rpx;
		background: #fff5f5;
	}

	.reward-unit {
		margin-right: 12rpx;
		color: #f43f5e;
		font-size: 36rpx;
		font-weight: 800;
	}

	.reward-input {
		flex: 1;
		color: #f43f5e;
		font-size: 32rpx;
		font-weight: 700;
	}

	.submit-btn {
		width: 100%;
		height: 94rpx;
		line-height: 94rpx;
		border-radius: 47rpx;
		font-size: 31rpx;
		font-weight: 700;
		color: #fff;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		box-shadow: 0 18rpx 34rpx rgba(102, 126, 234, .38);
		/* 讲义这里去掉了 type="primary"，小程序 button 默认那圈描边要自己清掉，
		   否则渐变胶囊外面会多一道灰边 */
		border: none;
	}

	.submit-btn::after {
		border: none;
	}

	.submit-btn:active {
		opacity: .9;
	}
</style>
