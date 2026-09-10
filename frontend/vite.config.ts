import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// Keep browser calls same-origin in local development.  The application API
// client defaults to a relative `/api` path, and Vite forwards only that path
// to the locally running Spring Boot service.  A deployed environment can set
// VITE_SRM_API_BASE explicitly when it provides its own same-origin gateway.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  const localApiTarget = env.VITE_SRM_API_PROXY_TARGET || 'http://127.0.0.1:18080'

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: localApiTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
