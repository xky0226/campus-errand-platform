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
