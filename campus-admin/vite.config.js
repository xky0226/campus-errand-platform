import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        // 后台前端开发地址：http://localhost:3000
        // 后端服务地址：http://localhost:8088（讲义写的是 8080，本项目实际 8088）
        target: 'http://localhost:8088',
        changeOrigin: true,
        // 讲义没有这一行：讲义的后端配了 context-path: /api，
        // 本项目后端没有那个前缀，所以要在这里把 /api 剥掉。
        // get('/admin/orders') → /api/admin/orders → /admin/orders 打到 8088
        rewrite: p => p.replace(/^\/api/, '')
      },
      // 上传的图片（申诉证据图、学生证照片）不挂在 /api 下，后端直接暴露在 /files/**，
      // 不单独代理的话 <el-image src="/files/2026/10/15/x.png"> 会被浏览器解析到 3000 端口
      // 然后 404 裂图。这里原样转发，不要 rewrite。
      '/files': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    chunkSizeWarningLimit: 1500
  }
})
