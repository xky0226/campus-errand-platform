import { get, post } from '@/utils/request.js'

// 订单聊天（需求「移动端前台功能 1、聊天界面」）。
// 发布者和跑腿员接单之后在这条通道里沟通取件、送达这些事。
//
// 走的是本项目的后端（8088），不是 AI 助手那个独立的 8080 ——
// services/ai.js 里那套是为了对接 ai-demo 单独写的（无 token、无 Result 信封、流式），
// 和这里完全是两回事，别拿来抄。

// 拉某个订单的聊天记录。后端会顺手把「发给我的」未读标成已读。
// 失败返回 null（utils/request.js 统一 resolve(null)，不是 reject）。
export function getMessages(orderId) {
	return get('/messages?orderId=' + orderId)
}

// 发一条消息。发送人由后端从 token 取，接收方按订单双方反推，前端只传内容和订单号。
// 返回刚发出去的那条消息（带 senderName / createdAt），失败同样是 null。
export function sendMessage(orderId, content) {
	return post('/messages', { orderId, content })
}
