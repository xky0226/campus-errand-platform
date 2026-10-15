// AI 助手接口封装
// 对应讲义「5-1 SpringAI-接入大模型」和「5-2 流式对话与角色预设」。
//
// 注意：AI 后端是独立的 ai-demo 工程，跑在 8080；
// 校园帮后端跑在 8088（见 utils/request.js）。两者不是同一个服务，
// 所以这里单独写一份地址，没有走 utils/request.js 那套 Result 信封 + token 的封装 ——
// AI 接口直接返回纯文本，没有 Result 包装，也没有登录态。
const AI_BASE_URL = 'http://localhost:8080'

// ===== 会话 ID（讲义 5-4 模块 06 对话记忆）=====
// 后端挂上 MessageChatMemoryAdvisor 之后，每个接口都必须带 chatId，
// 不带会抛 conversationId cannot be null。
// 同一个 chatId = 同一场对话，模型能把上文一起发给大模型（这就是「记忆」）；
// 换一个 chatId 就是全新的一场对话。
const CHAT_ID_KEY = 'ai_chat_id'

function newChatId() {
	return 'app-' + Date.now() + '-' + Math.floor(Math.random() * 10000)
}

// 取当前会话 ID，没有就生成一个存起来 —— 这样退出页面再进来还是同一场对话
export function getChatId() {
	let id = uni.getStorageSync(CHAT_ID_KEY)
	if (!id) {
		id = newChatId()
		uni.setStorageSync(CHAT_ID_KEY, id)
	}
	return id
}

// 开一场新对话（页面上点「清空」时调用）
export function resetChatId() {
	const id = newChatId()
	uni.setStorageSync(CHAT_ID_KEY, id)
	return id
}

// 把「可能被切断的 UTF-8 字节」留到下一块再解。
// 小程序的分块回调是按字节切的，一个汉字 3 个字节，很可能被切成两半，
// 直接逐块解码会出乱码，所以必须在块之间保留没解完的字节。
function createUtf8Decoder() {
	let leftover = []

	function bytesToString(bytes) {
		let out = ''
		for (let i = 0; i < bytes.length; i++) {
			const b = bytes[i]
			if (b < 0x80) {
				out += String.fromCharCode(b)
			} else if (b < 0xe0) {
				out += String.fromCharCode(((b & 0x1f) << 6) | (bytes[++i] & 0x3f))
			} else if (b < 0xf0) {
				out += String.fromCharCode(((b & 0x0f) << 12) | ((bytes[++i] & 0x3f) << 6) | (bytes[++i] & 0x3f))
			} else {
				// 4 字节的字符（emoji 等）要拆成代理对
				const cp = ((b & 0x07) << 18) | ((bytes[++i] & 0x3f) << 12) |
					((bytes[++i] & 0x3f) << 6) | (bytes[++i] & 0x3f)
				const c = cp - 0x10000
				out += String.fromCharCode(0xd800 + (c >> 10), 0xdc00 + (c & 0x3ff))
			}
		}
		return out
	}

	return function decode(buffer) {
		const incoming = new Uint8Array(buffer)
		const all = new Uint8Array(leftover.length + incoming.length)
		all.set(leftover, 0)
		all.set(incoming, leftover.length)

		// 从尾巴往前找出被截断的多字节序列，剩下的留到下一块
		let cut = all.length
		let lead = all.length - 1
		while (lead >= 0 && (all[lead] & 0xc0) === 0x80) lead-- // 跳过续字节
		if (lead >= 0) {
			const b = all[lead]
			let need = 1
			if ((b & 0x80) === 0x00) need = 1
			else if ((b & 0xe0) === 0xc0) need = 2
			else if ((b & 0xf0) === 0xe0) need = 3
			else if ((b & 0xf8) === 0xf0) need = 4
			if (all.length - lead < need) cut = lead
		}

		leftover = Array.prototype.slice.call(all.subarray(cut))
		return bytesToString(all.subarray(0, cut))
	}
}

// 非流式：等模型全部生成完再一次性返回。
// 对应讲义 5-2 的 /ai/chat（call()），也是流式不可用时的兜底。
export function chatOnce(question) {
	return new Promise((resolve, reject) => {
		uni.request({
			url: AI_BASE_URL + '/ai/chat?chatId=' + encodeURIComponent(getChatId())
				+ '&question=' + encodeURIComponent(question),
			method: 'GET',
			timeout: 60000,
			success(res) {
				if (res.statusCode === 200) resolve(res.data)
				else reject(new Error('HTTP ' + res.statusCode))
			},
			fail(err) {
				reject(new Error((err && err.errMsg) || '网络请求失败'))
			}
		})
	})
}

// 流式：边生成边把新增的文字回调出去。
// 对应讲义 5-2 的 /ai/chat2（stream()）。
//
// 三端拿流的方式不一样，用条件编译分开写：
//   H5        —— fetch + ReadableStream（讲义 5-2 Q1 提到的就是这种方式）
//   微信小程序 —— wx.request 的 enableChunked + onChunkReceived
//   其他（App）—— 退回非流式，整段返回
// 注意：/ai/chat2 返回的是 text/plain 的分块内容，不是 SSE 的 text/event-stream，
// 所以 H5 端不能用 EventSource，必须走 fetch。

// #ifdef H5
export function chatStream(question, { onChunk, onDone, onError }) {
	const decoder = new TextDecoder('utf-8')
	fetch(AI_BASE_URL + '/ai/chat2?chatId=' + encodeURIComponent(getChatId())
		+ '&question=' + encodeURIComponent(question))
		.then(res => {
			if (!res.ok) throw new Error('HTTP ' + res.status)
			const reader = res.body.getReader()
			const pump = () => reader.read().then(({ done, value }) => {
				if (done) {
					onDone && onDone()
					return
				}
				const text = decoder.decode(value, { stream: true })
				if (text) onChunk(text)
				return pump()
			})
			return pump()
		})
		.catch(err => onError && onError(err))
}
// #endif

// #ifdef MP-WEIXIN
export function chatStream(question, { onChunk, onDone, onError }) {
	const decode = createUtf8Decoder()
	const task = wx.request({
		url: AI_BASE_URL + '/ai/chat2?chatId=' + encodeURIComponent(getChatId())
			+ '&question=' + encodeURIComponent(question),
		method: 'GET',
		enableChunked: true, // 关键：不打开的话拿到的是一次性完整响应
		timeout: 60000,
		success() {
			onDone && onDone()
		},
		fail(err) {
			onError && onError(new Error((err && err.errMsg) || '网络请求失败'))
		}
	})
	// 分块回调：data 是 ArrayBuffer
	task.onChunkReceived(res => {
		const text = decode(res.data)
		if (text) onChunk(text)
	})
}
// #endif

// #ifndef H5 || MP-WEIXIN
export function chatStream(question, { onChunk, onDone, onError }) {
	chatOnce(question)
		.then(text => {
			onChunk(text)
			onDone && onDone()
		})
		.catch(err => onError && onError(err))
}
// #endif
