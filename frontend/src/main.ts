import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './style.css'
import './assets/styles/theme.scss'
// 字体：Inter（拉丁/数字）+ 系统中文字体回退
import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'
import { applyBg, getBg, isClient } from './utils/clientTheme'

// 桌面客户端：恢复背景偏好（网页端 isClient=false，空操作）
applyBg(getBg())

// 桌面客户端：根节点标记，供固定标题栏 / 导航栏偏移使用
if (isClient()) document.documentElement.setAttribute('data-client', '1')

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
