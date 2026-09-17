import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    // ElementPlus 按需引入: 组件/API/样式自动导入, 大幅缩减首屏体积
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: false
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    // 默认打包进后端 static(单 jar 部署)；设置 VITE_OUT_DIR=dist 时输出到前端目录(前后端分离部署)
    outDir: process.env.VITE_OUT_DIR || '../temporedata-guard-main/src/main/resources/static',
    emptyOutDir: true,
    // 三方依赖分包, 利用浏览器长缓存
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          echarts: ['echarts', 'vue-echarts'],
          element: ['element-plus']
        }
      }
    }
  }
})