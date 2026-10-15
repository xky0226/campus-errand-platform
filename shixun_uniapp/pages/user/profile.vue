<template>
	<view class="page">
		<!-- 没登录时不展示资料，避免读到空缓存报错 -->
		<view v-if="!token" class="empty">
			<text class="empty-title">还没有登录</text>
			<text class="empty-desc">登录后才能查看和修改个人信息</text>
			<button class="btn primary" @tap="goLogin">去登录</button>
		</view>

		<template v-else>
			<!-- 头部：头像 + 昵称 + 角色 -->
			<view class="header-card">
				<image v-if="profile.avatarUrl" class="avatar" :src="profile.avatarUrl" mode="aspectFill" />
				<view v-else class="avatar avatar-fallback">
					<text class="avatar-text">{{ avatarLetter }}</text>
				</view>
				<view class="header-info">
					<text class="nickname">{{ profile.nickname || '未设置昵称' }}</text>
					<text class="role-tag">{{ roleText }}</text>
				</view>
			</view>

			<!-- 三项数据 -->
			<view class="stat-card">
				<view class="stat-item">
					<text class="stat-value">{{ profile.creditScore == null ? '-' : profile.creditScore }}</text>
					<text class="stat-label">信用分</text>
				</view>
				<view class="stat-item">
					<text class="stat-value">{{ profile.completedOrders == null ? 0 : profile.completedOrders }}</text>
					<text class="stat-label">完成订单</text>
				</view>
				<view class="stat-item" @tap="goWallet">
					<text class="stat-value">{{ profile.walletBalance == null ? '0.00' : profile.walletBalance }}</text>
					<text class="stat-label">余额(元) ›</text>
				</view>
			</view>

			<!-- 资料展示 -->
			<view class="section-card">
				<view class="section-title">我的资料</view>
				<view class="info-row">
					<text class="info-label">学号</text>
					<text class="info-value">{{ profile.studentId || '-' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">手机号</text>
					<text class="info-value">{{ profile.phone || '未填写' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">认证状态</text>
					<text class="info-value">{{ verifyText }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">账号状态</text>
					<text class="info-value">{{ profile.status === 1 ? '正常' : '已冻结' }}</text>
				</view>
			</view>

			<!-- 实名认证入口：已认证了就不显示这块，不然纯占地方 -->
			<view v-if="profile.verifiedStatus !== 1" class="section-card">
				<view class="section-title">实名认证</view>
				<!-- 审核中：只提示，不给再点进去重复提交（后端也会拒） -->
				<template v-if="profile.verifiedStatus === 2">
					<view class="verify-status verify-pending">
						<text class="verify-status-title">资料审核中</text>
						<text class="verify-status-desc">管理员正在核对你的学生证照片，通过后这里会变成「已认证」</text>
					</view>
					<button class="btn plain" @tap="goVerify">查看已提交的资料</button>
				</template>
				<!-- 0 / null：未认证，可以提交。3 的含义不明确，不引导用户去重复提交 -->
				<template v-else-if="profile.verifiedStatus === 0 || profile.verifiedStatus == null">
					<view class="verify-status verify-todo">
						<text class="verify-status-title">尚未实名认证</text>
						<text class="verify-status-desc">上传学生证照片，管理员审核通过后即可发布和接取订单</text>
					</view>
					<button class="btn primary" @tap="goVerify">去实名认证</button>
				</template>
				<template v-else>
					<view class="verify-status verify-todo">
						<text class="verify-status-title">认证状态异常</text>
						<text class="verify-status-desc">当前认证状态码是 {{ profile.verifiedStatus }}，请联系管理员确认</text>
					</view>
				</template>
			</view>

			<!-- 资料修改 -->
			<view class="section-card">
				<view class="section-title">修改资料</view>
				<view class="form-item">
					<text class="label">昵称</text>
					<input class="input" v-model="form.nickname" placeholder="请输入昵称" maxlength="64" />
				</view>
				<view class="form-item">
					<text class="label">头像地址</text>
					<input class="input" v-model="form.avatarUrl" placeholder="填入一个图片链接" />
				</view>
				<view class="form-item">
					<text class="label">手机号</text>
					<input class="input" v-model="form.phone" type="number" placeholder="请输入手机号" maxlength="16" />
				</view>
				<view class="tip">学号、角色、信用分由后台管理，不能在这里改</view>
				<button class="btn primary" :loading="saving" @tap="handleSave">
					{{ saving ? '保存中...' : '保存修改' }}
				</button>
			</view>

			<!-- 我的订单快捷入口 -->
			<view class="section-card">
				<view class="section-title">我的订单</view>
				<view class="entry-row">
					<view class="entry" @tap="goOrders('published')">
						<text class="entry-icon">发</text>
						<text class="entry-text">我发布的</text>
					</view>
					<view class="entry" @tap="goOrders('running')">
						<text class="entry-icon">接</text>
						<text class="entry-text">我接取的</text>
					</view>
					<view class="entry" @tap="goWallet">
						<text class="entry-icon">钱</text>
						<text class="entry-text">我的钱包</text>
					</view>
				</view>
			</view>

			<!-- 角色切换（role=2 是管理员，不给切） -->
			<button v-if="profile.role === 1" class="btn success" :loading="switching" @tap="handleSwitchRole">
				{{ switching ? '切换中...' : '切换回普通用户' }}
			</button>
			<button v-else-if="profile.role !== 2" class="btn success" :loading="switching" @tap="handleSwitchRole">
				{{ switching ? '切换中...' : '切换为跑腿员' }}
			</button>

			<button class="btn danger" @tap="handleLogout">退出登录</button>
		</template>
	</view>
</template>

<script>
	import {
		getProfile,
		updateProfile,
		switchRole
	} from '@/services/auth.js'

	export default {
		data() {
			return {
				token: '',
				profile: {},
				form: {
					nickname: '',
					avatarUrl: '',
					phone: ''
				},
				saving: false,
				switching: false,
				// 讲义没有定义 verified_status 的取值含义，0/1/2 是按管理端
				// AdminDashboardService 里「待审核 = verifiedStatus==2」反推的，
				// 库里还有 3，不知道是什么，下面兜底成「状态3」而不是瞎编一个词
				verifyMap: {
					0: '未认证',
					1: '已认证',
					2: '审核中'
				}
			}
		},
		computed: {
			roleText() {
				const map = {
					0: '普通用户',
					1: '跑腿员',
					2: '管理员'
				}
				return map[this.profile.role] || '普通用户'
			},
			verifyText() {
				const v = this.profile.verifiedStatus
				if (v == null) return '未认证'
				return this.verifyMap[v] || ('状态' + v)
			},
			avatarLetter() {
				const name = this.profile.nickname || '用'
				return name.charAt(0)
			}
		},
		onShow() {
			this.token = uni.getStorageSync('token') || ''
			if (this.token) {
				this.loadProfile()
			}
		},
		methods: {
			async loadProfile() {
				const res = await getProfile()
				if (!res) {
					// token 过期或用户被删了，按未登录处理
					this.token = ''
					return
				}
				this.profile = res
				this.form.nickname = res.nickname || ''
				this.form.avatarUrl = res.avatarUrl || ''
				this.form.phone = res.phone || ''
				this.syncStorageUser(res)
			},
			// 把最新的用户信息同步到缓存 + globalData，别的页面读缓存才不会是旧的
			syncStorageUser(user) {
				const userInfo = {
					id: user.id,
					nickname: user.nickname || '微信用户',
					avatarUrl: user.avatarUrl || '',
					role: user.role || 0,
					verifiedStatus: user.verifiedStatus || 0,
					creditScore: user.creditScore || 100
				}
				uni.setStorageSync('userInfo', userInfo)
				const app = getApp()
				if (app && app.globalData) {
					app.globalData.userInfo = userInfo
					app.globalData.userId = userInfo.id
					app.globalData.role = userInfo.role
				}
			},
			async handleSave() {
				if (!this.form.nickname || !this.form.nickname.trim()) {
					uni.showToast({
						title: '昵称不能为空',
						icon: 'none'
					})
					return
				}
				this.saving = true
				try {
					const res = await updateProfile({
						nickname: this.form.nickname.trim(),
						avatarUrl: this.form.avatarUrl.trim(),
						phone: this.form.phone.trim()
					})
					if (!res) {
						uni.showToast({
							title: '保存失败',
							icon: 'none'
						})
						return
					}
					this.profile = res
					this.syncStorageUser(res)
					uni.showToast({
						title: '保存成功',
						icon: 'success'
					})
				} finally {
					this.saving = false
				}
			},
			async handleSwitchRole() {
				const toRunner = this.profile.role !== 1
				const target = toRunner ? 1 : 0
				this.switching = true
				try {
					const res = await switchRole(target)
					if (!res) {
						uni.showToast({
							title: '切换失败',
							icon: 'none'
						})
						return
					}
					// 后端会把 role 写进新 token 一起返回，这里按登录那套存一遍，
					// 否则 token 里还是旧角色
					uni.setStorageSync('token', res.token)
					this.token = res.token
					const app = getApp()
					if (app && app.globalData) {
						app.globalData.token = res.token
					}
					uni.showToast({
						title: toRunner ? '已切换为跑腿员' : '已切回普通用户',
						icon: 'success'
					})
					await this.loadProfile()
				} finally {
					this.switching = false
				}
			},
			goOrders(tab) {
				// switchTab 不能带参数，用缓存把要看的 tab 传给订单列表页
				uni.setStorageSync('orderListTab', tab)
				uni.switchTab({
					url: '/pages/order/list'
				})
			},
			goWallet() {
				uni.navigateTo({
					url: '/pages/wallet/wallet'
				})
			},
			goVerify() {
				uni.navigateTo({
					url: '/pages/user/verify'
				})
			},
			goLogin() {
				uni.navigateTo({
					url: '/pages/auth/login'
				})
			},
			handleLogout() {
				uni.showModal({
					title: '退出登录',
					content: '确定要退出当前账号吗？',
					success: res => {
						if (!res.confirm) return
						uni.removeStorageSync('token')
						uni.removeStorageSync('userInfo')
						const app = getApp()
						if (app && app.globalData) {
							app.globalData.token = ''
							app.globalData.userInfo = null
							app.globalData.userId = null
							app.globalData.role = 0
						}
						this.token = ''
						this.profile = {}
						uni.reLaunch({
							url: '/pages/auth/login'
						})
					}
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

	.header-card {
		display: flex;
		align-items: center;
		padding: 40rpx 30rpx;
		margin-bottom: 22rpx;
		border-radius: 26rpx;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		box-shadow: 0 12rpx 32rpx rgba(102, 126, 234, .3);
	}

	.avatar {
		width: 130rpx;
		height: 130rpx;
		border-radius: 50%;
		background: rgba(255, 255, 255, .3);
		flex-shrink: 0;
	}

	.avatar-fallback {
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.avatar-text {
		color: #fff;
		font-size: 56rpx;
		font-weight: 700;
	}

	.header-info {
		margin-left: 28rpx;
		display: flex;
		flex-direction: column;
	}

	.nickname {
		color: #fff;
		font-size: 40rpx;
		font-weight: 700;
	}

	.role-tag {
		align-self: flex-start;
		margin-top: 16rpx;
		padding: 6rpx 20rpx;
		border-radius: 999rpx;
		color: #667eea;
		background: rgba(255, 255, 255, .92);
		font-size: 24rpx;
	}

	.stat-card {
		display: flex;
		padding: 30rpx 0;
		margin-bottom: 22rpx;
		border-radius: 26rpx;
		background: #fff;
		box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, .08);
	}

	.stat-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.stat-value {
		color: #0f172a;
		font-size: 40rpx;
		font-weight: 700;
	}

	.stat-label {
		margin-top: 12rpx;
		color: #94a3b8;
		font-size: 24rpx;
	}

	.section-card {
		padding: 30rpx;
		margin-bottom: 22rpx;
		border-radius: 26rpx;
		background: #fff;
		box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, .08);
	}

	.section-title {
		margin-bottom: 24rpx;
		color: #0f172a;
		font-size: 32rpx;
		font-weight: 700;
	}

	.info-row {
		display: flex;
		justify-content: space-between;
		gap: 30rpx;
		padding: 20rpx 0;
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

	.form-item {
		margin-bottom: 26rpx;
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

	.tip {
		margin-bottom: 24rpx;
		color: #94a3b8;
		font-size: 24rpx;
	}

	.verify-status {
		display: flex;
		flex-direction: column;
		padding: 24rpx;
		margin-bottom: 24rpx;
		border-radius: 18rpx;
	}

	.verify-todo {
		background: #fff7ed;
	}

	.verify-pending {
		background: #eff6ff;
	}

	.verify-status-title {
		color: #334155;
		font-size: 28rpx;
		font-weight: 700;
	}

	.verify-status-desc {
		margin-top: 10rpx;
		color: #94a3b8;
		font-size: 24rpx;
		line-height: 1.5;
	}

	.plain {
		background: #e2e8f0;
		color: #334155;
	}

	.entry-row {
		display: flex;
		gap: 22rpx;
	}

	.entry {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 28rpx 0;
		border-radius: 18rpx;
		background: #f8fafc;
	}

	.entry-icon {
		width: 64rpx;
		height: 64rpx;
		line-height: 64rpx;
		border-radius: 50%;
		color: #fff;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		font-size: 28rpx;
		text-align: center;
	}

	.entry-text {
		margin-top: 16rpx;
		color: #334155;
		font-size: 26rpx;
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

	.success {
		background: linear-gradient(135deg, #22c55e, #16a34a);
	}

	.danger {
		background: linear-gradient(135deg, #fb7185, #ef4444);
	}
</style>
