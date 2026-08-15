import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import cssStub from './vite-css-stub-plugin.js'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    cssStub(),
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '/^element-plus.*\.css$/': fileURLToPath(new URL('./src/__tests__/css-stub.js', import.meta.url)),
          '\.css$': fileURLToPath(new URL('./src/__tests__/css-stub.js', import.meta.url)),
          'element-plus/theme-chalk/dark/css-vars.css': fileURLToPath(new URL('./src/__tests__/css-stub.js', import.meta.url))
    }
  },
  server: { fs: { allow: ['..'] } },
  optimizeDeps: { exclude: ['element-plus'] },
  test: {
    css: false,
    server: { deps: { inline: [] } },
    
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/__tests__/setup.js'],
    include: ['src/__tests__/**/*.test.js'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: ['node_modules/**', 'dist/**', '**/*.md'],
    }
  }
})
