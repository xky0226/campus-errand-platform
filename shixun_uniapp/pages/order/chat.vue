<template>
	<view class="page">
		<scroll-view class="msg-list" scroll-y :scroll-into-view="scrollInto" scroll-with-animation>
			<!-- 空状态：种子数据里订单 9 / 11 是有对话的，新接单的订单进来就是空的 -->
			<view v-if="!loading && messages.length === 0" class="empty">
				<view class="empty-icon"><text>聊</text></view>
				<text class="empty-title">还没有聊天记录</text>
				<text class="empty-desc">和对方约一下取件、送达的时间地点吧</text>
			</view>

			<view v-for="msg in messages" :key="msg.id" class="row"
				:class="isMine(msg) ? 'row-mine' : 'row-other'">
				<view class="col">
					<!-- 昵称和时间都带上：这张截图要放进报告里，光有气泡看不出谁在说话 -->
					<text class="meta">{{ msg.senderName }} · {{ formatTime(msg.createdAt) }}</text>
					<view class="bubble" :class="isMine(msg) ? 'bubble-mine' : 'bubble-other'">
						<text class="bubble-text">{{ msg.content }}</text>
					</view>
				</view>
			</view>

			<!-- 滚动锚点：id 每滚一次就变，逼 scroll-into-view 重新触发 -->
			<view :id="'anchor-' + scrollTick" class="anchor"></view>
		</scroll-view>

		<view class="composer">
			<!-- 和 AI 会话页一样用 input 而不是 textarea：
			     小程序端 textarea 是原生组件，压在固定底栏上会有层级问题 -->
			<input class="composer-input" v-model="input" :disabled="sending" confirm-type="send"
				placeholder="说点什么…" placeholder-class="composer-placeholder" @confirm="send" />
			<view class="send-btn" :class="sending || !input.trim() ? 'send-off' : ''"
				hover-class="send-hover" @click="send">
				<text>{{ sending ? '发送中' : '发送' }}</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getMessages, sendMessage } from '@/services/message.js'

	// 轮询间隔。没有 WebSocket，就靠这个定时拉新消息。
	// 后端每次拉取会顺手把发给我的标成已读，所以这个轮询也是「已读」的触发点。
	const POLL_INTERVAL = 3000

	// 和后端 MessagesServiceImpl.MAX_CONTENT_LENGTH 保持一致，先在本地拦一道，
	// 免得白白发一次请求再弹后端的报错
	const MAX_CONTENT_LENGTH = 500

	export default {
		data() {
			return {
				orderId: null,
				currentUserId: null,
				messages: [],
				input: '',
				sending: false,
				loading: true,
				// 定时器句柄。onUnload 里必须清掉，否则退出页面后还在打接口
				timer: null,
				scrollTick: 0,
				scrollInto: ''
			}
		},
		onLoad(options) {
			this.orderId = options && options.orderId ? Number(options.orderId) : null
			// 身份和订单详情页取的是同一份缓存（登录时存的 userInfo）
			const userInfo = uni.getStorageSync('userInfo')
			this.currentUserId = userInfo && userInfo.id ? userInfo.id : null

			if (!this.orderId) {
				this.loading = false
				uni.showToast({ title: '缺少订单参数', icon: 'none' })
				return
			}

			this.loadMessages()
			this.timer = setInterval(() => {
				this.loadMessages(true)
			}, POLL_INTERVAL)
		},
		onUnload() {
			if (this.timer) {
				clearInterval(this.timer)
				this.timer = null
			}
		},
		methods: {
			// 严格比较两个数字，和订单详情页里 isPublisher 的写法保持一致
			isMine(msg) {
				return msg.senderId === this.currentUserId
			},

			// 后端给的是 "2026-03-24T11:00:04"。不用 new Date 解析 ——
			// 带 T 的字符串在 iOS 上会得到 Invalid Date，直接切字符串最稳
			formatTime(value) {
				if (!value) return ''
				const text = String(value).replace('T', ' ')
				return text.slice(5, 10) + ' ' + text.slice(11, 16)
			},

			// silent=true 表示是轮询调用的，失败不用再提示一遍
			loadMessages(silent = false) {
				return getMessages(this.orderId).then(list => {
					this.loading = false
					// 失败时 request.js 已经统一弹过 toast 了，这里按 null 处理即可
					if (!list) return

					// 条数没变就不赋值：轮询每 3 秒一次，无脑 setData 会让列表整体重渲染、
					// 滚动位置来回跳（正打字的时候特别明显）
					if (list.length !== this.messages.length) {
						this.messages = list
						this.scrollToBottom()
						this.updateTitle()
					}
				})
			},

			// 标题带上对方昵称，导航栏一眼能看出是在和谁说话
			updateTitle() {
				const other = this.messages.find(m => m.senderId !== this.currentUserId)
				if (other && other.senderName) {
					uni.setNavigationBarTitle({ title: '与' + other.senderName + '聊天' })
				}
			},

			send() {
				const content = this.input.trim()
				// 发送中防重入：连点两下别发两条
				if (!content || this.sending) return
				if (content.length > MAX_CONTENT_LENGTH) {
					uni.showToast({ title: '一条消息最多 ' + MAX_CONTENT_LENGTH + ' 个字', icon: 'none' })
					return
				}

				this.sending = true
				sendMessage(this.orderId, content).then(msg => {
					this.sending = false
					// 失败返回 null，原因已经弹过 toast
					if (!msg) return
					this.input = ''
					this.messages.push(msg)
					this.scrollToBottom()
				})
			},

			scrollToBottom() {
				this.scrollTick += 1
				this.scrollInto = 'anchor-' + this.scrollTick
			}
		}
	}
