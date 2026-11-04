import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// In dev (npm run dev) Vite proxies /api/* to the backends running locally,
// so the app always calls relative URLs - same as behind nginx and CloudFront.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api/auth': 'http://localhost:8000',
      '/api/tasks': 'http://localhost:8080',
    },
  },
})
