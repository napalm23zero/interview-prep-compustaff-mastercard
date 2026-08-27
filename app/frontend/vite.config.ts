import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Listen on all interfaces, not just ::1 — inside the container, VS Code's port
    // forwarder connects over IPv4 and finds nothing on the default localhost bind.
    host: true,
    port: 5173,
    // The form calls /api/... on the same origin; Vite forwards it to Spring Boot.
    // No CORS config needed on the backend, and the fetch code ships unchanged.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
