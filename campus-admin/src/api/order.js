// 订单模块接口封装
// 说明：讲义 3-3 的页面脚本从 "@/api/order" 引入了下面 5 个函数，
//      但讲义没有给出本文件的代码，接口路径取自讲义 3-3 第 7 节「联调步骤」。
import { get, post, del } from '@/api'
import { downloadCsv } from '@/api/download'

// 订单列表。params 支持 { status, keyword }（需求 3.5.2 的多条件查询），
// 不传就是查全部，和接筛选之前的行为一致。
export const fetchOrders = (params) => get('/admin/orders', params)

// 导出订单报表。筛选用和列表页同一套条件，导出来的就是当前看到的那批。
export const exportOrders = (params) =>
  downloadCsv('/admin/orders/export', params, '订单报表.csv')

// 新增订单
export const createOrder = data => post('/admin/orders/create', data)

// 编辑订单
export const updateOrder = (id, data) => post(`/admin/orders/${id}/update`, data)

// 强制取消订单
export const forceCancelOrder = id => post(`/admin/orders/${id}/force-cancel`)

// 删除订单
export const deleteOrder = id => del(`/admin/orders/${id}`)
