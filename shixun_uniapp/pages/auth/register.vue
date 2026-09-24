<template>
	<view class="container">
		<view class="header"><text class="title">用户注册</text></view>
		<!-- 表单组件 -->
		<view class="form">
			<view class="input-item">
				<text class="label">昵称</text>
				<input class="input" v-model="form.nickname" placeholder="请输入昵称" />
			</view>
			<view class="input-item">
				<text class="label">学号</text>
				<input class="input" v-model="form.username" placeholder="请输入学号" />
			</view>
			<view class="input-item">
				<text class="label">密码</text>
				<input class="input" v-model="form.password" type="password" placeholder="请输入密码" />
			</view>
			<view class="input-item">
				<text class="label">确认密码</text>
				<input class="input" v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" />
			</view>
			<view class="input-item">
				<text class="label">手机号</text>
				<input class="input" v-model="form.phone" placeholder="请输入手机号" />
			</view>

			<button class="btn btn-primary" @tap="handleRegister" :loading="loading">
				{{ loading ? '注册中...' : '注册' }}
			</button>

			<view class="footer"><text class="text" @tap="goLogin">已有账号？去登录</text></view>
		</view>
	</view>
</template>

<script>
	import {
		register
	} from '@/services/auth.js'

	export default {
		data() {
			return {
				form: {
					nickname: '',
					username: '',
					password: '',
					confirmPassword: '',
					phone: ''
				},
				loading: false
			}
		},
		methods: {
			// 注册交互处理代码函数
			async handleRegister() {
				const {
					nickname,
					username,
					password,
					confirmPassword,
					phone
				} = this.form
				if (!nickname || !username || !password || !phone) {
					uni.showToast({
						title: '请填写完整信息',
						icon: 'none'
					})
					return
				}
				if (password !== confirmPassword) {
					uni.showToast({
						title: '两次密码不一致',
						icon: 'none'
					})
					return
				}

				this.loading = true
				try {
					//调用函数发起注册请求
					const res = await register({
						nickname,
						username,
						password,
						phone
					})
					// 同登录页：request.js 只返回 data 载荷，拿不到就是失败
					if (!res) {
						uni.showToast({
							title: '注册失败',
							icon: 'none'
						})
						return
					}
					uni.showToast({
						title: '注册成功，请登录',
						icon: 'success'
					})
					setTimeout(() => uni.navigateBack(), 800)
				} finally {
					this.loading = false
				}
			},
			goLogin() {
				uni.navigateBack()
			}
		}
	}
</script>

<style scoped>
	.container {
		min-height: 100vh;
		padding: 80rpx 30rpx;
		box-sizing: border-box;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	}

	.header {
		margin-bottom: 50rpx;
		text-align: center;
	}

	.title {
		color: #fff;
		font-size: 52rpx;
		font-weight: bold;
	}

	.form {
		background: #fff;
		border-radius: 28rpx;
		padding: 40rpx 30rpx;
		box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, .16);
	}

	.input-item {
		margin-bottom: 28rpx;
	}

	.label {
		display: block;
		margin-bottom: 12rpx;
		color: #334155;
		font-size: 26rpx;
	}

	.input {
		height: 82rpx;
		padding: 0 22rpx;
		box-sizing: border-box;
		border-radius: 14rpx;
		background: #f1f5f9;
		font-size: 28rpx;
	}

	.btn {
		width: 100%;
		height: 88rpx;
		line-height: 88rpx;
		margin-top: 22rpx;
		border-radius: 44rpx;
		font-size: 30rpx;
	}

	.btn-primary {
		color: #fff;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
