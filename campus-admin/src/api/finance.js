// 财务管理接口封装（需求 3.5.4）
// 对应后端 com.example.campus.controller.AdminFinanceController
import { get, post } from '@/api'
import { downloadCsv } from '@/api/download'

// 流水汇总：各类型的笔数与金额 + 待审核提现数量
export const fetchFinanceStatistics = () => get('/admin/finance/statistics')

// 营收趋势。period 取 'day'（最近 7 天）/ 'week'（最近 6 周）/ 'month'（最近 6 个月）
export const fetchRevenue = (period = 'day') => get('/admin/finance/revenue', { period })

// 提现申请列表。status 不传查全部：0 待审核 / 1 已通过 / 2 已驳回
// 注意 get 的第二个参数是 params 对象，传 null 会让 axios 把 status=null 当字符串发出去，
// 所以这里显式用空对象表示「不带条件」。
export const fetchWithdrawals = status =>
  get('/admin/finance/withdrawals', status === null || status === undefined ? {} : { status })

// 审核提现。pass=true 通过（钱真正转出），false 驳回（退回用户可用余额）
export const auditWithdraw = (id, pass, remark) =>
  post(`/admin/finance/withdrawals/${id}/audit`, { pass, remark })

// 导出财务报表：后端生成 CSV 字节流直接下载，不走 JSON 解包那套。
// 内容是全量资金流水明细（时间/类型/收支/金额/用户/关联订单/状态/备注）
export const exportFinance = () =>
  downloadCsv('/admin/finance/export', {}, '资金流水报表.csv')
