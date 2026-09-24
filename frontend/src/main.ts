import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './style.css'
import './assets/styles/theme.scss'
import { applyBg, getBg } from './utils/clientTheme'

// 桌面客户端：恢复背景偏好（网页端 isClient=false，空操作）
applyBg(getBg())

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
