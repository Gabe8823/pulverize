import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': import.meta.dirname + '/src',
    },
  },
  server: {
    port: 2020,
    proxy: {
      '/api': {
        target: 'http://localhost:2021',
        changeOrigin: true,
      },
    },
  },
  build: {
    // echarts 已按需注册（utils/chart.ts）并拆为懒加载 vendor chunk（gzip ≈180kB，
    // 仅 Dashboard/Report/ActivityDetail 路由访问时才加载），压缩后稳定在 ~530kB，
    // 属预期体积，放宽阈值以避免每次构建误报。
    chunkSizeWarningLimit: 600,
  },
})
