// 后端基础地址
// 讲义「4、移动端-构建」写的是 http://localhost:8080/api，两处都不对：
//  1) 本项目后端实际跑在 8088；
//  2) 后端没有配 server.servlet.context-path，路径里没有 /api
//     （campus-admin 是靠 vite proxy 的 rewrite 把 /api 剥掉的，小程序没有这层代理）。
// 所以这里只写到端口，页面里直接写业务路径。
// 导出给 services/upload.js 用：uni.uploadFile 走不了这个 request 封装
// （它是 multipart，不是 JSON），但得用同一个后端地址，不能各写各的。
export const BASE_URL = 'http://localhost:8088'

// 定义一个请求函数【封装请求】
function request(method, url, data = {}, skipAuth = false) {
	return new Promise(resolve => {
		const app = getApp()
		const token = (app && app.globalData && app.globalData.token) || uni.getStorageSync('token') || ''

		//uni.request发起请求
		uni.request({
			url: BASE_URL + url,
			method,
			data,
			header: {
				Authorization: token ? 'Bearer ' + token : '',
				'Content-Type': 'application/json'
			},
			//请求成功的回调
			success(res) {
				//请求状态判断
				if (res.data && res.data.code === 200) {
					// 只把 Result 里的 data 载荷交给页面。
					// 讲义 §8 原文是 resolve(res.data)（整个信封），但 §5 登录页、§11 列表页
					// 都把返回值当载荷用（直接读 res.token、把返回值当数组/对象），按载荷口径统一。
					resolve(res.data.data)
				} else if (res.data && res.data.code === 401 && !skipAuth) {
					uni.removeStorageSync('token')
					uni.removeStorageSync('userInfo')
					if (app && app.globalData) {
						app.globalData.token = ''
						app.globalData.userInfo = null
					}
					resolve(null)
				} else {
					console.error('接口请求失败:', url, res.data)
					// 业务失败（比如钱包余额不足）时后端会把原因写在 message 里，
					// 统一弹个 toast，不然页面只能看到「操作失败」，不知道到底为什么
					const message = res.data && res.data.message
					if (message && message !== 'success') {
						uni.showToast({
							title: message,
							icon: 'none'
						})
					}
					resolve(null)
				}
			},
			//请求失败的回调
			fail(err) {
				console.error('网络请求失败:', url, err)
				uni.showToast({
					title: '网络请求失败',
					icon: 'none'
				})
				resolve(null)
			}
		})
	})
}

export const get = url => request('GET', url)
export const post = (url, data) => request('POST', url, data)
export const put = (url, data) => request('PUT', url, data)
export const del = url => request('DELETE', url)
// 登录、注册这类不需要 token 的接口用 postLogin
export const postLogin = (url, data) => request('POST', url, data, true)
