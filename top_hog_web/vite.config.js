import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
        // rewrite: (path) => path.replace(/^\/api/, '') // The backend expects /api prefix based on controllers
      },
      '/ws-game': {
        target: 'ws://localhost:8088',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
