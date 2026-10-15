import axios from 'axios'

/**
 * 下载后端生成的报表文件。
 *
 * 为什么不能用 @/api 里那个 axios 实例：它的响应拦截器是按 JSON 解包的
 * （只认 body.code === 200，否则弹「请求失败」）。导出接口返回的是文件字节流，
 * 走那个实例会被当成解析失败。所以这里单独发一个 responseType: 'blob' 的请求。
 *
 * @param url      接口路径，不带 /api 前缀（下面会拼）
 * @param params   查询参数，和列表页用同一套筛选条件
 * @param fallback 后端没给文件名时用的兜底名
 * @returns {Promise<string>} 实际保存的文件名
 */
export async function downloadCsv(url, params, fallback) {
  const token = localStorage.getItem('admin_token')

  let response
  try {
    response = await axios.get('/api' + url, {
      params,
      responseType: 'blob',
      // 这个请求绕过了 @/api 的实例，所以 token 得自己带
      headers: token ? { Authorization: 'Bearer ' + token } : {}
    })
  } catch (error) {
    // 走到这里说明是 4xx / 5xx。错误响应体同样是 Blob（因为 responseType 是 blob），
    // 不读出来就只能给用户看一句没用的「Network Error」。
    const blob = error.response && error.response.data
    if (blob instanceof Blob) {
      throw new Error(await readMessage(blob))
    }
    throw error
  }

  // 后端出错时返回的是 Result 封装的 JSON。它可能是 HTTP 200（业务错误，
  // 比如「无管理员权限」就是 BusinessException），这时 axios 不会报错，
  // 只能靠 Content-Type 认出来 —— 否则会把一段 JSON 当 CSV 下下来，
  // 用户拿到一个打不开的文件还不知道为什么。
  const contentType = String(response.headers['content-type'] || '')
  if (contentType.includes('application/json')) {
    throw new Error(await readMessage(response.data))
  }

  saveBlob(response.data, parseFilename(response.headers['content-disposition']) || fallback)
  return parseFilename(response.headers['content-disposition']) || fallback
}

async function readMessage(blob) {
  try {
    const body = JSON.parse(await blob.text())
    return body.message || '导出失败'
  } catch (e) {
    return '导出失败'
  }
}

function saveBlob(blob, filename) {
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  // 不释放的话这个 Blob 会一直占着内存，导几次大报表就顶起来了
  URL.revokeObjectURL(objectUrl)
}

function parseFilename(disposition) {
  if (!disposition) return ''
  // 后端两个都给了：filename="____20261015.csv"（非 ASCII 换成下划线的兜底名）
  // 和 filename*=UTF-8''%E8%AE%A2...（RFC 5987 的编码名）。
  // 优先用后者，中文文件名才不会变成一片下划线。
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (encoded) {
    try {
      return decodeURIComponent(encoded[1])
    } catch (e) {
      // 编码坏了就往下走，用兜底的那个
    }
  }
  const plain = /filename="([^"]+)"/i.exec(disposition)
  return plain ? plain[1] : ''
}
