import '@fontsource-variable/hanken-grotesk'
import '@fontsource-variable/newsreader/opsz.css'
import './index.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { toast } from 'vue-sonner'

import App from './App.vue'
import router from './router'
import { initTheme } from './composables/useTheme'
import { setUnauthorizedHandler } from './lib/http'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

initTheme()

// Sessão expirada, conta suspensa ou role alterado: a API responde 401 e voltamos ao login.
setUnauthorizedHandler(() => {
  const auth = useAuthStore(pinia)
  if (!auth.isLoggedIn) return
  auth.logout()
  toast.error('A sessão terminou. Inicie sessão novamente.')
  router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
})

app.mount('#app')
