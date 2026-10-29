import { createRouter, createWebHistory } from 'vue-router'
import { toast } from 'vue-sonner'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/', redirect: '/publicacoes' },

  // ---- sem sessão
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/auth/LoginPage.vue'),
    meta: { guestOnly: true, layout: 'bare', title: 'Entrar' },
  },
  {
    path: '/recuperar-password',
    name: 'forgot-password',
    component: () => import('@/pages/auth/ForgotPasswordPage.vue'),
    meta: { guestOnly: true, layout: 'bare', title: 'Recuperar palavra-passe' },
  },
  {
    // o link enviado por e-mail aponta para aqui
    path: '/repor-password',
    name: 'reset-password',
    component: () => import('@/pages/auth/ResetPasswordPage.vue'),
    meta: { layout: 'bare', title: 'Nova palavra-passe' },
  },

  // ---- publicações
  {
    path: '/publicacoes',
    name: 'publicacoes',
    component: () => import('@/pages/publicacoes/PublicacoesPage.vue'),
    meta: { requiresAuth: true, title: 'Publicações' },
  },
  {
    path: '/publicacoes/create',
    name: 'publicacao-create',
    component: () => import('@/pages/publicacoes/PublicacaoCreatePage.vue'),
    meta: { requiresAuth: true, title: 'Nova publicação' },
  },
  {
    path: '/publicacoes/:id(\\d+)',
    name: 'publicacao',
    component: () => import('@/pages/publicacoes/PublicacaoDetailPage.vue'),
    props: true,
    meta: { requiresAuth: true, title: 'Publicação' },
  },

  // ---- tags, atividade, perfil
  {
    path: '/tags',
    name: 'tags',
    component: () => import('@/pages/tags/TagsPage.vue'),
    meta: { requiresAuth: true, title: 'Tags' },
  },
  { path: '/tags/subscricao', redirect: '/tags' },
  { path: '/tags/create', redirect: '/tags' },
  {
    path: '/historico',
    name: 'historico',
    component: () => import('@/pages/historico/HistoricoPage.vue'),
    meta: { requiresAuth: true, title: 'Atividade' },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/pages/profile/ProfilePage.vue'),
    meta: { requiresAuth: true, title: 'O meu perfil' },
  },

  // ---- administração
  {
    path: '/users',
    name: 'users',
    component: () => import('@/pages/users/UsersPage.vue'),
    meta: { requiresAuth: true, roles: ['ADMINISTRADOR'], title: 'Utilizadores' },
  },
  {
    path: '/users/create',
    name: 'user-create',
    component: () => import('@/pages/users/UserCreatePage.vue'),
    meta: { requiresAuth: true, roles: ['ADMINISTRADOR'], title: 'Novo utilizador' },
  },
  {
    path: '/users/:username',
    name: 'user',
    component: () => import('@/pages/users/UserDetailsPage.vue'),
    props: true,
    meta: { requiresAuth: true, roles: ['ADMINISTRADOR'], title: 'Utilizador' },
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/pages/errors/NotFoundPage.vue'),
    meta: { layout: 'bare', title: 'Página não encontrada' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: (to, from, saved) => saved || (to.path === from.path ? false : { top: 0 }),
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.init() // restaura a sessão guardada antes de decidir

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { name: 'publicacoes' }
  }
  if (to.meta.roles && !to.meta.roles.includes(auth.currentUserRole)) {
    toast.error('Não tem permissão para aceder a esta página.')
    return { name: 'publicacoes' }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - Publicações XYZ` : 'Publicações XYZ'
})

export default router
