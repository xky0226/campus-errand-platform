<script>
	export default {
		// 讲义 §8 的 utils/request.js 和 §5 的登录页都会读写 getApp().globalData.token / .userInfo，
		// 但 uni-app 的 globalData 必须在 App.vue 里声明，
		// 不声明的话 app.globalData 是 undefined，第一次发请求就会抛
		// "Cannot read property 'token' of undefined" —— 讲义没提这一步。
		globalData: {
			token: '',
			userInfo: null,
			userId: null,
			role: 0
		},
		onLaunch: function() {
			console.log('App Launch')
		},
		onShow: function() {
			console.log('App Show')
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style>
	/*每个页面公共css */
</style>
