import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { usePermissionStore } from '@/stores/permission'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/global.css'

const app = createApp(App)

// Register all Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)

// 按钮权限指令: v-permission="'workflow:execute'"
app.directive('permission', {
  mounted(el, binding) {
    const store = usePermissionStore()
    const codes = binding.value
    const list = Array.isArray(codes) ? codes : [codes]
    const has = list.some(c => store.hasPermission(c))
    if (!has && list.length) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
})

app.mount('#app')