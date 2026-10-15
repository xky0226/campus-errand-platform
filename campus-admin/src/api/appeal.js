// 纠纷仲裁中心接口封装（需求 3.5.3）
// 对应后端 com.example.campus.controller.AdminAppealController
import { get, post } from '@/api'

// 申诉列表。status 不传查全部：0 待处理 / 1 已处理；keyword 匹配订单号、任务类型、双方昵称
export const fetchAppeals = (params = {}) => get('/admin/appeals', params)

// 申诉详情：带订单快照、证据图、处理记录
export const fetchAppealDetail = id => get(`/admin/appeals/${id}`)

// 仲裁处理。actions 是数组，取值 REFUND / COMPENSATE / BAN / REJECT（REJECT 与其它三个互斥）
// amount 只在带 COMPENSATE 时有意义
export const handleAppeal = (id, data) => post(`/admin/appeals/${id}/handle`, data)

// 该申诉对应订单的聊天记录（需求 3.5.3「查看聊天记录与证据」）。
// 后端顺着 申诉 → orderId → messages 走，所以传的是申诉 id 不是订单 id。
// 订单里没聊过天时返回的是空数组，不算失败；真失败（网络/权限）会走全局拦截器弹提示，
// 调用处再 catch 一下兜底，别让聊天记录这一块把整个详情弹窗带崩。
export const fetchAppealMessages = id => get(`/admin/appeals/${id}/messages`)
