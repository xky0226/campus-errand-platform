import { get, put } from '@/api'

//查询用户列表（后端一次返回全部，前端做搜索 / 筛选 / 分页）
export function fetchUsers() {
  return get('/admin/users')
}

//编辑用户资料，或只传 { status } 做禁用 / 启用
export function updateUser(id, data) {
  return put('/admin/users/' + id, data)
}
