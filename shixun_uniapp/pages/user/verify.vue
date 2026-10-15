<template>
	<view class="page">
		<!-- 状态横幅：进去就能看到自己现在到哪一步了 -->
		<view class="banner" :class="bannerClass">
			<text class="banner-title">{{ bannerTitle }}</text>
			<text class="banner-desc">{{ bannerDesc }}</text>
		</view>

		<!-- 已认证 / 审核中都不需要再填表单，直接给个返回 -->
		<view v-if="!canSubmit" class="section-card">
			<view class="info-row">
				<text class="info-label">真实姓名</text>
				<text class="info-value">{{ profile.realName || '-' }}</text>
			</view>
			<view class="info-row">
				<text class="info-label">学号</text>
				<text class="info-value">{{ profile.studentId || '-' }}</text>
			</view>
			<button class="btn primary" @tap="goBack">返回个人中心</button>
		</view>

		<template v-else>
			<view class="section-card">
				<view class="section-title">填写认证资料</view>

				<view class="form-item">
					<text class="label">真实姓名</text>
					<input
						class="input"
						v-model="form.realName"
						placeholder="请填写身份证上的姓名"
						maxlength="10"
					/>
					<text class="sub-tip">2~10 个汉字，要和学籍信息一致</text>
				</view>

				<view class="form-item">
					<text class="label">学号</text>
					<!-- 学号同时是登录账号，改了用户自己就登不进来了，所以只读 -->
					<input class="input input-readonly" :value="profile.studentId" disabled />
					<text class="sub-tip">学号是登录账号，不能在这里修改；如填错请联系管理员</text>
				</view>

				<view class="form-item">
					<text class="label">学生证照片</text>
					<view class="cert-wrap">
						<view v-if="form.studentCertUrl" class="cert-box">
							<image class="cert-img" :src="fileUrl(form.studentCertUrl)" mode="aspectFill" />
							<text class="cert-replace" @tap="pickCert">重新上传</text>
						</view>
						<view v-else class="cert-add" @tap="pickCert">
							<text class="cert-add-icon">+</text>
							<text class="cert-add-text">{{ uploading ? '上传中...' : '上传学生证照片' }}</text>
						</view>
					</view>
					<text class="sub-tip">请拍清楚姓名、学号和学校名称，管理员会人工核对</text>
				</view>
			</view>

			<view class="section-card">
				<view class="section-title">审核流程</view>
				<view class="step-row">
					<text class="step-num">1</text>
					<text class="step-text">填写资料并上传学生证照片</text>
				</view>
				<view class="step-row">
					<text class="step-num">2</text>
					<text class="step-text">管理员人工审核（通常 1 个工作日内）</text>
				</view>
				<view class="step-row">
					<text class="step-num">3</text>
					<text class="step-text">通过后显示「已认证」，可直接接单 / 提现</text>
				</view>
			</view>

			<button class="btn primary" :loading="submitting" @tap="handleSubmit">
				{{ submitting ? '提交中...' : '提交认证资料' }}
			</button>
		</template>
	</view>
</template>

