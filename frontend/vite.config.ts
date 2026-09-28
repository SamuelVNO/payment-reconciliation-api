import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    port: 5173,
    // Em dev o Vite repassa /api para o Spring Boot: mesma origem, sem CORS
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
