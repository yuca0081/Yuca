import { createApp } from 'vue'
import { create } from 'naive-ui'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import './assets/styles/main.css'

const app = createApp(App)
const pinia = createPinia()

// 配置 Naive UI
const naive = create({
  messageProvider: {
    placement: 'top'
  },
  themeOverrides: {
    common: {
      primaryColor: '#14b8a6',
      primaryColorHover: '#0f766e',
      primaryColorPressed: '#134e4a'
    }
  }
})

app.use(naive)
app.use(router)
app.use(pinia)

app.mount('#app')
