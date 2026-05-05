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
        manualChunks(id) {
          // ── Vendors lourds ────────────────────────────────────────────────
          if (id.includes('node_modules/exceljs'))      return 'vendor-exceljs';
          if (id.includes('node_modules/html2canvas'))  return 'vendor-html2canvas';
          if (id.includes('node_modules/jspdf'))        return 'vendor-jspdf';
          if (id.includes('node_modules/fflate'))       return 'vendor-fflate';
          if (id.includes('node_modules/react'))        return 'vendor-react';
          if (id.includes('node_modules/axios') || id.includes('node_modules/dayjs')) return 'vendor-utils';
          // ── Pages lourdes (code-split par page) ───────────────────────────
          if (id.includes('/pages/PalanqueePage'))      return 'page-palanquee';
          if (id.includes('/pages/FreeSessionPage'))    return 'page-free-session';
          if (id.includes('/pages/AdminPage'))          return 'page-admin';
          if (id.includes('/pages/HelpPage'))           return 'page-help';
          if (id.includes('/pages/StatsPage'))          return 'page-stats';
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
