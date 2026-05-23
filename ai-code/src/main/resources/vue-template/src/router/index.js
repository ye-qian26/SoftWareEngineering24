import { createRouter, createWebHashHistory } from 'vue-router'
import Home from '@/pages/Home.vue'
import Upload from '@/pages/Upload.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: Home
  },
  {
    path: '/upload',
    name: 'upload',
    component: Upload
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
