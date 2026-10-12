<template>
	<view class="page">
		<!-- 顶部：当前角色 + 清空。
		     角色是后端 ai-demo 里 .system(...) 设定的（讲义 5-2 方式一），
		     前端改不了，所以这里只做展示。想在前端切角色得给后端加参数。 -->
		<view class="role-bar">
			<view class="role-chip">
				<view class="role-dot"></view>
				<text class="role-text">{{ roleName }}</text>
			</view>
			<text class="clear-btn" hover-class="clear-hover" @click="clearMessages">清空</text>
		</view>

		<scroll-view class="msg-list" scroll-y :scroll-into-view="scrollInto" scroll-with-animation>
			<!-- 空状态：给几个能立刻试的例子，点一下直接发 -->
			<view v-if="messages.length === 0" class="empty">
				<view class="empty-icon"><text>AI</text></view>
				<text class="empty-title">有什么想问的？</text>
				<text class="empty-desc">回复会一个字一个字地出现，这就是流式输出</text>
				<view class="samples">
					<view v-for="(s, i) in samples" :key="i" class="sample" hover-class="sample-hover"
						@click="sendSample(s)">
						<text>{{ s }}</text>
					</view>
				</view>
			</view>

			<view v-for="(msg, index) in messages" :key="index" class="row" :class="msg.role === 'user' ? 'row-user' : 'row-ai'">
				<view class="bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
					<!-- pre-wrap：模型返回的内容里有换行和 Markdown，这里按纯文本原样展示 -->
					<text class="bubble-text">{{ msg.content }}</text>
					<!-- 还在往下吐字时，末尾跟一个闪烁光标 -->
					<text v-if="msg.streaming" class="cursor">▌</text>
				</view>
			</view>

			<!-- 滚动锚点：id 每块都变，强制 scroll-into-view 重新滚到底 -->
			<view :id="'anchor-' + scrollTick" class="anchor"></view>
		</scroll-view>

		<view class="composer">
			<!-- 用 input 而不是 textarea：小程序端 textarea 是原生组件，
			     跟固定底栏叠在一起容易出层级问题。需要多行输入再换 textarea。 -->
			<input class="composer-input" v-model="input" :disabled="sending" confirm-type="send"
				placeholder="说点什么…" placeholder-class="composer-placeholder" @confirm="send" />
			<view class="send-btn" :class="sending || !input.trim() ? 'send-off' : ''" hover-class="send-hover"
				@click="send">
				<text>{{ sending ? '生成中' : '发送' }}</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { chatStream } from '@/services/ai.js'

	export default {
		data() {
			return {
				// 和后端 /ai/chat2 里 .system(...) 的人设保持一致，只用于展示
				roleName: '健身达人',
				samples: ['减脂期怎么吃？', '新手一周该练几次？', '深蹲膝盖疼怎么办？'],
				messages: [],
				input: '',
				sending: false,
				// scroll-into-view 的值不变就不会重新滚，流式输出时目标一直在长高，
				// 所以用一个自增序号当锚点 id，逼它每块都重新滚到底
				scrollTick: 0,
				scrollInto: ''
			}
		},
		methods: {
			sendSample(text) {
				this.input = text
				this.send()
			},

			send() {
				const question = this.input.trim()
				if (!question || this.sending) return

				this.messages.push({ role: 'user', content: question, streaming: false })
				this.messages.push({ role: 'ai', content: '', streaming: true })

				// 注意：必须从数组里取出这条消息再改。
				// 如果拿着 push 之前那个对象字面量的引用去改 content，
				// 改的是原始对象、绕过了 Vue3 的响应式代理，界面不会更新。
				const aiMsg = this.messages[this.messages.length - 1]

				this.input = ''
				this.sending = true
				this.scrollToBottom()

				chatStream(question, {
					onChunk: piece => {
						aiMsg.content += piece
						this.scrollToBottom()
					},
					onDone: () => {
						aiMsg.streaming = false
						this.sending = false
						this.scrollToBottom()
					},
					onError: err => {
						aiMsg.streaming = false
						// 已经吐了一部分就别把已输出的内容覆盖掉
						if (!aiMsg.content) {
							aiMsg.content = '请求失败：' + (err && err.message ? err.message : '未知错误')
						}
						this.sending = false
						uni.showToast({ title: '请求失败', icon: 'none' })
					}
				})
			},

			scrollToBottom() {
				this.scrollTick += 1
				this.scrollInto = 'anchor-' + this.scrollTick
			},

			clearMessages() {
				if (this.sending) return
				this.messages = []
				this.scrollToBottom()
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

	/* 顶部角色条 */
	.role-bar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 22rpx 28rpx 12rpx;
	}

	.role-chip {
		display: flex;
		align-items: center;
		padding: 10rpx 24rpx;
		border-radius: 999rpx;
		background: #fff;
		box-shadow: 0 8rpx 24rpx rgba(30, 41, 59, .07);
	}

	.role-dot {
		width: 14rpx;
		height: 14rpx;
		border-radius: 50%;
		margin-right: 12rpx;
		background: #6366f1;
	}

	.role-text {
		color: #4338ca;
		font-size: 24rpx;
		font-weight: 600;
	}

	.clear-btn {
		color: #64748b;
		font-size: 26rpx;
		padding: 8rpx 16rpx;
	}

	.clear-hover {
		opacity: .6;
	}

	/* 消息区 */
	.msg-list {
		flex: 1;
		padding: 8rpx 28rpx;
	}

	.empty {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 90rpx 20rpx 0;
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

	.samples {
		margin-top: 36rpx;
		display: flex;
		flex-direction: column;
		gap: 20rpx;
		width: 100%;
	}

	.sample {
		padding: 24rpx 28rpx;
		border-radius: 24rpx;
		background: #fff;
		color: #475569;
		font-size: 27rpx;
		box-shadow: 0 10rpx 28rpx rgba(30, 41, 59, .06);
	}

	.sample-hover {
		transform: scale(.98);
		opacity: .9;
	}

	/* 气泡 */
	.row {
		display: flex;
		margin-top: 26rpx;
	}

	.row-user {
		justify-content: flex-end;
	}

	.row-ai {
		justify-content: flex-start;
	}

	.bubble {
		max-width: 78%;
		padding: 24rpx 28rpx;
		border-radius: 28rpx;
	}

	.bubble-user {
		background: linear-gradient(135deg, #667eea, #764ba2);
		border-bottom-right-radius: 10rpx;
		box-shadow: 0 14rpx 30rpx rgba(102, 126, 234, .3);
	}

	.bubble-ai {
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

	.bubble-user .bubble-text {
		color: #fff;
	}

	.bubble-ai .bubble-text {
		color: #1e293b;
	}

	.cursor {
		color: #6366f1;
		font-size: 29rpx;
		animation: blink .8s steps(1) infinite;
	}

	@keyframes blink {
		0%, 50% { opacity: 1; }
		51%, 100% { opacity: 0; }
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
