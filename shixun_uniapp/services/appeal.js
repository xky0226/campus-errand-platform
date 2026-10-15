import { get, post } from '@/utils/request.js'

// 订单仲裁（需求 3.5.3 的用户侧入口）。
// 提交后订单会变成「申诉中」，等管理员在管理端「申诉仲裁」里处理。

// 提交申诉。申诉人和被申诉人由后端按订单反推，前端不用传。
// evidenceUrls 是先用 services/upload.js 上传拿到的 /files/... 路径，最多 3 张。
export function submitAppeal(orderId, reason, evidenceUrls = []) {
	return post('/appeals', { orderId, reason, evidenceUrls })
}

// 我提交过的申诉，用来回看仲裁结果
export function getMyAppeals() {
	return get('/appeals/my')
}
