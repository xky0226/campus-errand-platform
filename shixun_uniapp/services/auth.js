import { get, post, postLogin, put } from '@/utils/request.js'

export function login(username, password) {
	return postLogin('/auth/login', { username, password })
}

export function wxLogin(code, nickname = '微信用户', avatarUrl = '') {
	return postLogin('/auth/wx-login', { code, nickname, avatarUrl })
}

export function register(data) {
	return postLogin('/auth/register', data)
}

export function getProfile() {
	return get('/auth/profile')
}

export function updateProfile(data) {
	return put('/auth/profile', data)
}

export function switchRole(role) {
	return post('/auth/switch-role?role=' + role)
}

//提交实名认证资料，提交后状态变成「审核中」，等管理端在「认证审核」页处理。
//照片要先走 services/upload.js 传到 /files/upload 拿到路径，这里只传路径。
//注意 studentId 只是原样回传做一致性校验，后端不会改它 —— 学号同时是登录账号。
export function submitVerify(data) {
	return post('/auth/verify', data)
}