</script>

<style scoped>
	.page {
		display: flex;
		flex-direction: column;
		height: 100vh;
		background: linear-gradient(180deg, #eef0ff 0%, #f3f4fa 320rpx);
	}

	.msg-list {
		flex: 1;
		padding: 20rpx 28rpx;
	}

	/* 空状态 */
	.empty {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 120rpx 20rpx 0;
	}

	.empty-icon {
		width: 130rpx;
		height: 130rpx;
		border-radius: 40rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background: linear-gradient(135deg, #667eea, #764ba2);
		box-shadow: 0 18rpx 36rpx rgba(102, 126, 234, .35);
	}

	.empty-icon text {
		color: #fff;
		font-size: 46rpx;
		font-weight: 800;
	}

	.empty-title {
		margin-top: 30rpx;
		color: #1e293b;
		font-size: 34rpx;
		font-weight: 700;
	}

	.empty-desc {
		margin-top: 12rpx;
		color: #94a3b8;
		font-size: 25rpx;
	}

	/* 气泡 */
	.row {
		display: flex;
		margin-top: 26rpx;
	}

	.row-mine {
		justify-content: flex-end;
	}

	.row-other {
		justify-content: flex-start;
	}

	.col {
		display: flex;
		flex-direction: column;
		max-width: 78%;
	}

	.row-mine .col {
		align-items: flex-end;
	}

	.row-other .col {
		align-items: flex-start;
	}

	.meta {
		color: #94a3b8;
		font-size: 22rpx;
		margin-bottom: 8rpx;
	}

	.bubble {
		padding: 24rpx 28rpx;
		border-radius: 28rpx;
	}

	.bubble-mine {
		background: linear-gradient(135deg, #667eea, #764ba2);
		border-bottom-right-radius: 10rpx;
		box-shadow: 0 14rpx 30rpx rgba(102, 126, 234, .3);
	}

	.bubble-other {
		background: #fff;
		border-bottom-left-radius: 10rpx;
		box-shadow: 0 12rpx 30rpx rgba(30, 41, 59, .07);
	}

	.bubble-text {
		font-size: 29rpx;
		line-height: 1.6;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.bubble-mine .bubble-text {
		color: #fff;
	}

	.bubble-other .bubble-text {
		color: #1e293b;
	}

	.anchor {
		height: 20rpx;
	}

	/* 底部输入栏 */
	.composer {
		display: flex;
		align-items: center;
		gap: 18rpx;
		padding: 18rpx 28rpx calc(24rpx + env(safe-area-inset-bottom));
		background: #fff;
		box-shadow: 0 -10rpx 30rpx rgba(30, 41, 59, .06);
	}

	.composer-input {
		flex: 1;
		height: 84rpx;
		padding: 0 30rpx;
		border-radius: 42rpx;
		background: #f1f2f9;
		color: #1e293b;
		font-size: 28rpx;
	}

	.composer-placeholder {
		color: #a3a9bd;
	}

	.send-btn {
		padding: 0 36rpx;
		height: 84rpx;
		line-height: 84rpx;
		border-radius: 42rpx;
		background: linear-gradient(135deg, #667eea, #764ba2);
		box-shadow: 0 12rpx 26rpx rgba(102, 126, 234, .35);
	}

	.send-btn text {
		color: #fff;
		font-size: 28rpx;
		font-weight: 600;
	}

	.send-off {
		opacity: .45;
	}

	.send-hover {
		transform: scale(.96);
	}
</style>