<script>
	import {
		getProfile,
		submitVerify
	} from '@/services/auth.js'
	import {
		chooseAndUpload
	} from '@/services/upload.js'
	import {
		BASE_URL
	} from '@/utils/request.js'

	export default {
		data() {
			return {
				profile: {},
				form: {
					realName: '',
					studentCertUrl: ''
				},
				uploading: false,
				submitting: false
			}
		},
		computed: {
			//后端 VerifyStatus：0 未认证 / 1 已认证 / 2 审核中。
			//null 当 0 处理 —— 用户表里这一列默认是 0，老 token 里可能没有这个字段。
			//3 的含义没人知道（库里只有 3 个用户是 3），不猜，一律按「不能提交」处理。
			verifyStatus() {
				return this.profile.verifiedStatus == null ? 0 : this.profile.verifiedStatus
			},
			canSubmit() {
				return this.verifyStatus === 0
			},
			bannerClass() {
				if (this.verifyStatus === 1) return 'banner-ok'
				if (this.verifyStatus === 2) return 'banner-pending'
				return 'banner-todo'
			},
			bannerTitle() {
				if (this.verifyStatus === 1) return '已通过实名认证'
				if (this.verifyStatus === 2) return '资料审核中'
				if (this.verifyStatus === 3) return '认证状态异常'
				return '尚未实名认证'
			},
			bannerDesc() {
				if (this.verifyStatus === 1) return '你的身份信息已核验通过'
				if (this.verifyStatus === 2) return '管理员正在核对你的学生证照片，请耐心等待'
				if (this.verifyStatus === 3) return '请联系管理员确认账号的认证状态'
				return '完成实名认证后才能发布和接取订单、发起提现'
			}
		},
		onLoad() {
			this.loadProfile()
		},
		methods: {
			async loadProfile() {
				const res = await getProfile()
				if (!res) return
				this.profile = res
				//驳回后 real_name 是留在库里的，帮用户填回去，不用重新打一遍
				this.form.realName = res.realName || ''
				this.form.studentCertUrl = ''
			},
			//相对路径要拼上后端地址才能显示，绝对地址（种子数据里的历史数据）直接用
			fileUrl(url) {
				if (!url) return ''
				return url.indexOf('http') === 0 ? url : BASE_URL + url
			},
			async pickCert() {
				if (this.uploading) return
				this.uploading = true
				try {
					//只选一张：学生证只需要一张
					const urls = await chooseAndUpload(1)
					//用户取消选图时返回空数组，不是错误，不弹提示
					if (urls && urls.length) {
						this.form.studentCertUrl = urls[0]
					}
				} finally {
					this.uploading = false
				}
			},
			async handleSubmit() {
				const realName = (this.form.realName || '').trim()
				if (!/^[一-龥]{2,10}$/.test(realName)) {
					uni.showToast({
						title: '请填写 2~10 个汉字的真实姓名',
						icon: 'none'
					})
					return
				}
				if (!this.form.studentCertUrl) {
					uni.showToast({
						title: '请先上传学生证照片',
						icon: 'none'
					})
					return
				}
				this.submitting = true
				try {
					const res = await submitVerify({
						realName,
						studentId: this.profile.studentId,
						studentCertUrl: this.form.studentCertUrl
					})
					//失败时 request.js 已经弹过 toast 了，这里不再补一个
					if (!res) return
					this.profile = res
					//个人中心页读的是缓存里的 verifiedStatus，不同步的话
					//返回去还显示「未认证」，会让人以为没提交成功
					this.syncStorageUser(res)
					uni.showToast({
						title: '已提交，等待管理员审核',
						icon: 'success'
					})
					setTimeout(() => this.goBack(), 1200)
				} finally {
					this.submitting = false
				}
			},
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
				}
			},
			goBack() {
				uni.navigateBack({
					//直接从「个人中心」以外的地方进来时栈里可能没有上一页，兜底回个人中心
					fail() {
						uni.switchTab({
							url: '/pages/user/profile'
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

	.banner {
		padding: 36rpx 30rpx;
		margin-bottom: 22rpx;
		border-radius: 26rpx;
		display: flex;
		flex-direction: column;
	}

	.banner-todo {
		background: linear-gradient(135deg, #f59e0b, #f97316);
	}

	.banner-pending {
		background: linear-gradient(135deg, #38bdf8, #0ea5e9);
	}

	.banner-ok {
		background: linear-gradient(135deg, #22c55e, #16a34a);
	}

	.banner-title {
		color: #fff;
		font-size: 36rpx;
		font-weight: 700;
	}

	.banner-desc {
		margin-top: 14rpx;
		color: rgba(255, 255, 255, .9);
		font-size: 25rpx;
		line-height: 1.5;
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
		margin-bottom: 30rpx;
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

	.input-readonly {
		color: #94a3b8;
		background: #e2e8f0;
	}

	.sub-tip {
		display: block;
		margin-top: 12rpx;
		color: #94a3b8;
		font-size: 23rpx;
		line-height: 1.5;
	}

	.cert-wrap {
		display: flex;
	}

	.cert-add {
		width: 260rpx;
		height: 180rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		border: 2rpx dashed #cbd5e1;
		border-radius: 18rpx;
		background: #f8fafc;
	}

	.cert-add-icon {
		color: #94a3b8;
		font-size: 56rpx;
		line-height: 1;
	}

	.cert-add-text {
		margin-top: 10rpx;
		color: #94a3b8;
		font-size: 24rpx;
	}

	.cert-box {
		display: flex;
		flex-direction: column;
	}

	.cert-img {
		width: 260rpx;
		height: 180rpx;
		border-radius: 18rpx;
		background: #f1f5f9;
	}

	.cert-replace {
		margin-top: 12rpx;
		color: #6366f1;
		font-size: 25rpx;
	}

	.step-row {
		display: flex;
		align-items: flex-start;
		margin-bottom: 18rpx;
	}

	.step-num {
		width: 40rpx;
		height: 40rpx;
		line-height: 40rpx;
		margin-right: 16rpx;
		border-radius: 50%;
		color: #fff;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		font-size: 22rpx;
		text-align: center;
		flex-shrink: 0;
	}

	.step-text {
		flex: 1;
		color: #334155;
		font-size: 26rpx;
		line-height: 1.6;
	}

	.btn {
		width: 100%;
		height: 88rpx;
		line-height: 88rpx;
		margin: 22rpx 0 0;
		border-radius: 44rpx;
		color: #fff;
		font-size: 30rpx;
	}

	.primary {
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	}
</style>
