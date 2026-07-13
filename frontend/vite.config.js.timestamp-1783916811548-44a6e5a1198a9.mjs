// vite.config.js
import { defineConfig, loadEnv } from "file:///run/csi/mount-root/nas/eab0d61a99b6696edb3d2aff87b585e8/miniLiugl/frontend/node_modules/vite/dist/node/index.js";
import vue from "file:///run/csi/mount-root/nas/eab0d61a99b6696edb3d2aff87b585e8/miniLiugl/frontend/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import AutoImport from "file:///run/csi/mount-root/nas/eab0d61a99b6696edb3d2aff87b585e8/miniLiugl/frontend/node_modules/unplugin-auto-import/dist/vite.js";
import Components from "file:///run/csi/mount-root/nas/eab0d61a99b6696edb3d2aff87b585e8/miniLiugl/frontend/node_modules/unplugin-vue-components/dist/vite.js";
import { ElementPlusResolver } from "file:///run/csi/mount-root/nas/eab0d61a99b6696edb3d2aff87b585e8/miniLiugl/frontend/node_modules/unplugin-vue-components/dist/resolvers.js";
import { fileURLToPath, URL } from "node:url";
var __vite_injected_original_import_meta_url = "file:///run/csi/mount-root/nas/eab0d61a99b6696edb3d2aff87b585e8/miniLiugl/frontend/vite.config.js";
var vite_config_default = defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const isProd = mode === "production";
  const useGateway = env.VITE_USE_GATEWAY !== "false";
  const gatewayTarget = env.VITE_GATEWAY_URL || "http://localhost:8080";
  const directTarget = env.VITE_API_BASE || "http://localhost:8080";
  return {
    plugins: [
      vue(),
      AutoImport({ resolvers: [ElementPlusResolver()] }),
      Components({ resolvers: [ElementPlusResolver()] })
      // V5.8 优化: 移除 vite-plugin-compression (含 brotli native 依赖, 沙箱装不上)
      // nginx 端已配置运行时 gzip + br 压缩 (scripts/nginx-minimax-3000.conf)
    ],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", __vite_injected_original_import_meta_url))
      }
    },
    server: {
      port: 3e3,
      host: "0.0.0.0",
      open: false,
      cors: true,
      proxy: useGateway ? {
        // 走 gateway (端口 8080) — 12 个微服务在 gateway 后
        "/api": {
          target: gatewayTarget,
          changeOrigin: true,
          cookieDomainRewrite: "",
          proxyTimeout: 3e4,
          timeout: 3e4
        },
        "/sse": {
          target: gatewayTarget,
          changeOrigin: true,
          proxyTimeout: 6e4,
          // SSE 长连接
          timeout: 6e4
        },
        "/ws": {
          target: gatewayTarget.replace("http", "ws"),
          ws: true,
          changeOrigin: true
        }
      } : {
        // 直连微服务 (开发调试用)
        "/api/v1/auth": { target: "http://localhost:8081", changeOrigin: true, proxyTimeout: 3e4 },
        "/api/v1/sessions": { target: "http://localhost:8082", changeOrigin: true },
        "/api/v1/messages": { target: "http://localhost:8082", changeOrigin: true },
        "/api/v1/chat": { target: "http://localhost:8082", changeOrigin: true },
        "/api/v1/models": { target: "http://localhost:8083", changeOrigin: true },
        "/api/v1/test": { target: "http://localhost:8083", changeOrigin: true },
        "/api/v1/openai": { target: "http://localhost:8083", changeOrigin: true },
        "/api/v1/imagegen": { target: "http://localhost:8083", changeOrigin: true },
        "/api/v1/audio": { target: "http://localhost:8083", changeOrigin: true },
        "/api/v1/leaderboard": { target: "http://localhost:8083", changeOrigin: true },
        "/api/v1/memory": { target: "http://localhost:8084", changeOrigin: true },
        "/api/v1/rag": { target: "http://localhost:8085", changeOrigin: true },
        "/api/v1/function": { target: "http://localhost:8086", changeOrigin: true },
        "/api/v1/admin": { target: "http://localhost:8087", changeOrigin: true },
        "/api/v1/monitor": { target: "http://localhost:8089", changeOrigin: true },
        "/api/v1/multimodal": { target: "http://localhost:8088", changeOrigin: true },
        "/api/v1/prompts": { target: "http://localhost:8091", changeOrigin: true },
        "/api/v1/prompt": { target: "http://localhost:8091", changeOrigin: true },
        "/api/v1/agent": { target: "http://localhost:8090", changeOrigin: true },
        "/api/v1/ws": { target: "ws://localhost:8095", ws: true, changeOrigin: true },
        "/ws": { target: "ws://localhost:8095", ws: true, changeOrigin: true },
        "/api": { target: directTarget, changeOrigin: true },
        "/sse": { target: directTarget, changeOrigin: true }
      }
    },
    build: {
      // V3.0.0: target 降为 es2015, 兼容 Chrome 63+/Edge 79+/Firefox 60+/Safari 12+
      // package.json 中 browserslist 会覆盖
      target: "es2015",
      // V5.8: sourcemap 关闭 (生产不暴露源码)
      sourcemap: isProd ? false : "eval",
      // V3.0.0: 浏览器 polyfill 支持
      polyfillModulePreload: true,
      cssCodeSplit: true,
      chunkSizeWarningLimit: 1500,
      // V5.8: 智能分包 (按依赖 + 路由)
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes("node_modules")) {
              if (id.includes("element-plus") || id.includes("@element-plus")) return "element";
              if (id.includes("echarts") || id.includes("vue-echarts")) return "echarts";
              if (id.includes("axios") || id.includes("@element-plus/icons-vue")) return "common";
              if (id.includes("vue") || id.includes("pinia") || id.includes("vue-router")) return "vue";
              if (id.includes("dayjs") || id.includes("markdown")) return "utils";
              return "vendor";
            }
            if (id.includes("/src/api/") || id.includes("/src/views/admin/")) return "admin";
            if (id.includes("/src/views/agent/")) return "agent";
            if (id.includes("/src/views/kg/")) return "kg";
            if (id.includes("/src/views/showcase/")) return "showcase";
          },
          // hash 文件名长期缓存
          entryFileNames: "assets/[name].[hash].js",
          chunkFileNames: "assets/[name].[hash].js",
          assetFileNames: "assets/[name].[hash].[ext]"
        }
      },
      // V5.8: terser 压缩 (生产)
      minify: isProd ? "terser" : false,
      terserOptions: isProd ? {
        compress: {
          drop_console: true,
          drop_debugger: true,
          pure_funcs: ["console.info", "console.debug"]
        }
      } : void 0
    },
    // V5.8: esbuild 优化
    esbuild: {
      target: "es2018",
      drop: isProd ? ["console", "debugger"] : []
    }
  };
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvcnVuL2NzaS9tb3VudC1yb290L25hcy9lYWIwZDYxYTk5YjY2OTZlZGIzZDJhZmY4N2I1ODVlOC9taW5pTGl1Z2wvZnJvbnRlbmRcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIi9ydW4vY3NpL21vdW50LXJvb3QvbmFzL2VhYjBkNjFhOTliNjY5NmVkYjNkMmFmZjg3YjU4NWU4L21pbmlMaXVnbC9mcm9udGVuZC92aXRlLmNvbmZpZy5qc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vcnVuL2NzaS9tb3VudC1yb290L25hcy9lYWIwZDYxYTk5YjY2OTZlZGIzZDJhZmY4N2I1ODVlOC9taW5pTGl1Z2wvZnJvbnRlbmQvdml0ZS5jb25maWcuanNcIjtpbXBvcnQgeyBkZWZpbmVDb25maWcsIGxvYWRFbnYgfSBmcm9tICd2aXRlJ1xuaW1wb3J0IHZ1ZSBmcm9tICdAdml0ZWpzL3BsdWdpbi12dWUnXG5pbXBvcnQgQXV0b0ltcG9ydCBmcm9tICd1bnBsdWdpbi1hdXRvLWltcG9ydC92aXRlJ1xuaW1wb3J0IENvbXBvbmVudHMgZnJvbSAndW5wbHVnaW4tdnVlLWNvbXBvbmVudHMvdml0ZSdcbmltcG9ydCB7IEVsZW1lbnRQbHVzUmVzb2x2ZXIgfSBmcm9tICd1bnBsdWdpbi12dWUtY29tcG9uZW50cy9yZXNvbHZlcnMnXG5pbXBvcnQgeyBmaWxlVVJMVG9QYXRoLCBVUkwgfSBmcm9tICdub2RlOnVybCdcblxuZXhwb3J0IGRlZmF1bHQgZGVmaW5lQ29uZmlnKCh7IG1vZGUgfSkgPT4ge1xuICBjb25zdCBlbnYgPSBsb2FkRW52KG1vZGUsIHByb2Nlc3MuY3dkKCksICcnKVxuICBjb25zdCBpc1Byb2QgPSBtb2RlID09PSAncHJvZHVjdGlvbidcblxuICAvLyBWNS4zOiBcdTlFRDhcdThCQTRcdThENzAgbmdpbnggXHU3QUVGXHU1M0UzIDMwMDAgKFx1NTQwQ1x1NkU5MFx1OEJCRlx1OTVFRSwgXHU2NUUwIENPUlMpXG4gIGNvbnN0IHVzZUdhdGV3YXkgPSBlbnYuVklURV9VU0VfR0FURVdBWSAhPT0gJ2ZhbHNlJ1xuICBjb25zdCBnYXRld2F5VGFyZ2V0ID0gZW52LlZJVEVfR0FURVdBWV9VUkwgfHwgJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA4MCdcbiAgY29uc3QgZGlyZWN0VGFyZ2V0ID0gZW52LlZJVEVfQVBJX0JBU0UgfHwgJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA4MCdcblxuICByZXR1cm4ge1xuICAgIHBsdWdpbnM6IFtcbiAgICAgIHZ1ZSgpLFxuICAgICAgQXV0b0ltcG9ydCh7IHJlc29sdmVyczogW0VsZW1lbnRQbHVzUmVzb2x2ZXIoKV0gfSksXG4gICAgICBDb21wb25lbnRzKHsgcmVzb2x2ZXJzOiBbRWxlbWVudFBsdXNSZXNvbHZlcigpXSB9KVxuICAgICAgLy8gVjUuOCBcdTRGMThcdTUzMTY6IFx1NzlGQlx1OTY2NCB2aXRlLXBsdWdpbi1jb21wcmVzc2lvbiAoXHU1NDJCIGJyb3RsaSBuYXRpdmUgXHU0RjlEXHU4RDU2LCBcdTZDOTlcdTdCQjFcdTg4QzVcdTRFMERcdTRFMEEpXG4gICAgICAvLyBuZ2lueCBcdTdBRUZcdTVERjJcdTkxNERcdTdGNkVcdThGRDBcdTg4NENcdTY1RjYgZ3ppcCArIGJyIFx1NTM4Qlx1N0YyOSAoc2NyaXB0cy9uZ2lueC1taW5pbWF4LTMwMDAuY29uZilcbiAgICBdLFxuICAgIHJlc29sdmU6IHtcbiAgICAgIGFsaWFzOiB7XG4gICAgICAgICdAJzogZmlsZVVSTFRvUGF0aChuZXcgVVJMKCcuL3NyYycsIGltcG9ydC5tZXRhLnVybCkpXG4gICAgICB9XG4gICAgfSxcbiAgICBzZXJ2ZXI6IHtcbiAgICAgIHBvcnQ6IDMwMDAsXG4gICAgICBob3N0OiAnMC4wLjAuMCcsXG4gICAgICBvcGVuOiBmYWxzZSxcbiAgICAgIGNvcnM6IHRydWUsXG4gICAgICBwcm94eTogdXNlR2F0ZXdheSA/IHtcbiAgICAgICAgLy8gXHU4RDcwIGdhdGV3YXkgKFx1N0FFRlx1NTNFMyA4MDgwKSBcdTIwMTQgMTIgXHU0RTJBXHU1RkFFXHU2NzBEXHU1MkExXHU1NzI4IGdhdGV3YXkgXHU1NDBFXG4gICAgICAgICcvYXBpJzoge1xuICAgICAgICAgIHRhcmdldDogZ2F0ZXdheVRhcmdldCxcbiAgICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWUsXG4gICAgICAgICAgY29va2llRG9tYWluUmV3cml0ZTogJycsXG4gICAgICAgICAgcHJveHlUaW1lb3V0OiAzMDAwMCxcbiAgICAgICAgICB0aW1lb3V0OiAzMDAwMCxcbiAgICAgICAgfSxcbiAgICAgICAgJy9zc2UnOiB7XG4gICAgICAgICAgdGFyZ2V0OiBnYXRld2F5VGFyZ2V0LFxuICAgICAgICAgIGNoYW5nZU9yaWdpbjogdHJ1ZSxcbiAgICAgICAgICBwcm94eVRpbWVvdXQ6IDYwMDAwLCAgLy8gU1NFIFx1OTU3Rlx1OEZERVx1NjNBNVxuICAgICAgICAgIHRpbWVvdXQ6IDYwMDAwLFxuICAgICAgICB9LFxuICAgICAgICAnL3dzJzoge1xuICAgICAgICAgIHRhcmdldDogZ2F0ZXdheVRhcmdldC5yZXBsYWNlKCdodHRwJywgJ3dzJyksXG4gICAgICAgICAgd3M6IHRydWUsXG4gICAgICAgICAgY2hhbmdlT3JpZ2luOiB0cnVlLFxuICAgICAgICB9XG4gICAgICB9IDoge1xuICAgICAgICAvLyBcdTc2RjRcdThGREVcdTVGQUVcdTY3MERcdTUyQTEgKFx1NUYwMFx1NTNEMVx1OEMwM1x1OEJENVx1NzUyOClcbiAgICAgICAgJy9hcGkvdjEvYXV0aCc6IHsgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDgxJywgY2hhbmdlT3JpZ2luOiB0cnVlLCBwcm94eVRpbWVvdXQ6IDMwMDAwIH0sXG4gICAgICAgICcvYXBpL3YxL3Nlc3Npb25zJzogeyB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODInLCBjaGFuZ2VPcmlnaW46IHRydWUgfSxcbiAgICAgICAgJy9hcGkvdjEvbWVzc2FnZXMnOiB7IHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA4MicsIGNoYW5nZU9yaWdpbjogdHJ1ZSB9LFxuICAgICAgICAnL2FwaS92MS9jaGF0JzogeyB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODInLCBjaGFuZ2VPcmlnaW46IHRydWUgfSxcbiAgICAgICAgJy9hcGkvdjEvbW9kZWxzJzogeyB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODMnLCBjaGFuZ2VPcmlnaW46IHRydWUgfSxcbiAgICAgICAgJy9hcGkvdjEvdGVzdCc6IHsgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDgzJywgY2hhbmdlT3JpZ2luOiB0cnVlIH0sXG4gICAgICAgICcvYXBpL3YxL29wZW5haSc6IHsgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDgzJywgY2hhbmdlT3JpZ2luOiB0cnVlIH0sXG4gICAgICAgICcvYXBpL3YxL2ltYWdlZ2VuJzogeyB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODMnLCBjaGFuZ2VPcmlnaW46IHRydWUgfSxcbiAgICAgICAgJy9hcGkvdjEvYXVkaW8nOiB7IHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA4MycsIGNoYW5nZU9yaWdpbjogdHJ1ZSB9LFxuICAgICAgICAnL2FwaS92MS9sZWFkZXJib2FyZCc6IHsgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDgzJywgY2hhbmdlT3JpZ2luOiB0cnVlIH0sXG4gICAgICAgICcvYXBpL3YxL21lbW9yeSc6IHsgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDg0JywgY2hhbmdlT3JpZ2luOiB0cnVlIH0sXG4gICAgICAgICcvYXBpL3YxL3JhZyc6IHsgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDg1JywgY2hhbmdlT3JpZ2luOiB0cnVlIH0sXG4gICAgICAgICcvYXBpL3YxL2Z1bmN0aW9uJzogeyB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODYnLCBjaGFuZ2VPcmlnaW46IHRydWUgfSxcbiAgICAgICAgJy9hcGkvdjEvYWRtaW4nOiB7IHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA4NycsIGNoYW5nZU9yaWdpbjogdHJ1ZSB9LFxuICAgICAgICAnL2FwaS92MS9tb25pdG9yJzogeyB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODknLCBjaGFuZ2VPcmlnaW46IHRydWUgfSxcbiAgICAgICAgJy9hcGkvdjEvbXVsdGltb2RhbCc6IHsgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDg4JywgY2hhbmdlT3JpZ2luOiB0cnVlIH0sXG4gICAgICAgICcvYXBpL3YxL3Byb21wdHMnOiB7IHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA5MScsIGNoYW5nZU9yaWdpbjogdHJ1ZSB9LFxuICAgICAgICAnL2FwaS92MS9wcm9tcHQnOiB7IHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA5MScsIGNoYW5nZU9yaWdpbjogdHJ1ZSB9LFxuICAgICAgICAnL2FwaS92MS9hZ2VudCc6IHsgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDkwJywgY2hhbmdlT3JpZ2luOiB0cnVlIH0sXG4gICAgICAgICcvYXBpL3YxL3dzJzogeyB0YXJnZXQ6ICd3czovL2xvY2FsaG9zdDo4MDk1Jywgd3M6IHRydWUsIGNoYW5nZU9yaWdpbjogdHJ1ZSB9LFxuICAgICAgICAnL3dzJzogeyB0YXJnZXQ6ICd3czovL2xvY2FsaG9zdDo4MDk1Jywgd3M6IHRydWUsIGNoYW5nZU9yaWdpbjogdHJ1ZSB9LFxuICAgICAgICAnL2FwaSc6IHsgdGFyZ2V0OiBkaXJlY3RUYXJnZXQsIGNoYW5nZU9yaWdpbjogdHJ1ZSB9LFxuICAgICAgICAnL3NzZSc6IHsgdGFyZ2V0OiBkaXJlY3RUYXJnZXQsIGNoYW5nZU9yaWdpbjogdHJ1ZSB9XG4gICAgICB9XG4gICAgfSxcbiAgICBidWlsZDoge1xuICAgICAgLy8gVjMuMC4wOiB0YXJnZXQgXHU5NjREXHU0RTNBIGVzMjAxNSwgXHU1MTdDXHU1QkI5IENocm9tZSA2MysvRWRnZSA3OSsvRmlyZWZveCA2MCsvU2FmYXJpIDEyK1xuICAgICAgLy8gcGFja2FnZS5qc29uIFx1NEUyRCBicm93c2Vyc2xpc3QgXHU0RjFBXHU4OTg2XHU3NkQ2XG4gICAgICB0YXJnZXQ6ICdlczIwMTUnLFxuICAgICAgLy8gVjUuODogc291cmNlbWFwIFx1NTE3M1x1OTVFRCAoXHU3NTFGXHU0RUE3XHU0RTBEXHU2NkI0XHU5NzMyXHU2RTkwXHU3ODAxKVxuICAgICAgc291cmNlbWFwOiBpc1Byb2QgPyBmYWxzZSA6ICdldmFsJyxcbiAgICAgIC8vIFYzLjAuMDogXHU2RDRGXHU4OUM4XHU1NjY4IHBvbHlmaWxsIFx1NjUyRlx1NjMwMVxuICAgICAgcG9seWZpbGxNb2R1bGVQcmVsb2FkOiB0cnVlLFxuICAgICAgY3NzQ29kZVNwbGl0OiB0cnVlLFxuICAgICAgY2h1bmtTaXplV2FybmluZ0xpbWl0OiAxNTAwLFxuICAgICAgLy8gVjUuODogXHU2NjdBXHU4MEZEXHU1MjA2XHU1MzA1IChcdTYzMDlcdTRGOURcdThENTYgKyBcdThERUZcdTc1MzEpXG4gICAgICByb2xsdXBPcHRpb25zOiB7XG4gICAgICAgIG91dHB1dDoge1xuICAgICAgICAgIG1hbnVhbENodW5rcyhpZCkge1xuICAgICAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdub2RlX21vZHVsZXMnKSkge1xuICAgICAgICAgICAgICBpZiAoaWQuaW5jbHVkZXMoJ2VsZW1lbnQtcGx1cycpIHx8IGlkLmluY2x1ZGVzKCdAZWxlbWVudC1wbHVzJykpIHJldHVybiAnZWxlbWVudCdcbiAgICAgICAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdlY2hhcnRzJykgfHwgaWQuaW5jbHVkZXMoJ3Z1ZS1lY2hhcnRzJykpIHJldHVybiAnZWNoYXJ0cydcbiAgICAgICAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdheGlvcycpIHx8IGlkLmluY2x1ZGVzKCdAZWxlbWVudC1wbHVzL2ljb25zLXZ1ZScpKSByZXR1cm4gJ2NvbW1vbidcbiAgICAgICAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCd2dWUnKSB8fCBpZC5pbmNsdWRlcygncGluaWEnKSB8fCBpZC5pbmNsdWRlcygndnVlLXJvdXRlcicpKSByZXR1cm4gJ3Z1ZSdcbiAgICAgICAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdkYXlqcycpIHx8IGlkLmluY2x1ZGVzKCdtYXJrZG93bicpKSByZXR1cm4gJ3V0aWxzJ1xuICAgICAgICAgICAgICByZXR1cm4gJ3ZlbmRvcidcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChpZC5pbmNsdWRlcygnL3NyYy9hcGkvJykgfHwgaWQuaW5jbHVkZXMoJy9zcmMvdmlld3MvYWRtaW4vJykpIHJldHVybiAnYWRtaW4nXG4gICAgICAgICAgICBpZiAoaWQuaW5jbHVkZXMoJy9zcmMvdmlld3MvYWdlbnQvJykpIHJldHVybiAnYWdlbnQnXG4gICAgICAgICAgICBpZiAoaWQuaW5jbHVkZXMoJy9zcmMvdmlld3Mva2cvJykpIHJldHVybiAna2cnXG4gICAgICAgICAgICBpZiAoaWQuaW5jbHVkZXMoJy9zcmMvdmlld3Mvc2hvd2Nhc2UvJykpIHJldHVybiAnc2hvd2Nhc2UnXG4gICAgICAgICAgfSxcbiAgICAgICAgICAvLyBoYXNoIFx1NjU4N1x1NEVGNlx1NTQwRFx1OTU3Rlx1NjcxRlx1N0YxM1x1NUI1OFxuICAgICAgICAgIGVudHJ5RmlsZU5hbWVzOiAnYXNzZXRzL1tuYW1lXS5baGFzaF0uanMnLFxuICAgICAgICAgIGNodW5rRmlsZU5hbWVzOiAnYXNzZXRzL1tuYW1lXS5baGFzaF0uanMnLFxuICAgICAgICAgIGFzc2V0RmlsZU5hbWVzOiAnYXNzZXRzL1tuYW1lXS5baGFzaF0uW2V4dF0nLFxuICAgICAgICB9XG4gICAgICB9LFxuICAgICAgLy8gVjUuODogdGVyc2VyIFx1NTM4Qlx1N0YyOSAoXHU3NTFGXHU0RUE3KVxuICAgICAgbWluaWZ5OiBpc1Byb2QgPyAndGVyc2VyJyA6IGZhbHNlLFxuICAgICAgdGVyc2VyT3B0aW9uczogaXNQcm9kID8ge1xuICAgICAgICBjb21wcmVzczoge1xuICAgICAgICAgIGRyb3BfY29uc29sZTogdHJ1ZSxcbiAgICAgICAgICBkcm9wX2RlYnVnZ2VyOiB0cnVlLFxuICAgICAgICAgIHB1cmVfZnVuY3M6IFsnY29uc29sZS5pbmZvJywgJ2NvbnNvbGUuZGVidWcnXSxcbiAgICAgICAgfSxcbiAgICAgIH0gOiB1bmRlZmluZWQsXG4gICAgfSxcbiAgICAvLyBWNS44OiBlc2J1aWxkIFx1NEYxOFx1NTMxNlxuICAgIGVzYnVpbGQ6IHtcbiAgICAgIHRhcmdldDogJ2VzMjAxOCcsXG4gICAgICBkcm9wOiBpc1Byb2QgPyBbJ2NvbnNvbGUnLCAnZGVidWdnZXInXSA6IFtdLFxuICAgIH1cbiAgfVxufSkiXSwKICAibWFwcGluZ3MiOiAiO0FBQW1aLFNBQVMsY0FBYyxlQUFlO0FBQ3piLE9BQU8sU0FBUztBQUNoQixPQUFPLGdCQUFnQjtBQUN2QixPQUFPLGdCQUFnQjtBQUN2QixTQUFTLDJCQUEyQjtBQUNwQyxTQUFTLGVBQWUsV0FBVztBQUwyTixJQUFNLDJDQUEyQztBQU8vUyxJQUFPLHNCQUFRLGFBQWEsQ0FBQyxFQUFFLEtBQUssTUFBTTtBQUN4QyxRQUFNLE1BQU0sUUFBUSxNQUFNLFFBQVEsSUFBSSxHQUFHLEVBQUU7QUFDM0MsUUFBTSxTQUFTLFNBQVM7QUFHeEIsUUFBTSxhQUFhLElBQUkscUJBQXFCO0FBQzVDLFFBQU0sZ0JBQWdCLElBQUksb0JBQW9CO0FBQzlDLFFBQU0sZUFBZSxJQUFJLGlCQUFpQjtBQUUxQyxTQUFPO0FBQUEsSUFDTCxTQUFTO0FBQUEsTUFDUCxJQUFJO0FBQUEsTUFDSixXQUFXLEVBQUUsV0FBVyxDQUFDLG9CQUFvQixDQUFDLEVBQUUsQ0FBQztBQUFBLE1BQ2pELFdBQVcsRUFBRSxXQUFXLENBQUMsb0JB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
