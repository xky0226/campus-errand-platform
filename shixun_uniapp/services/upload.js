import { BASE_URL } from '@/utils/request.js'

// 图片上传。走不了 utils/request.js 那个封装，原因有两个：
//   1) uni.request 发不了 multipart/form-data，上传必须用 uni.uploadFile；
//   2) request.js 已经统一弹过失败 toast 了，这里再套一层会弹两次。
// 所以单独封一层，自己负责拼 token、解析响应、弹提示。
//
// 上传成功后返回的是 /files/yyyy/MM/dd/xxx.png 这种相对路径，
// 直接存进 appeals.evidence_urls / users.student_cert_url 即可。

/**
 * 选图并逐张上传。
 * @param {number} count 最多选几张
 * @returns {Promise<string[]>} 上传成功的图片路径数组；用户取消或全部失败时是空数组
 */
export function chooseAndUpload(count = 3) {
	return new Promise(resolve => {
		uni.chooseImage({
			count,
			// 手机拍的照片动辄好几兆，后端限制 5MB，这里统一走压缩
			sizeType: ['compressed'],
			sourceType: ['album', 'camera'],
			success(res) {
				const paths = res.tempFilePaths || []
				if (!paths.length) {
					return resolve([])
				}
				Promise.all(paths.map(uploadOne)).then(urls => resolve(urls.filter(Boolean)))
			},
			// 用户自己取消了选图，不算失败，不弹提示
			fail() {
				resolve([])
			}
		})
	})
}

function uploadOne(filePath) {
	return new Promise(resolve => {
		const app = getApp()
		const token = (app && app.globalData && app.globalData.token) || uni.getStorageSync('token') || ''

		uni.uploadFile({
			url: BASE_URL + '/files/upload',
			filePath,
			// 必须和后端 FileController 的 @RequestParam("file") 对上
			name: 'file',
			header: {
				Authorization: token ? 'Bearer ' + token : ''
			},
			success(res) {
				// 注意坑：uni.uploadFile 不像 uni.request 那样自动把响应解析成对象，
				// res.data 是字符串，必须自己 JSON.parse。
				let body = null
				try {
					body = JSON.parse(res.data)
				} catch (e) {
					body = null
				}
				if (body && body.code === 200 && body.data) {
					return resolve(body.data.url)
				}
				uni.showToast({
					title: (body && body.message) || '图片上传失败',
					icon: 'none'
				})
				resolve(null)
			},
			fail() {
				uni.showToast({
					title: '图片上传失败',
					icon: 'none'
				})
				resolve(null)
			}
		})
	})
}
