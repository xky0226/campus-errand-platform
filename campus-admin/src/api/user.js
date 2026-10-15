import { get, post, put } from '@/api'

//查询用户列表（后端一次返回全部，前端做搜索 / 筛选 / 分页）
export function fetchUsers() {
  return get('/admin/users')
}

//编辑用户资料，或只传 { status } 做禁用 / 启用
export function updateUser(id, data) {
  return put('/admin/users/' + id, data)
}

// ===== 实名认证审核（需求 3.5.1）=====

//待审核列表：后端只返回 verified_status=2（审核中）的用户
export function fetchPendingVerify() {
  return get('/admin/users/pending-verify')
}

//审核：pass=true 通过，pass=false 驳回（驳回时 reason 必填）
export function verifyUser(id, pass, reason) {
  return post('/admin/users/' + id + '/verify', { pass, reason })
}

// ===== 信用分（需求 3.5.1）=====

//某个用户的信用分变动历史
export function fetchCreditHistory(id) {
  return get('/admin/users/' + id + '/credit-history')
}

//按「变动值」调整信用分，会留一条可追溯的历史记录。
//注意和 updateUser(id, { creditScore }) 的区别：那个是直接设成某个值，不留痕。
export function adjustCredit(id, delta, reason) {
  return post('/admin/users/' + id + '/credit', { delta, reason })
}
