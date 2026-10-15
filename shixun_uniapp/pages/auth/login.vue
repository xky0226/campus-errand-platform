<template>
	<view class="container">
		<view class="deco">
			<view class="circle c1"></view>
			<view class="circle c2"></view>
			<view class="circle c3"></view>
		</view>

		<view class="logo-section">
			<view class="logo"><text class="logo-text">帮</text></view>
			<text class="title">校园互助跑腿</text>
			<text class="subtitle">让你的小事有人帮</text>
		</view>

		<!-- 表单组件 -->
		<view class="form">
			<view class="input-item">
				<text class="label">学号/账号</text>
				<input class="input" v-model="form.username" placeholder="请输入你的学号" />
			</view>

			<view class="input-item">
				<text class="label">密码</text>
				<input class="input" v-model="form.password" type="password" placeholder="请输入密码" />
			</view>

			<button class="btn btn-primary" @tap="handleLogin" :loading="loading">
				{{ loading ? '登录中...' : '登录' }}
			</button>

			<button class="btn btn-primary wechat-btn" @tap="handleWxLogin" :loading="loading">
				微信一键登录
			</button>

			<view class="footer">
				<text class="text" @tap="goRegister">没有账号？去注册</text>
			</view>
		</view>
	</view>
</template>

<script>
	// 导入 请求处理的函数
	import {
		login,
		wxLogin
	} from '@/services/auth.js'

	export default {
		data() {
			return {
				form: {
					// 原来这里预填了演示账号（学号 + 口令），方便演示时一键登录。
					// 但这个仓库是公开的：预填等于把一个账号的口令写在明面上，
					// 而那个口令恰好也是本机 MySQL 的 root 口令。演示时手输一遍就行。
					username: '',
					password: ''
				},
				loading: false
			}
		},
		methods: {
			//存放user信息和token函数
			saveAuthData(res) {
				uni.setStorageSync('token', res.token)
				uni.setStorageSync('userInfo', {
					id: res.userId,
					nickname: res.nickname || '微信用户',
					avatarUrl: res.avatarUrl || '',
					role: res.role || 0,
					verifiedStatus: res.verifiedStatus || 0,
					creditScore: res.creditScore || 100
				})

				const app = getApp()
				app.globalData.token = res.token
				app.globalData.userInfo = uni.getStorageSync('userInfo')
				app.globalData.userId = res.userId
				app.globalData.role = res.role || 0
			},
			//登录交互处理函数
			async handleLogin() {
				if (!this.form.username || !this.form.password) {
					uni.showToast({
						title: '请填写完整信息',
						icon: 'none'
					})
					return
				}

				this.loading = true
				try {
					//发起请求
					const res = await login(this.form.username, this.form.password)
					// 讲义原文判的是 res.code（因为 §8 的 request.js 返回整个 Result 信封）；
					// 现在 request.js 统一只返回 data 载荷，所以拿不到数据就是失败
					if (!res) {
						uni.showToast({
							title: '登录失败，请检查账号密码',
							icon: 'none'
						})
						return
					}
					//存放user信息和token
					this.saveAuthData(res)
					//登录成功提示
					uni.showToast({
						title: '登录成功',
						icon: 'success'
					})
					//页面跳转
					setTimeout(() => {
						uni.reLaunch({
							url: '/pages/index/index'
						})
					}, 800)
				} finally {
					this.loading = false
				}
			},
			handleWxLogin() {
				this.loading = true
				uni.login({
					success: async res => {
						try {
							if (!res.code) {
								uni.showToast({
									title: '获取登录凭证失败',
									icon: 'none'
								})
								return
							}
							const data = await wxLogin(res.code, '微信用户', '')
							if (!data) {
								uni.showToast({
									title: '微信登录失败',
									icon: 'none'
								})
								return
							}
							this.saveAuthData(data)
							uni.showToast({
								title: '登录成功',
								icon: 'success'
							})
							setTimeout(() => {
								uni.reLaunch({
									url: '/pages/index/index'
								})
							}, 800)
						} finally {
							this.loading = false
						}
					},
					fail: () => {
						this.loading = false
						uni.showToast({
							title: '微信登录失败',
							icon: 'none'
						})
					}
				})
			},
			goRegister() {
				uni.navigateTo({
					url: '/pages/auth/register'
				})
			}
		}
	}
</script>

<style scoped>
	.container {
		position: relative;
		min-height: 100vh;
		padding: 0 40rpx;
		display: flex;
		flex-direction: column;
		justify-content: center;
		background: linear-gradient(160deg, #667eea 0%, #764ba2 100%);
	}

	/* 背景装饰圆斑 */
	.deco {
		position: absolute;
		inset: 0;
		overflow: hidden;
		pointer-events: none;
	}

	.circle {
		position: absolute;
		border-radius: 50%;
		background: rgba(255, 255, 255, .12);
	}

	.c1 {
		width: 340rpx;
		height: 340rpx;
		top: -130rpx;
		right: -100rpx;
	}

	.c2 {
		width: 220rpx;
		height: 220rpx;
		bottom: 120rpx;
		left: -110rpx;
	}

	.c3 {
		width: 130rpx;
		height: 130rpx;
		top: 180rpx;
		left: 60rpx;
		background: rgba(255, 255, 255, .08);
	}

	.logo-section {
		position: relative;
		text-align: center;
		color: #fff;
		margin-bottom: 56rpx;
	}

	.logo {
		width: 150rpx;
		height: 150rpx;
		margin: 0 auto 32rpx;
		border-radius: 44rpx;
		background: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 18rpx 40rpx rgba(60, 46, 130, .35);
	}

	.logo-text {
		font-size: 70rpx;
		font-weight: bold;
		color: #667eea;
	}

	.title {
		display: block;
		font-size: 52rpx;
		font-weight: bold;
		letter-spacing: 2rpx;
	}

	.subtitle {
		display: block;
		margin-top: 14rpx;
		font-size: 27rpx;
		opacity: .9;
	}

	.form {
		position: relative;
		background: #fff;
		border-radius: 36rpx;
		padding: 48rpx 36rpx 40rpx;
		box-shadow: 0 30rpx 60rpx rgba(49, 38, 105, .3);
	}

	.input-item {
		margin-bottom: 30rpx;
	}

	.label {
		display: block;
		font-size: 26rpx;
		color: #475569;
		font-weight: 600;
		margin-bottom: 14rpx;
	}

	.input {
		height: 88rpx;
		border-radius: 20rpx;
		background: #f4f6fb;
		padding: 0 26rpx;
		font-size: 28rpx;
		color: #1e293b;
	}

	.btn {
		width: 100%;
		height: 92rpx;
		line-height: 92rpx;
		border-radius: 46rpx;
		font-size: 30rpx;
		margin-top: 24rpx;
		font-weight: 600;
	}

	.btn:active {
		opacity: .9;
	}

	.btn-primary {
		color: #fff;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		box-shadow: 0 16rpx 30rpx rgba(102, 126, 234, .35);
	}

	.wechat-btn {
		background: linear-gradient(135deg, #34d399, #10b981);
		box-shadow: 0 16rpx 30rpx rgba(52, 211, 153, .32);
	}

	.footer {
		margin-top: 36rpx;
		text-align: center;
	}

	.text {
		color: #667eea;
		font-size: 26rpx;
	}
</style>
