<template>
	<view class="page">
		<view class="hero">
			<view class="blob blob-1"></view>
			<view class="blob blob-2"></view>
			<view class="hero-inner">
				<image class="logo" src="/static/logo.png"></image>
				<text class="brand">{{ title }} · 校园帮</text>
				<text class="slogan">校园互助跑腿，让你的小事有人帮</text>
			</view>
		</view>

		<view class="entry-list">
			<view class="entry-card" hover-class="entry-hover" @click="goToCreate">
				<view class="entry-icon icon-create"><text>发</text></view>
				<view class="entry-text">
					<text class="entry-title">发布订单</text>
					<text class="entry-desc">填个任务，挂个悬赏，马上有人接单</text>
				</view>
				<text class="chevron">›</text>
			</view>

			<view class="entry-card" hover-class="entry-hover" @click="goToLogin">
				<view class="entry-icon icon-login"><text>登</text></view>
				<view class="entry-text">
					<text class="entry-title">立即登录</text>
					<text class="entry-desc">登录后发布、接取和管理你的订单</text>
				</view>
				<text class="chevron">›</text>
			</view>

			<view class="entry-card" hover-class="entry-hover" @click="goToRegister">
				<view class="entry-icon icon-register"><text>注</text></view>
				<view class="entry-text">
					<text class="entry-title">注册账号</text>
					<text class="entry-desc">新同学？用学号 30 秒完成注册</text>
				</view>
				<text class="chevron">›</text>
			</view>

			<view class="entry-card" hover-class="entry-hover" @click="goToAi">
				<view class="entry-icon icon-ai"><text>AI</text></view>
				<view class="entry-text">
					<text class="entry-title">AI 助手</text>
					<text class="entry-desc">流式对话，回答一个字一个字往外蹦</text>
				</view>
				<text class="chevron">›</text>
			</view>
		</view>

		<view class="tip-card">
			<view class="tip-dot"></view>
			<text class="tip-text">诚信互助，快乐校园，从发布第一个任务开始～</text>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				// 讲义原文是模板默认的 'Hello'，首页大标题会渲染成「Hello · 校园帮」
				title: '校园互助'
			}
		},
		onLoad() {

		},
		methods: {
			//订单发布界面跳转
			goToCreate() {
				uni.navigateTo({
					url: '/pages/order/create'
				})
			},
			//登录界面跳转
			goToLogin() {
				uni.navigateTo({
					url: '/pages/auth/login'
				})
			},
			//注册界面跳转
			goToRegister() {
				uni.navigateTo({
					url: '/pages/auth/register'
				})
			},
			//AI 会话界面跳转
			goToAi() {
				uni.navigateTo({
					url: '/pages/ai/chat'
				})
			}
		}
	}
</script>

<style scoped>
	.page {
		min-height: 100vh;
		padding: 30rpx 28rpx 60rpx;
		background: linear-gradient(180deg, #eef0ff 0%, #f3f4fa 320rpx);
	}

	/* 顶部渐变横幅 */
	.hero {
		position: relative;
		overflow: hidden;
		border-radius: 36rpx;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		box-shadow: 0 24rpx 50rpx rgba(102, 126, 234, .35);
	}

	.blob {
		position: absolute;
		border-radius: 50%;
		background: rgba(255, 255, 255, .14);
	}

	.blob-1 {
		width: 280rpx;
		height: 280rpx;
		right: -90rpx;
		top: -110rpx;
	}

	.blob-2 {
		width: 190rpx;
		height: 190rpx;
		left: -70rpx;
		bottom: -90rpx;
		background: rgba(255, 255, 255, .1);
	}

	.hero-inner {
		position: relative;
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 60rpx 30rpx 54rpx;
	}

	.logo {
		width: 150rpx;
		height: 150rpx;
		padding: 22rpx;
		border-radius: 40rpx;
		background: #fff;
		box-shadow: 0 14rpx 30rpx rgba(60, 46, 130, .3);
	}

	.brand {
		margin-top: 28rpx;
		color: #fff;
		font-size: 40rpx;
		font-weight: 800;
		letter-spacing: 2rpx;
	}

	.slogan {
		margin-top: 14rpx;
		color: rgba(255, 255, 255, .9);
		font-size: 26rpx;
	}

	/* 入口卡片 */
	.entry-list {
		margin-top: 34rpx;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.entry-card {
		display: flex;
		align-items: center;
		padding: 30rpx 28rpx;
		border-radius: 30rpx;
		background: #fff;
		box-shadow: 0 14rpx 40rpx rgba(30, 41, 59, .07);
		transition: transform .15s ease;
	}

	.entry-hover {
		transform: scale(.97);
		opacity: .92;
	}

	.entry-icon {
		width: 96rpx;
		height: 96rpx;
		border-radius: 28rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.entry-icon text {
		color: #fff;
		font-size: 40rpx;
		font-weight: 700;
	}

	.icon-create {
		background: linear-gradient(135deg, #667eea, #764ba2);
		box-shadow: 0 12rpx 22rpx rgba(102, 126, 234, .35);
	}

	.icon-login {
		background: linear-gradient(135deg, #38bdf8, #3b82f6);
		box-shadow: 0 12rpx 22rpx rgba(56, 189, 248, .35);
	}

	.icon-register {
		background: linear-gradient(135deg, #fbbf24, #fb923c);
		box-shadow: 0 12rpx 22rpx rgba(251, 191, 36, .35);
	}

	.icon-ai {
		background: linear-gradient(135deg, #34d399, #059669);
		box-shadow: 0 12rpx 22rpx rgba(52, 211, 153, .35);
	}

	.entry-text {
		flex: 1;
		margin-left: 26rpx;
		display: flex;
		flex-direction: column;
	}

	.entry-title {
		color: #0f172a;
		font-size: 32rpx;
		font-weight: 700;
	}

	.entry-desc {
		margin-top: 10rpx;
		color: #94a3b8;
		font-size: 24rpx;
	}

	.chevron {
		color: #cbd5e1;
		font-size: 48rpx;
		font-weight: 300;
	}

	/* 底部提示 */
	.tip-card {
		margin-top: 34rpx;
		padding: 26rpx 28rpx;
		border-radius: 26rpx;
		background: rgba(102, 126, 234, .08);
		display: flex;
		align-items: center;
	}

	.tip-dot {
		width: 16rpx;
		height: 16rpx;
		border-radius: 50%;
		background: #667eea;
		margin-right: 18rpx;
		flex-shrink: 0;
	}

	.tip-text {
		color: #6d7fe8;
		font-size: 24rpx;
	}
</style>
