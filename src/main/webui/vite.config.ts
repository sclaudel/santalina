import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'url'
import fs from 'fs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const propsContent = fs.readFileSync(path.resolve(__dirname, '../../../gradle.properties'), 'utf-8')
const versionMatch = propsContent.match(/appVersion=(.+)/)
const appVersion = versionMatch ? versionMatch[1].trim() : '0.0.0'

export default defineConfig({
  plugins: [react()],
  define: {
    __APP_VERSION__: JSON.stringify(appVersion),
  },
  build: {
    outDir: path.resolve(__dirname, '../resources/META-INF/resources'),
    emptyOutDir: true,
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'utils-vendor': ['axios', 'dayjs'],
        },
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
    },
  },
})
