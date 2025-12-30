import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Lobby from '../views/Lobby.vue'
import Game from '../views/Game.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  { path: '/lobby', component: Lobby, meta: { requiresAuth: true } },
  { path: '/game/:roomId', component: Game, meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('jwt_token');
    if (!token) {
      next('/login');
    } else {
      next();
    }
  } else {
    next();
  }
})

export default router
